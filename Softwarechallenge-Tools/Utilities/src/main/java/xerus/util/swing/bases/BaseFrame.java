package xerus.util.swing.bases;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public abstract class BaseFrame extends JFrame {

	static {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
	}
	
	public BaseFrame() {
		this(null);
	}

	public BaseFrame(Component parent) {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(400, 200));
		initGUI();
		pack();
		setLocationRelativeTo(parent);
		setVisible(true);
	}
    
    protected abstract void initGUI();

}
