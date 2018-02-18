import xerus.util.swing.bases.Base;
import xerus.util.tools.FileTools;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class TurnExtractor extends Base {

	private static final Path DATAFILE = Paths.get(System.getProperty("java.io.tmpdir"), "turnextractor-data");

	public static void main(String[] args) {
		SwingUtilities.invokeLater(TurnExtractor::new);
	}

	private static JTextField turn = new JTextField();
	private static JTextField serverinput = new JTextField();
	private static JCheckBox autoterminate = new JCheckBox("Testserver beim schließen automatisch terminieren", true);

	@Override
	protected void registerComponents() {
		String replaypath = "";
		try {
			List<String> data = Files.readAllLines(DATAFILE);
			replaypath = data.get(0);
			turn.setText(data.get(1));
			serverinput.setText(data.get(2));
			autoterminate.setSelected(data.get(3).contains("1"));
		} catch (IOException ignored) {
		}
		regFileChooser("Replay", replaypath, new FileNameExtensionFilter("Replays", "xml"));
		regLabel("Turn:", 0);
		reg(turn, 1);
		regButton("Schreibe Datei");
		regOutLabel();

		regLabel("Pfad zum Testserver:", 0);
		reg(serverinput, 1);
		regButton("Testserver mit Zug starten");
		reg(autoterminate);
		regButton("Testserver manuell terminieren");

	}

	private Process testserver;

	@Override
	protected String buttonProcess(int buttonid) throws IOException {
		if (buttonid == 2) {
			if (testserver == null)
				return "Es wurde noch kein Testserver gestartet!";
			testserver.destroy();
			return testserver.isAlive() ? "Fehler!" : "Terminiert";
		}
		String[] file = FileTools.readall(getFileChooserInput());
		if (file == null)
			return "Konnte nicht auf Datei zugreifen!";
		FileTools.write(DATAFILE.toString(), false, new File(getFileChooserInput()).getParentFile().toString(), turn.getText(), serverinput.getText(), autoterminate.isSelected() ? "1" : "0");

		String search = "turn=\"" + turn.getText().trim() + "\"";
		int[] found = {0, 0};
		for (int i = 0; i < file.length; i++) {
			String line = file[i];
			String[] split = line.split(" ");
			for (String e : split)
				if (e.contains(search)) {
					found[0] = i;
					break;
				}
			if (found[0] > 0 && line.contains("</state>")) {
				found[1] = i;
				break;
			}
		}
		if (found[1] == 0)
			return search + " nicht gefunden!";

		String filename = getFileChooserInput();
		Path p = Paths.get(filename);
		String newfile;
		if (filename.startsWith("replay_swc")) {
			newfile = filename.substring(filename.length() - 23, filename.length() - 4);
		} else
			newfile = filename.substring(0, filename.length() - 4);
		String outfile = p.resolveSibling(newfile) + "-TURN_" + turn.getText() + ".xml";
		try (Writer w = new FileWriter(outfile)) {
			w.write("<object-stream>\n");
			for (int i = found[0]; i < found[1] + 1; i++)
				w.write(file[i] + "\n");
			w.write("</object-stream>");
		}

		if (buttonid == 1) {
			Path spath = getPath(serverinput);
			ProcessBuilder pb = new ProcessBuilder("java", "-Dfile.encoding=UTF-8", "-Dlogback.configurationFile=./logback.xml",
					"-jar", spath.toString(),
					"--loadGameFile", outfile);
			pb.directory(spath.toFile());
			testserver = pb.start();
			if (autoterminate.isSelected())
				this.frame.addWindowListener(new java.awt.event.WindowAdapter() {
					public void windowClosing(java.awt.event.WindowEvent windowEvent) {
						testserver.destroy();
					}
				});
			File f = new File(outfile);
			f.delete();
			return "Testerver läuft mit " + f.getName();
		}
		return "Turn in " + outfile + " geschrieben";
	}

	@Override
	public String getTitle() {
		return "Softwarechallenge Turnextractor";
	}

}
