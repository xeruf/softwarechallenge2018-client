import jargs.gnu.CmdLineParser
import xerus.ktutil.*
import xerus.util.SysoutListener
import java.io.File
import java.util.concurrent.TimeUnit

private const val GAMES = 100

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
	strategies = basepath.resolve("evolution")
	bestFile = strategies.resolve("best.csv")
	
	debug = parser.getValue(debugOption, false)
	aiLocation = parser.getValue(aiOption, basepath.resolve("start-client.sh").toString())
	
	try {
		Evolution(parser.getValue(idOption, getNextId())).start()
		while (true) {
			println("Server alive: ${server.isAlive}")
			Evolution().start()
		}
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

class Evolution constructor(private val id: Int = getNextId()) {
	
	private val outputFile = file(id)
	private var strategy = if (file(id).exists()) {
		println("Reading id $id")
		Strategy(file(id).readText(), false)
	} else {
		Strategy(file(0).readText())
	}
	
	fun start() {
		try {
			while (strategy.games < GAMES) {
				val ai = startAI()
				if (ai.waitFor(2, TimeUnit.MINUTES)) {
					val result = File("evolution/result$id")
					strategy.write(result.readText())
					result.delete()
				}
			}
			strategy.writeEnd("Finished")
		} catch (e: Exception) {
			strategy.writeEnd("Error - $e")
		}
	}
	
	private fun startAI(): Process {
		buildAI().start()
		val builder = buildAI()
		builder.command().addAll("-s", strategy.joinparams(), "-e", id.toString())
		return builder.redirectOutput(ProcessBuilder.Redirect.INHERIT).start()
	}
	
	internal fun resetStrategy() {
		strategy.canceled = true
		//strategy = Strategy(file(0).readText())
	}
	
	private inner class Strategy internal constructor(input: String, mutate: Boolean = true) {
		var canceled = false
		
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
			if (split[0].toInt() >= 0) {
				games++
				if (split[0] == "1")
					won++
			}
			score += split[1].toInt()
			
			val s = toString()
			outputFile.writeText(s)
			println("\"$s\" in $outputFile geschrieben")
			if (games > 10) {
				if (winrate < 0.3 || (games > 30 && winrate < 0.45))
					resetStrategy()
				if (games > 50) {
					if (winrate < 0.5) {
						resetStrategy()
					} else if (winrate > 0.54) {
						bestFile.write(bestLine, s)
					}
				}
			}
		}
		
		internal fun writeEnd(msg: String) {
			println("Fertig!")
			outputFile.writeText(toString() + ";" + msg)
		}
		
		override fun toString(): String {
			winrate = (won.toDouble() / games)
			return arrayOf(joinparams(), variation.joinToString(",") { it.format(2) }, winrate.format(2), score, games, won, formattedTime()).joinToString(" ; ")
		}
		
		internal fun joinparams() = params.joinToString(",") { it.format(2) }
		
	}
	
}

private fun startServer(): Process {
	val serverBuilder = ProcessBuilder(basepath.resolve("testserver/start.sh").toString())
	serverBuilder.directory(basepath.resolve("testserver"))
	serverBuilder.redirectErrorStream(true)
	serverBuilder.redirectOutput(File("server.log"))
	return serverBuilder.start()
}
