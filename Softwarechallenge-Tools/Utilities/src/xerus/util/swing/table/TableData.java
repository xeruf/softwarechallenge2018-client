package xerus.util.swing.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class TableData extends AbstractTableModel {

	String[] columnNames;
	private List<Row> data;
	/** When this is set to true, you have to manually fire Data updates when modifying the table-data.<br>
	 * Exceptions are {@link #setColumnNames(String...)} and {@link #setData(List)} */
	public boolean silent;

	public TableData(String... columnames) {
		data = new ArrayList<>();
		columnNames = columnames;
		silent = false;
	}

	public void clearData() {
		data = new ArrayList<>();
		if (!silent)
			fireTableDataChanged();
	}

	public void setData(List<Row> newdata) {
		data = newdata;
	}

	public void setColumnNames(String... columnNames) {
		this.columnNames = columnNames;
		fireTableStructureChanged();
	}

	public String getColumnName(int col) {
		return columnNames[col];
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return data.size();
	}

	public String getValueAt(int row, int col) {
		return data.get(row).get(col);
	}

	public void setValueAt(String value, int row, int col) {
		data.get(row).set(col, value);
		if (!silent)
			fireTableCellUpdated(row, col);
	}

	public void addRows(List<String[]> rows) {
		for (String[] value : rows)
			data.add(new Row(value));
		if (!silent)
			fireTableRowsInserted(data.size() - rows.size(), data.size() - 1);
	}

	public void addRow(String... value) {
		data.add(new Row(value));
		if (!silent)
			fireTableRowsInserted(data.size() - 1, data.size() - 1);
	}

	public void addRow(Object... value) {
		String[] insertData = new String[value.length];
		for (int i = 0; i < value.length; i++)
			insertData[i] = value[i].toString();
		data.add(new Row(insertData));
		if (!silent)
			fireTableRowsInserted(data.size() - 1, data.size() - 1);
	}

	public void replaceRow(int row, String... value) {
		data.set(row, new Row(value));
		if (!silent)
			fireTableRowsUpdated(row, row);
	}

	class Row {
	
		String[] data;
	
		public Row(String... data) {
			this.data = data;
		}
	
		String get(int col) {
			if (col >= data.length)
				return null;
			return data[col];
		}
	
		void set(int col, String o) {
			if (data.length < col)
				data = Arrays.copyOf(data, col + 1);
			data[col] = o;
		}
	
	}

}