package xerus.util.javafx;

import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import xerus.util.ui.LogArea;

public class LogTextArea extends TextArea implements LogArea {

	public LogTextArea() {
		setEditable(false);
		setWrapText(true);
	}

	/** add this to the parent<br>
	 * only suppposed to be invoked once
	 * @return a new JScrollPane that wraps this component */
	public ScrollPane get() {
		return new ScrollPane(this);
	}

}
