package xerus.util.ui;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import javafx.application.Platform;
import javafx.scene.control.Dialog;
import javafx.scene.control.ScrollPane;
import javafx.stage.Window;
import xerus.util.javafx.LogTextArea;
import xerus.util.swing.JLog;

public class Changelog {

	private String[] notes;
	private Map<String, String[]> changelog;

	public Changelog(String... notes) {
		changelog = new LinkedHashMap<>();
		this.notes = notes;
	}

	public void addVersion(String version, String... message) {
		changelog.put(version, message);
	}

	public void show(JFrame parent) {
		JLog text = new JLog();
		appendLog(text);
		JScrollPane scrollPane = text.get();
		SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
		
		JDialog dialog = new JDialog(parent);
		dialog.getContentPane().add(scrollPane);
		dialog.setTitle("Changelog");
		dialog.setSize(600, 400);
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
	}

	public void show(Window parent) {
		LogTextArea text = new LogTextArea();
		appendLog(text);
		ScrollPane scrollPane = text.get();
		Platform.runLater(() -> scrollPane.setVvalue(0));

		Dialog dialog = new Dialog();
		dialog.initOwner(parent);
		dialog.getDialogPane().setContent(scrollPane);
		dialog.setTitle("Changelog");
		dialog.setWidth(600);
		dialog.setHeight(500);
		dialog.show();
	}

	private void appendLog(LogArea text) {
		if (notes.length > 0) {
			text.appendAll("", notes);
			text.appendln();
		}
		for (Entry<String, String[]> e : changelog.entrySet()) {
			text.appendln(e.getKey());
			text.appendAll(" - ", e.getValue());
			text.appendln();
		}
	}

}
