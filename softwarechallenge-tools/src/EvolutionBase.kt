import jargs.gnu.CmdLineParser
import xerus.ktutil.safe
import java.io.File

@Suppress("UNCHECKED_CAST")
fun <T> CmdLineParser.getValue(option: CmdLineParser.Option, default: T, converter: (Any) -> T? = { it as? T }) =
		getOptionValue(option)?.let { converter(it) } ?: default

@Suppress("UNCHECKED_CAST")
fun <T> CmdLineParser.getValue(option: CmdLineParser.Option, converter: (Any) -> T? = { it as? T }) =
		getOptionValue(option)?.let { converter(it) }

abstract class EvolutionBase {
	
	val separator = " ; "
	var serverlocation = "testserver/start.sh"
	var port = "13054"
	var school = false
	
	abstract var baseDir: File
	abstract var evolutionDir: File
	val logDir
		get() = if(school) File("C:\\Users\\fischerja\\Desktop\\logs\\") else evolutionDir.resolve("logs")
	val archiveDir: File
		get() = evolutionDir.resolve("archiv")
	
	fun startServer(): Process {
		val serverBuilder = ProcessBuilder(baseDir.resolve(serverlocation).toString(), "--port", port)
		serverBuilder.directory(File(serverlocation).parentFile)
		serverBuilder.redirectErrorStream(true)
		serverBuilder.redirectOutput(logDir.apply { mkdir() }.resolve("server.log"))
		return serverBuilder.start()
	}
	
	fun file(id: Int) = evolutionDir.resolve(id.toString())
	
	fun getNextId(): Int {
		var id = 1
		val nextid = evolutionDir.resolve("nextid")
		nextid.safe {
			try {
				id = readText().toInt()
			} catch (_: Exception) {
			}
			writeText((id + 1).toString())
		}
		return id
	}
	
}