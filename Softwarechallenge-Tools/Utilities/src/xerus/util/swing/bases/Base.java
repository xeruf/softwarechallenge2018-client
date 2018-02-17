package xerus.util.swing.bases;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import xerus.util.swing.SwingTools;
/**Grundklasse für ein simples, aber relativ dynamisches Interface <br/>
 * Enthält einen Timer */
public abstract class Base extends BasePanel {
	
	/*
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new thisclass());
	}
	*/
	
	protected JFrame frame;
	public Base() {
		frame = createFrame(getTitle());
	}
	
	public abstract String getTitle();
	
	// == TIMER ==
	
	private long time;
	protected void start() {
		time = System.currentTimeMillis();
		System.out.print("Running... ");
	}
	
	protected long runtime() {
		return System.currentTimeMillis()-time;
	}
	
	protected void finish() {
		long time = runtime();
		System.out.println("Done! Time: "+(time>20000 ? time/1000 : time+"m")+"s");
	}

	// == PREDEFINED COMPONENTS ==
	
	/** simple, copyable output label. Output will be automatically assigned when {@link #buttonProcess(int)} is used for buttonpress processing */
	protected static CopyableLabel outLabel;
	/** registers an output label */
	protected void regOutLabel() {
		outLabel = new CopyableLabel();
		JScrollPane scroll = new JScrollPane(outLabel);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		reg(scroll);
		SwingTools.addChangeListener(outLabel, c -> SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0)));
		outLabel.setPreferredSize(new Dimension(500, 20));
	}
	
	@Override
	protected void addButtonListener(JButton button, int buttonid) {
		button.addActionListener(evt -> {
			try {
				buttonCall(buttonid);
				outLabel.setText(null);
				String result = buttonProcess(buttonid);
				outLabel.setText(result);
			} catch (Exception e) { e.printStackTrace(); }
		});
	}
	
	/** called when a registered button gets pressed
	 * empty implementation given for the case no buttons are present
	 * @param buttonid the id of the pressed button
	 * @return the String to be displayed in the {@link #outLabel}
	 * @throws Exception dispose annoying exceptions 
	 */
	protected String buttonProcess(int buttonid) throws Exception { return null; }
	
	public static class CopyableLabel extends JTextPane {

	    private static final Font DEFAULT_FONT;
	    static {
	        Font font = UIManager.getFont("Label.font");
	        DEFAULT_FONT = (font != null) ? font : new Font("Tahoma", Font.PLAIN, 12);
	    }

	    public CopyableLabel() {
	        setContentType("text/html");
	        setEditable(false);
	        setBackground(null);
	        setBorder(null);
	        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
	        setFont(DEFAULT_FONT);
	        setAlignmentY(CENTER_ALIGNMENT);
	    }
	    
	}
	
}
