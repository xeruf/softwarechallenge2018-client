package xerus.util.swing.table;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.*;

public class ScrollableJTable extends JPanel {

	private MyTable table;
	
	public ScrollableJTable(MyTable table) {
		setLayout(new OverlayLayout(this));
		this.table = table;
		JScrollPane pane = new JScrollPane(table);
		pane.setBorder(BorderFactory.createEmptyBorder());
		add(pane, BorderLayout.CENTER);
	}

	public ScrollableJTable(String... columnames) {
		this(new MyTable(columnames));
		int colwidth =  (int) (360 * Math.sqrt(columnames.length));
		setPreferredSize(new Dimension(colwidth, 500));
	}
	
	public MyTable getTable() {
		return table;
	}

	public void addRow(Object... value) {
		table.getData().addRow(value);
	}
	
	public MyTable addToComponent(Container comp, Object constraints) {
		comp.add(this, constraints);
		return table;
	}
	
	public static MyTable addToComponent(Container comp, Object constraints, String... columnnames) {
		ScrollableJTable t = new ScrollableJTable(columnnames);
		return t.addToComponent(comp, constraints);
	}

	@Override
	public boolean isOptimizedDrawingEnabled() {
		return false;
	}

	// @Override public void validate() { super.validate(); table.repaint(); } 

	public static void showFrame() {
		JPanel panel = new ScrollableJTable();
		panel.setOpaque(true);

		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Scrollable JTable");
		frame.setContentPane(panel);
		frame.pack();
		frame.setVisible(true);
	}

}