import jargs.gnu.CmdLineParser
import xerus.ktutil.addAll
import xerus.ktutil.formattedTime
import xerus.ktutil.round
import xerus.ktutil.write
import xerus.util.SysoutListener
import xerus.util.tools.Tools
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

private lateinit var basepath: File
private lateinit var ailoc: File
private lateinit var strategies: File
private lateinit var bestFile: File

private var debug: Boolean = false

@Suppress("UNCHECKED_CAST")
fun <T> CmdLineParser.getValue(option: CmdLineParser.Option, default: T, converter: (Any) -> T? = { it as? T }) =
		converter(getOptionValue(option)) ?: default

fun main(args: Array<String>) {
	val parser = CmdLineParser()
	val path = parser.addStringOption('p', "path")
	val aiOption = parser.addStringOption("ai")
	val idOption = parser.addIntegerOption('i', "id")
	val debugOption = parser.addBooleanOption('d', "debug")
	parser.parse(args)
	
	basepath = File(parser.getOptionValue(path, System.getProperty("user.dir")) as String)
	val server = startServer()
	strategies = basepath.resolve("strategies")
	bestFile = strategies.resolve("best.csv")
	
	debug = parser.getValue(debugOption, false)
	ailoc = parser.getValue(aiOption, basepath.resolve("Jumper-1.8.2.jar")) { File(it as String) }
	
	try {
		Evolution(parser.getValue(idOption, 0))
		while (true)
			Evolution()
	} finally {
		server.destroy()
	}
}

private fun startServer(): Process {
	val serverBuilder = ProcessBuilder(basepath.resolve("testserver/start.bat").toString())
	serverBuilder.directory(basepath.resolve("testserver"))
	serverBuilder.redirectErrorStream(true)
	return serverBuilder.start()
}

private fun buildAI() = ProcessBuilder("java", "-jar", ailoc.toString())

private fun file(id: Int) = basepath.resolve(id.toString())

fun getNextId(): Int {
	val id = try {
		basepath.resolve("lastid").readText().toInt()
	} catch (t: Throwable) {
		1
	}
	basepath.resolve("lastid").writeText((id+1).toString())
	return id
}

class Evolution constructor(private val id: Int = 0) {
	
	private var strategy: Strategy
	private var started: Boolean = false
	
	private val outputFile = file(if(id == 0) getNextId() else id)
	
	init {
		strategy = if (this.id == 0) {
			Strategy(file(0).readText())
		} else {
			println("Reading id $id")
			Strategy(false, file(id).readText())
		}
		try {
			while (strategy.games < 200) {
				val ai = startAI()
				started = false
				SysoutListener.addObserver { e -> if (e.contains("Ich bin")) started = true }
				Thread.sleep(6000)
				if (!started)
					buildAI().start()
				if (ai.waitFor(2, TimeUnit.MINUTES))
					strategy.write(ai.exitValue())
			}
			strategy.writeEnd("Finished")
		} catch (e: IOException) {
			strategy.writeEnd("Error - $e")
		}
		
	}
	
	private fun startAI(): Process {
		buildAI().start()
		val pb = buildAI()
		pb.command().addAll("-s", strategy.joinparams())
		if (!debug)
			pb.command().addAll("-d", "0")
		return pb.inheritIO().start()
	}
	
	private inner class Strategy internal constructor(mutate: Boolean, info: String) {
		internal var params: DoubleArray
		internal var variation: DoubleArray
		internal var winrate: Double = 0.toDouble()
		internal var games: Int = 0
		internal var won: Int = 0
		internal var score: Int = 0
		
		internal val bestLine by lazy { bestFile.readLines().size }
		
		internal var c: Int = 0
		
		internal constructor(input: String) : this(true, input)
		
		init {
			val infos = info.split(';')
			params = infos[0].split(',').map { it.toDouble() }.toDoubleArray()
			variation = infos[1].split(',').map { it.toDouble() }.toDoubleArray()
			if (mutate)
				mutate()
			else {
				c = 2
				score = parseInfo(infos)
				games = parseInfo(infos)
				won = parseInfo(infos)
			}
			write(0)
		}
		
		private fun parseInfo(s: List<String>) =
				if (s.size > ++c) Integer.parseInt(s[c]) else 0
		
		internal fun mutate() {
			for (i in params.indices) {
				variation[i] = ((Math.random() * 2 - 1) * variation[i]).round()
				params[i] += variation[i]
			}
			games = 0
			won = 0
			score = 0
		}
		
		internal fun write(exitvalue: Int) {
			if (exitvalue > 0) {
				games++
				if (exitvalue / 100 >= 1)
					won++
				score += exitvalue % 100
			}
			
			val s = toString()
			outputFile.writeText(s)
			println("\"$s\" geschrieben")
			if (games > 30) {
				if (winrate < 0.45)
					resetStrategy()
				if (games > 100) {
					if (winrate < 0.5) {
						resetStrategy()
					} else if (winrate > 0.53) {
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
			winrate = Tools.round(won.toDouble() / games)
			return arrayOf(joinparams(), variation.joinToString(","), winrate, score, games, won, formattedTime()).joinToString(",")
		}
		
		internal fun joinparams() = params.joinToString(",")
		
	}
	
}
