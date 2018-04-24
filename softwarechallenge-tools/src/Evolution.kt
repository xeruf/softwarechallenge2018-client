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
private lateinit var datafile: File
private lateinit var bestfile: File

private var debug: Boolean = false

fun <T> CmdLineParser.getValue(option: CmdLineParser.Option, default: T, converter: (Any) -> T? = { it as? T }) =
		converter(getOptionValue(option)) ?: default

fun main(args: Array<String>) {
	val parser = CmdLineParser()
	val path = parser.addStringOption('p', "path")
	val aiOption = parser.addStringOption("ai")
	val lineOption = parser.addIntegerOption('l', "line")
	val debugOption = parser.addBooleanOption('d', "debug")
	parser.parse(args)
	
	basepath = File(parser.getOptionValue(path, System.getProperty("user.dir") + "/") as String)
	val server = startServer()
	
	debug = parser.getValue(debugOption, false)
	ailoc = parser.getValue(aiOption, basepath.resolve("Jumper-1.6.1.jar")) { File(it as String) }
	datafile = basepath.resolve("strategies.csv")
	bestfile = basepath.resolve("strategies_best.csv")
	
	try {
		Evolution(parser.getValue(lineOption, 0))
		while (true)
			Evolution()
	} finally {
		server.destroy()
	}
}

private fun startServer(): Process {
	val serverbuilder = ProcessBuilder(basepath.resolve("testserver/start.bat").toString())
	serverbuilder.directory(basepath.resolve("testserver"))
	serverbuilder.redirectErrorStream(true)
	return serverbuilder.start()
}

private fun buildAI() = ProcessBuilder("java", "-jar", ailoc.toString())

class Evolution constructor(lineparam: Int = 0) {
	
	private var strategy: Strategy
	private var line: Int = lineparam
	
	private var started: Boolean = false
	
	init {
		val file = datafile.readLines()
		if (line == 0) {
			line = file.size
			strategy = Strategy(file[0])
		} else {
			println("Reading line $line")
			strategy = Strategy(false, file[line])
		}
		try {
			while (strategy.games < 200) {
				val AI = startAI()
				started = false
				SysoutListener.addObserver { e -> if (e.contains("Ich bin")) started = true }
				Thread.sleep(6000)
				if (!started)
					buildAI().start()
				if (AI.waitFor(2, TimeUnit.MINUTES))
					strategy.write(AI.exitValue())
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
		
		internal var bestline: Int = 0
		
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
			
			val towrite = toString()
			datafile.write(line, towrite)
			println("\"%s\" auf Zeile %s geschrieben".format(towrite, line))
			if (games > 30) {
				if (winrate < 0.45)
					resetStrategy()
				if (games > 100) {
					if (winrate < 0.5) {
						resetStrategy()
					} else if (winrate > 0.53) {
						if (bestline == 0) {
							val file = bestfile.readLines()
							bestline = file.size
						}
						bestfile.write(bestline, towrite)
					}
				}
			}
		}
		
		internal fun resetStrategy() {
			var file: String? = null
			while (file == null)
				file = datafile.bufferedReader().readLine()
			strategy = Strategy(file)
		}
		
		internal fun writeEnd(msg: String) {
			println("Fertig!")
			datafile.write(line, toString() + ";" + msg)
		}
		
		override fun toString(): String {
			winrate = Tools.round(won.toDouble() / games)
			return arrayOf(joinparams(), variation.joinToString(","), winrate, score, games, won, formattedTime()).joinToString(",")
		}
		
		internal fun joinparams() =
				params.joinToString(",")
		
	}
	
}
