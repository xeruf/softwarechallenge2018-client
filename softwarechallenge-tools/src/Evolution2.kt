import jargs.gnu.CmdLineParser
import xerus.ktutil.*
import java.io.File
import java.util.concurrent.TimeUnit

/*
object Evolution2 : EvolutionBase() {
	
	override lateinit var baseDir: File
	override lateinit var strategies: File
	private lateinit var starter: String
	private lateinit var bestFile: File
	
	private var debug: Boolean = false
	
	fun main(args: Array<String>) {
		val parser = CmdLineParser()
		val path = parser.addStringOption('p', "path")
		val aiOption = parser.addStringOption("ai")
		val idOption = parser.addIntegerOption('i', "id")
		val debugOption = parser.addBooleanOption('d', "debug")
		
		parser.parse(args)
		
		baseDir = parser.getValue(path, File(System.getProperty("user.dir"))) { File(it as String) }
		val server = startServer()
		Runtime.getRuntime().addShutdownHook(Thread({
			server.destroyForcibly()
		}))
		strategies = baseDir.resolve("evolution")
		bestFile = strategies.resolve("best.csv")
		
		debug = parser.getValue(debugOption, false)
		starter = parser.getValue(aiOption, baseDir.resolve("start-client.sh").toString())
		
		Evolve(parser.getValue(idOption, getNextId())).start()
		while (true)
			Evolve().start()
	}
	
	class Evolve constructor(private val id: Int = getNextId()) {
		
		private val outputFile = file(id)
		private var strategy = if (file(id).exists()) {
			println("Reading id $id")
			createStrategy(file(id).readText(), false)
		} else {
			createStrategy(file(0).readText())
		}
		
		fun createStrategy(input: String, mutate: Boolean = true) {
			val split = input.split(separator)
			val params = split[0].split(',').map { it.toDouble() }.toDoubleArray()
			val variation = split[1].split(',').map { it.toDouble() }.toDoubleArray()
			if (mutate) {
				for (i in params.indices) {
					variation[i] = ((Math.random() * 2.2 - 1.1) * variation[i]).round()
					params[i] += variation[i]
				}
			}
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
				outputFile.writeText(toString() + separator + msg)
			}
			
			override fun toString(): String {
				winrate = (won.toDouble() / games)
				return arrayOf(joinparams(), variation.joinToString(",") { it.format(2) }, winrate.format(2), score, games, won, formattedTime()).joinToString(separator)
			}
			
			internal fun joinparams() = params.joinToString(",") { it.format(2) }
			
		}
		
	}
	
}*/
