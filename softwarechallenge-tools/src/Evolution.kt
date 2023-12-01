import jargs.gnu.CmdLineParser
import xerus.ktutil.*
import java.io.File
import java.util.concurrent.TimeUnit

object Evolution : EvolutionBase() {
	
	private const val GAMES = 100
	
	override lateinit var baseDir: File
	override lateinit var evolutionDir: File
	private lateinit var aiFile: File
	private lateinit var bestFile: File
	
	private var debug = false
	private var mutator = false
	
	private lateinit var server: Process
	private var running = true
		get() = server.isAlive && field
	
	@JvmStatic
	fun main(args: Array<String>) {
		val parser = CmdLineParser()
		val basedirOption = parser.addStringOption("basedir")
		val evolutiondirOption = parser.addStringOption("evolutiondir")
		val aiOption = parser.addStringOption("ai")
		val serverlocationOption = parser.addStringOption("server")
		val mutatorOption = parser.addBooleanOption("mutator")
		
		val debugOption = parser.addBooleanOption('d', "debug")
		val schoolOption = parser.addBooleanOption("school")
		val idOption = parser.addIntegerOption('i', "id")
		val portOption = parser.addStringOption('p', "port")
		
		parser.parse(args)
		
		mutator = parser.getValue(mutatorOption, false)
		school = parser.getValue(schoolOption, false)
		baseDir = parser.getValue(basedirOption, File(System.getProperty("user.dir"))) { File(it as String) }
		evolutionDir = File(baseDir, parser.getValue(evolutiondirOption, "evolution"))
		archiveDir.mkdirs()
		bestFile = evolutionDir.resolve("best.csv")
		
		port = parser.getValue(portOption, port)
		println("Port: $port")
		serverlocation = parser.getValue(serverlocationOption, if (school) "testserver/start.bat" else serverlocation)
		server = startServer()
		Runtime.getRuntime().addShutdownHook(Thread({
			running = false
			server.destroy()
			println("Stopping server...")
			server.waitFor()
		}, "Server terminator"))
		
		debug = parser.getValue(debugOption, false)
		aiFile = parser.getValue(aiOption, baseDir.resolve(if (school) "clients/start-client.bat" else "docker-client.sh")) { File(it as String) }.absoluteFile
		println("AI: $aiFile")
		
		var result = Evolve(parser.getValue(idOption) ?: getNextId()).start()
		while (result)
			result = Evolve().start()
	}
	
	private fun buildAI(): ProcessBuilder {
		val builder = if (aiFile.endsWith("jar"))
			ProcessBuilder("java", "-jar", aiFile.toString())
		else
			ProcessBuilder(aiFile.toString())
		builder.redirectOutput(logDir.resolve("${aiFile.name}-${System.currentTimeMillis()}.log"))
		if (debug)
			builder.redirectError(ProcessBuilder.Redirect.INHERIT)
		builder.directory(aiFile.parentFile)
		builder.command().addAll("--port", port)
		if (!debug)
			builder.command().addAll("-d", "0")
		return builder
	}
	
	class Evolve constructor(private val id: Int = getNextId()) {
		
		private val outputFile = file(id)
		private var strategy = if (outputFile.exists()) {
			println("Reading id $id")
			Strategy(file(id).readText(), false)
		} else {
			println("Got id $id")
			Strategy()
		}
		
		fun start(): Boolean {
			try {
				while (strategy.games < GAMES) {
					if (school) logDir.mkdirs()
					if (!running)
						return false
					val ai = startAI()
					if (ai.waitFor(1, TimeUnit.SECONDS) && ai.exitValue() != 0) {
						println("$ai exited unexpectedly with exit code ${ai.exitValue()}!")
						return false
					}
					if (ai.waitFor(3, TimeUnit.MINUTES)) {
						if (!running)
							return false
						val result = baseDir.resolve("clients/result$id")
						strategy.write(result.readText())
						result.delete()
					} else {
						println("The game did not finish in time!")
						return false
					}
					if (school) logDir.deleteRecursively()
				}
				strategy.writeEnd("Finished")
			} catch (e: Exception) {
				strategy.writeEnd("Error - $e")
				running = false
			}
			return running
		}
		
		private fun startAI(): Process {
			buildAI().start()
			val builder = buildAI()
			builder.command().addAll("-s", strategy.joinparams(), "-e", id.toString())
			return builder.redirectOutput(ProcessBuilder.Redirect.INHERIT).start()
		}
		
		internal fun resetStrategy() {
			outputFile.renameTo(archiveDir.resolve("$id-${System.currentTimeMillis()}"))
			println("Resetting!")
			strategy = Strategy()
		}
		
		private inner class Strategy internal constructor(input: String = bestFile.safe { readLines().let { if (mutator) it[(Math.random() * it.size).toInt()] else it[0] } }, mutate: Boolean = true) {
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
				println("Wrote \"$s\" to $outputFile")
				if (games > 15) {
					val bcd = binominalCD(won, games)
					if (bcd < 0.4)
						resetStrategy()
					if (bestLine.isInitialized() || (games > 50 && bcd > 0.95)) {
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