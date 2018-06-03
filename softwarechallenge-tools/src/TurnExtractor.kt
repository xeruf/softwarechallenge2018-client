import xerus.util.swing.bases.Base
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.*
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import javax.swing.JCheckBox
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

class TurnExtractor : Base() {
	
	private lateinit var testserver: Process
	
	override fun registerComponents() {
		var replaypath = ""
		try {
			val data = DATAFILE.readLines()
			replaypath = data[0]
			turn.text = data[1]
			serverinput.text = data[2]
			autoterminate.isSelected = data[3].toBoolean()
		} catch (ignored: IOException) {
		}
		
		regFileChooser("Replay", replaypath, FileNameExtensionFilter("Replays", "xml", "gz"))
		regLabel("Turn:", 0)
		reg(turn, 1)
		regButton("Schreibe Datei")
		regOutLabel()
		
		regLabel("Pfad zum Testserver:", 0)
		reg(serverinput, 1)
		regButton("Testserver mit Zug starten")
		reg(autoterminate)
		regButton("Testserver manuell terminieren")
		
		SwingUtilities.invokeLater { frame.size = Dimension(600, 400) }
	}
	
	override fun buttonProcess(buttonid: Int): String {
		if (buttonid == 2) {
			if (!this::testserver.isInitialized)
				return "Es wurde noch kein Testserver gestartet!"
			Base.outLabel.text = "Terminiere..."
			testserver.destroy()
			testserver.waitFor(3, TimeUnit.SECONDS)
			return if (testserver.isAlive) "Fehler!" else "Terminiert"
		}
		
		var file = File(fileChooserInput)
		if (!file.exists())
			return "Datei nicht gefunden!"
		if (file.extension == "gz") {
			val extractedFile = file.resolveSibling(file.nameWithoutExtension)
			GZIPInputStream(FileInputStream(file)).copyTo(extractedFile.outputStream())
			file = extractedFile
		}
		val lines = file.readLines()
		DATAFILE.writeText(arrayOf(File(fileChooserInput).parent, turn.text, serverinput.text, autoterminate.isSelected).joinToString("\n"))
		
		val search = "turn=\"" + turn.text.trim { it <= ' ' } + "\""
		val found = intArrayOf(0, 0)
		lines.forEachIndexed { i, line ->
			val split = line.split(" ")
			for (e in split)
				if (e.contains(search)) {
					found[0] = i
					break
				}
			if (found[0] > 0 && line.contains("</state>")) {
				found[1] = i
				return@forEachIndexed
			}
		}
		if (found[1] == 0)
			return "$search nicht gefunden!"
		
		val turnFile = file.resolveSibling("${file.nameWithoutExtension}-TURN_${turn.text}.xml")
		FileWriter(turnFile).use({ w ->
			w.write("<protocol>\n")
			for (i in found[0] until found[1] + 1)
				w.write(lines[i] + "\n")
			w.write("</protocol>")
		})
		
		if (buttonid == 1) {
			val serverFile = File(serverinput.text)
			val pb = ProcessBuilder("java", "-Dfile.encoding=UTF-8", "-Dlogback.configurationFile=./logback.xml",
					"-jar", serverFile.toString(),
					"--loadGameFile", turnFile.toString())
			pb.directory(serverFile.parentFile)
			pb.inheritIO()
			if (::testserver.isInitialized)
				testserver.destroy()
			testserver = pb.start()
			if (autoterminate.isSelected)
				this.frame.addWindowListener(object : WindowAdapter() {
					override fun windowClosing(windowEvent: WindowEvent?) {
						testserver.destroy()
					}
				})
			turnFile.deleteOnExit()
			return "Testerver läuft mit " + turnFile.name
		}
		return "Turn in $turnFile geschrieben"
	}
	
	override fun getTitle() = "Softwarechallenge Turnextractor"
	
	companion object {
		
		private val DATAFILE = File(System.getProperty("java.io.tmpdir"), "turnextractor-data")
		
		@JvmStatic
		fun main(args: Array<String>) {
			SwingUtilities.invokeLater { TurnExtractor() }
		}
		
		private val turn = JTextField()
		private val serverinput = JTextField()
		private val autoterminate = JCheckBox("Testserver beim schließen automatisch terminieren", true)
	}
	
}
