import jargs.gnu.CmdLineParser
import java.io.File
import java.io.FileNotFoundException
import java.nio.channels.FileChannel

@Suppress("UNCHECKED_CAST")
fun <T> CmdLineParser.getValue(option: CmdLineParser.Option, default: T, converter: (Any) -> T? = { it as? T }) =
		getOptionValue(option)?.let { converter(it) } ?: default

abstract class EvolutionBase {
	
	val separator = " ; "
	
	abstract var basepath: File
	abstract var strategies: File
	
	fun startServer(): Process {
		val serverBuilder = ProcessBuilder(basepath.resolve("testserver/start.sh").toString())
		serverBuilder.directory(basepath.resolve("testserver"))
		serverBuilder.redirectErrorStream(true)
		serverBuilder.redirectOutput(File("server.log"))
		return serverBuilder.start()
	}
	
	fun File.safe(operation: File.() -> Unit) {
		val sibling = resolveSibling("$name~")
		while(!sibling.createNewFile())
			Thread.sleep(10)
		sibling.writeText(readText())
		operation(this)
		sibling.delete()
	}
	
	fun file(id: Int) = strategies.resolve(id.toString())
	
	fun getNextId(): Int {
		var id = 1
		val nextid = strategies.resolve("nextid")
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