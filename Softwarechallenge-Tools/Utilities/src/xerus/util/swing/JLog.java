package xerus.util.swing;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import xerus.util.ui.LogArea;

public class JLog extends JTextArea implements LogArea {

	public JLog() {
		setEditable(false);
		setLineWrap(true);
		setWrapStyleWord(true);
	}

	/** add this to the parent<br>
	 * only suppposed to be invoked once
	 * @return a new JScrollPane that wraps this component */
	public JScrollPane get() {
		return new JScrollPane(this);
	}

	@Override
	public void appendText(String s) {
		super.append(s);
	}

}
