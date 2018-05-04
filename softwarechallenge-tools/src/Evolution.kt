import jargs.gnu.CmdLineParser
import xerus.ktutil.*
import xerus.util.SysoutListener
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

private lateinit var basepath: File
private lateinit var aiLocation: String
private lateinit var strategies: File
private lateinit var bestFile: File

private var debug: Boolean = false

@Suppress("UNCHECKED_CAST")
fun <T> CmdLineParser.getValue(option: CmdLineParser.Option, default: T, converter: (Any) -> T? = { it as? T }) =
		getOptionValue(option)?.let { converter(it) } ?: default

fun main(args: Array<String>) {
	val parser = CmdLineParser()
	val path = parser.addStringOption('p', "path")
	val aiOption = parser.addStringOption("ai")
	val idOption = parser.addIntegerOption('i', "id")
	val debugOption = parser.addBooleanOption('d', "debug")
	parser.parse(args)
	
	basepath = parser.getValue(path, File(System.getProperty("user.dir"))) { File(it as String) }
	val server = startServer()
	strategies = basepath.resolve("strategies")
	bestFile = strategies.resolve("best.csv")
	
	debug = parser.getValue(debugOption, false)
	aiLocation = parser.getValue(aiOption, basepath.resolve("start-client.sh").toString())
	
	try {
		Evolution(parser.getValue(idOption, 0))
		while (true)
			Evolution()
	} finally {
		server.destroy()
	}
}

private fun buildAI(): ProcessBuilder {
	val builder =
			if (aiLocation.endsWith(".jar")) ProcessBuilder("java", "-jar", aiLocation)
			else ProcessBuilder(aiLocation)
	if (!debug)
		builder.command().addAll("-d", "0")
	return builder
}

private fun file(id: Int) = strategies.resolve(id.toString())

fun getNextId(): Int {
	val id = try {
		strategies.resolve("nextid").readText().toInt()
	} catch (t: Throwable) {
		1
	}
	strategies.resolve("nextid").writeText((id + 1).toString())
	return id
}

class Evolution constructor(private val id: Int = 0) {
	
	private var strategy: Strategy
	private var running: Boolean = false
	
	private val outputFile = file(if (id == 0) getNextId() else id)
	
	init {
		strategy = if (id == 0) {
			Strategy(file(0).readText())
		} else {
			println("Reading id $id")
			Strategy(file(id).readText(), false)
		}
		val observer = SysoutListener.addObserver { if (it.contains("Ich bin")) running = true }
		try {
			while (strategy.games < 2/*00*/) {
				val ai = startAI()
				running = false
				Thread.sleep(5000)
				if (!running)
					buildAI().start()
				if (ai.waitFor(2, TimeUnit.MINUTES)) {
					val result = File("strategies/result$id")
					strategy.write(result.readText())
					result.delete()
				}
			}
			strategy.writeEnd("Finished")
		} catch (e: Exception) {
			strategy.writeEnd("Error - $e")
		}
		SysoutListener.removeObserver(observer)
	}
	
	private fun startAI(): Process {
		buildAI().start()
		val builder = buildAI()
		builder.command().addAll("-s", strategy.joinparams(), "-e", id.toString())
		return builder.inheritIO().start()
	}
	
	private inner class Strategy internal constructor(input: String, mutate: Boolean = true) {
		internal var params: DoubleArray
		internal var variation: DoubleArray
		internal var winrate: Double = 0.0
		internal var games: Int = 0
		internal var won: Int = 0
		internal var score: Int = 0
		
		internal val bestLine by lazy { bestFile.readLines().size }
		
		init {
			val split = input.split(" ; ")
			params = split[0].split(',').map { it.toDouble() }.toDoubleArray()
			variation = split[1].split(',').map { it.toDouble() }.toDoubleArray()
			if (mutate)
				mutate()
			else {
				var c = 2
				fun parseInput(s: List<String>) = if (s.size > ++c) s[c].toInt() else 0
				score = parseInput(split)
				games = parseInput(split)
				won = parseInput(split)
			}
			write("-1 0")
		}
		
		internal fun mutate() {
			for (i in params.indices) {
				variation[i] = ((Math.random() * 2.2 - 1.1) * variation[i]).round()
				params[i] += variation[i]
			}
			games = 0
			won = 0
			score = 0
		}
		
		internal fun write(result: String) {
			val split = result.split(' ')
			if (split[0].toInt() > 0)
				games++
			if (split[0] == "1")
				won++
			score += split[1].toInt()
			
			val s = toString()
			outputFile.writeText(s)
			println("\"$s\" geschrieben")
			if (games > 30) {
				if (winrate < 0.45)
					resetStrategy()
				if (games > 100) {
					if (winrate < 0.5) {
						resetStrategy()
					} else if (winrate > 0.54) {
						bestFile.write(bestLine, s)
					}
				}
			}
		}
		
		internal fun resetStrategy() {
			strategy = Strategy(file(0).readText())
		}
		
		internal fun writeEnd(msg: String) {
			println("Fertig!")
			outputFile.writeText(toString() + ";" + msg)
		}
		
		override fun toString(): String {
			winrate = (won.toDouble() / games)
			return arrayOf(joinparams(), variation.joinToString(","), winrate.round(), score, games, won, formattedTime()).joinToString(" ; ")
		}
		
		internal fun joinparams() = params.joinToString(",")
		
	}
	
}

private fun startServer(): Process {
	val serverBuilder = ProcessBuilder(basepath.resolve("testserver/start.sh").toString())
	serverBuilder.directory(basepath.resolve("testserver"))
	serverBuilder.redirectErrorStream(true)
	return serverBuilder.start()
}
