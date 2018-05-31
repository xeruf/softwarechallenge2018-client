import jargs.gnu.CmdLineParser
import xerus.ktutil.*
import java.io.File
import java.util.concurrent.TimeUnit

object Evolution : EvolutionBase() {
	
	private const val GAMES = 100
	
	override lateinit var basepath: File
	override lateinit var strategiesDir: File
	val garbageDir: File
		get() = strategiesDir.resolve("garbage")
	private lateinit var aiFile: File
	private lateinit var bestFile: File
	
	private var debug: Boolean = false
	private lateinit var server: Process
	
	@JvmStatic
	fun main(args: Array<String>) {
		val parser = CmdLineParser()
		val pathOption = parser.addStringOption("path")
		val aiOption = parser.addStringOption("ai")
		val serverOption = parser.addStringOption('s', "server")
		
		val idOption = parser.addIntegerOption('i', "id")
		val debugOption = parser.addBooleanOption('d', "debug")
		val portOption = parser.addStringOption("port")
		
		parser.parse(args)
		
		basepath = parser.getValue(pathOption, File(System.getProperty("user.dir"))) { File(it as String) }
		strategiesDir = basepath.resolve("evolution")
		garbageDir.mkdirs()
		bestFile = strategiesDir.resolve("best.csv")
		
		port = parser.getValue(portOption, port)
		serverPath = parser.getValue(serverOption, serverPath)
		server = startServer()
		Runtime.getRuntime().addShutdownHook(Thread({
			server.destroyForcibly()
		}, "Server terminator"))
		
		debug = parser.getValue(debugOption, false)
		aiFile = parser.getValue(aiOption, basepath.resolve("docker-client.sh")) { File(it as String) }.absoluteFile
		
		var result = Evolve(parser.getValue(idOption) ?: getNextId()).start()
		while (server.isAlive && result)
			result = Evolve().start()
	}
	
	private fun buildAI(): ProcessBuilder {
		val builder = if (aiFile.endsWith("jar"))
			ProcessBuilder("java", "-jar", aiFile.toString())
		else
			ProcessBuilder(aiFile.toString())
		builder.directory(aiFile.parentFile)
		builder.command().addAll("--port", "13055")
		if (!debug)
			builder.command().addAll("-d", "0")
		return builder
	}
	
	class Evolve constructor(private val id: Int = getNextId()) {
		
		private val outputFile = file(id)
		private var strategy = if (file(id).exists()) {
			println("Reading id $id")
			Strategy(file(id).readText(), false)
		} else {
			Strategy()
		}
		
		fun start(): Boolean {
			try {
				while (strategy.games < GAMES) {
					val ai = startAI()
					if (ai.waitFor(4, TimeUnit.SECONDS)) {
						println("$ai exited unexpectedly with exit code ${ai.exitValue()}!")
						return false
					}
					if (ai.waitFor(2, TimeUnit.MINUTES)) {
						if (!server.isAlive)
							return false
						val result = aiFile.resolveSibling("result$id")
						strategy.write(result.readText())
						result.delete()
					}
				}
				strategy.writeEnd("Finished")
			} catch (e: Exception) {
				strategy.writeEnd("Error - $e")
			}
			return true
		}
		
		private fun startAI(): Process {
			buildAI().start()
			val builder = buildAI()
			builder.command().addAll("-s", strategy.joinparams(), "-e", id.toString())
			return builder.redirectOutput(ProcessBuilder.Redirect.INHERIT).start()
		}
		
		internal fun resetStrategy() {
			outputFile.renameTo(garbageDir.resolve("$id-${System.currentTimeMillis()}"))
			strategy = Strategy()
		}
		
		private inner class Strategy internal constructor(input: String = bestFile.safe { readLines()[0] }, mutate: Boolean = true) {
			internal var params: DoubleArray
			internal var variation: DoubleArray
			internal var winrate: Double = 0.0
			internal var games: Int = 0
			internal var won: Int = 0
			internal var score: Int = 0
			
			internal val bestLine = lazy(LazyThreadSafetyMode.NONE) { bestFile.readLines().size }
			
			init {
				val split = input.split(separator)
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
				if (games > 15) {
					val bcd = binominalCD(won, games)
					if (bcd < 0.3)
						resetStrategy()
					if (bestLine.isInitialized() || (games > 40 && bcd > 0.9)) {
						bestFile.safe {
							println("Good Strategy - Writing to $bestFile line ${bestLine.value}")
							write(bestLine.value, s)
						}
					}
				}
			}
			
			internal fun writeEnd(msg: String) {
				println("Done: $msg")
				println()
				outputFile.writeText(toString() + separator + msg)
			}
			
			override fun toString(): String {
				winrate = won.toDouble() / games
				return arrayOf(joinparams(), variation.joinToString(",") { it.format(2) }, winrate.format(2), score, games, won, formattedTime()).joinToString(separator)
			}
			
			internal fun joinparams() = params.joinToString(",") { it.format(2) }
			
		}
		
	}
	
}