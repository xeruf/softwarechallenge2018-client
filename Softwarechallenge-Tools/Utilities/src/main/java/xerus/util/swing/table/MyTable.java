package xerus.util.swing.table;

import java.awt.Dimension;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.commons.lang3.ArrayUtils;

public class MyTable extends JTable {
	
	private TableData data;
	
	public MyTable(String... columnames) {
		this(new TableData(columnames));
		setMinimumSize(new Dimension((int) (200 * Math.sqrt(columnames.length)), 200));
	}
	
	public MyTable(TableData tabledata) {
		super(tabledata);
		data = tabledata;
		((DefaultTableCellRenderer) getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
	}
	
	public TableData getData() {
		return data;
	}

	public void addRow(Object... value) {
		data.addRow(value);
	}

	public void fitColumns(Object... preferredColumns) {
		final TableColumnModel columnModel = getColumnModel();
		boolean isInt = true;
		for(Object o : preferredColumns)
			if(!(o.getClass() == Integer.class))
				isInt = false;
		for (int column = 0; column < getColumnCount(); column++) {
			int width = 40;
			for (int row = 0; row < getRowCount(); row++) {
				int compWidth = prepareRenderer(getCellRenderer(row, column), row, column).getPreferredSize().width + 1;
				width = Math.max(compWidth, width);
			}
			TableColumn col = columnModel.getColumn(column);
			/*col.setMaxWidth((int) (1.1 * width));
			int avg = widthSum / table.getRowCount();
			if(width > avg*2 && width > 100)
				width = (width + avg) / 2;*/
			if (isInt ? ArrayUtils.contains(preferredColumns, column) : ArrayUtils.contains(preferredColumns, col.getHeaderValue()))
				col.setMinWidth(width);
			col.setPreferredWidth(width);
		}
	}

	public void centerColumns(Object... columns) {
		final DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		final TableColumnModel columnModel = getColumnModel();
		for (Object column : columns) {
			if(column.getClass() == Integer.class)
				columnModel.getColumn((Integer)column).setCellRenderer(centerRenderer);
			else
				columnModel.getColumn(columnModel.getColumnIndex(column)).setCellRenderer(centerRenderer);
		}
	}

}
