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
	var serverPath = "testserver/start.sh"
	var port = "13054"
	var school = false
	
	abstract var basepath: File
	abstract var strategiesDir: File
	
	fun startServer(): Process {
		val serverBuilder = ProcessBuilder(basepath.resolve(serverPath).toString(), "--port", port)
		serverBuilder.directory(File(serverPath).parentFile)
		serverBuilder.redirectErrorStream(true)
		if (!school)
			serverBuilder.redirectOutput(strategiesDir.resolve("server.log"))
		return serverBuilder.start()
	}
	
	fun file(id: Int) = strategiesDir.resolve(id.toString())
	
	fun getNextId(): Int {
		var id = 1
		val nextid = strategiesDir.resolve("nextid")
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