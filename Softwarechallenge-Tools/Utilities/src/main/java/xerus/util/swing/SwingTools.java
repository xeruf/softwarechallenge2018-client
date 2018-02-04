package xerus.util.swing;

import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EventListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

public class SwingTools {

	public static void addChangeListener(JComponent comp, ChangeListener listener) {
		if(comp instanceof JTextComponent) {
			addChangeListener((JTextComponent)comp, listener);
		}
		Class c = comp.getClass();
		Method m = null;
		ActionListener al = a -> listener.stateChanged(new ChangeEvent(comp));
		for(EventListener el : new EventListener[]{listener, al}) {
			try {
				m = findAdder(comp, el.getClass());
				if (m != null) {
					m.invoke(comp, el);
					return;
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) { }
		}
		/*try {
			m = findAdder(comp, ChangeListener.class);
			if (m != null) {
				m.invoke(comp, listener);
				return;
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) { }
		try {
			m = findAdder(comp, ActionListener.class);
			if (m != null) {
				ActionListener al = a -> listener.stateChanged(new ChangeEvent(comp));
				m.invoke(comp, al);
				return;
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) { }*/
		throw new IllegalArgumentException("Can't attach a ChangeListener to " + c);
	}

	private static Method findAdder(JComponent comp, Class c) {
		c = c.getInterfaces()[0];
		try {
			return comp.getClass().getDeclaredMethod("add" + c.getSimpleName(), c);
		} catch (NoSuchMethodException | SecurityException e) {
			return null;
		}
	}

	/** Installs a listener to receive notification when the text of the {@code JTextComponent} is changed. Internally,
	 * it installs a {@link DocumentListener} on the text component's {@link Document}, and a
	 * {@link PropertyChangeListener} on the text component to detect if the {@code Document} itself is replaced.
	 * @param textfield any text component, such as a {@link JTextField} or {@link JTextArea}
	 * @param listener a listener to receieve {@link ChangeEvent}s when the text is changed; the source object for
	 *        the events will be the text component
	 * @throws NullPointerException if either parameter is null */
	public static void addChangeListener(JTextComponent textfield, ChangeListener listener) {
		DocumentListener dl = new DocumentListener() {
			private int lastChange = 0, lastNotifiedChange = 0;

			public void insertUpdate(DocumentEvent e) {
				changedUpdate(e);
			}

			public void removeUpdate(DocumentEvent e) {
				changedUpdate(e);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				lastChange++;
				SwingUtilities.invokeLater(() -> {
					if (lastNotifiedChange != lastChange) {
						lastNotifiedChange = lastChange;
						listener.stateChanged(new ChangeEvent(textfield));
					}
				});
			}
		};

		textfield.addPropertyChangeListener("document", (PropertyChangeEvent e) -> {
			Document d1 = (Document) e.getOldValue();
			Document d2 = (Document) e.getNewValue();
			if (d1 != null)
				d1.removeDocumentListener(dl);
			if (d2 != null)
				d2.addDocumentListener(dl);
			dl.changedUpdate(null);
		});

		Document d = textfield.getDocument();
		if (d != null)
			d.addDocumentListener(dl);
	}

	public static JTextField getTableSearchField(JTable table) {
		TableRowSorter<TableModel> rowSorter = new TableRowSorter<>(table.getModel());
		JTextField searchField = new JTextField();
		SwingTools.addChangeListener(searchField, c -> {
			rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchField.getText()));
		});
		table.setRowSorter(rowSorter);
		return searchField;
	}

}
