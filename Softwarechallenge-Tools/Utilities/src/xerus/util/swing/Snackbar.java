package xerus.util.swing;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Snackbar extends JPanel implements ActionListener {

	private boolean permanent;
	private JLabel textLabel;
	
	public Snackbar(String info, boolean permanent, boolean visible) {
		this.permanent = permanent;
		setLayout(new BorderLayout());
		add(textLabel = new JLabel(info), BorderLayout.CENTER);
		textLabel.setHorizontalAlignment(JLabel.CENTER);
		JButton x = new JButton("X");
		add(x, BorderLayout.EAST);
		x.addActionListener(this);
		setVisible(visible);
	}

	/** creates an inititally visible Snackbar that will vanish when clicked away */
	public Snackbar(String info) {
		this(info, false, true);
	}

	/** creates an inititally invisible Snackbar that will persist when clicked away */
	public Snackbar() {
		this("", true, false);
	}
	
	public String getText() {
		return textLabel.getText();
	}
	
	/** sets the */
	public void show(String s, Object... args) {
		textLabel.setText(String.format(s, args));
		this.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (permanent) {
			setVisible(false);
		} else {
			this.getParent().remove(this);
		}
	}

}
