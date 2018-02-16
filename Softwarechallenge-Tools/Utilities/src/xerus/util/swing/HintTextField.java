package xerus.util.swing;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;

public class HintTextField extends JTextField implements FocusListener {

	private final String hint;
	private boolean showingHint;

	public HintTextField(String hint) {
		super();
		this.hint = hint;
		showHint(true);
		addFocusListener(this);
	}

	@Override
	public void focusGained(FocusEvent e) {
		if(showingHint)
			showHint(false);
	}

	@Override
	public void focusLost(FocusEvent e) {
		if(getText().isEmpty())
			showHint(true);
	}

	@Override
	public void setText(String t) {
		if(t != null && !t.isEmpty()) {
			showHint(false);
			super.setText(t);
		} else {
			showHint(true);
		}
	}

	private void showHint(boolean show) {
		super.setText(show ? hint : "");
		showingHint = show;
		setForeground(show ? Color.GRAY : Color.BLACK);
	}

	@Override
	public String getText() {
		return showingHint ? "" : super.getText();
	}
}