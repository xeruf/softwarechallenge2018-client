package xerus.util;

import xerus.util.swing.SwingTools;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public interface Setting {

	/** the JTextComponent is set to the current value of this Setting<br>
	 * subsequent changes to the field will change this setting too */
	default JTextComponent addField(JTextComponent comp) {
		comp.setText(get());
		SwingTools.addChangeListener(comp, c -> put(comp.getText()));
		return comp;
	}

	/** the JComboBox is set to the current value of this setting<br>
	 * subsequent changes to the Combobox will change this setting too */
	public default <E> JComboBox<E> addField(JComboBox<E> comp) {
		comp.setSelectedItem(get());
		SwingTools.addChangeListener(comp, a -> put(comp.getSelectedItem()));
		return comp;
	}

	/** the JCheckBox is set to the current value of this setting<br>
	 * subsequent changes to the it will change this setting too */
	public default JCheckBox addField(JCheckBox comp) {
		comp.setSelected(getBool());
		SwingTools.addChangeListener(comp, a -> put(comp.isSelected()));
		return comp;
	}

	/** changes to the JSpinner will change this setting too */
	public default JSpinner addField(JSpinner comp) {
		SwingTools.addChangeListener(comp, a -> put(comp.getValue()));
		return comp;
	}

	/** associates the given String with this field within the Preferences */
	default void put(String value) {
		getPrefs().put(getName(), value);
	}

	/** associates the given Objects toString() with this field within the Preferences */
	default void put(Object value) {
		getPrefs().put(getName(), value.toString());
	}

	/** returns the String associated with this field within the Preferences */
	public default String get() {
		return getPrefs().get(getName(), getDefault());
	}

	/** returns the boolean associated with this field within the Preferences */
	public default boolean getBool() {
		return Boolean.valueOf(get());
	}

	/** returns the int associated with this field within the Preferences */
	public default int getInt() {
		try {
			return Integer.valueOf(get());
		} catch(NumberFormatException e) {
			return 0;
		}
	}
	
	// MULTI MECHANICS!

	public default JCheckBox addCheckbox(String title, String key) {
		JCheckBox box = new JCheckBox(title, getMulti(key));
		box.addActionListener(a -> putMulti(key, box.isSelected()));
		return box;
	}
	
	static final String delimiter = ";";
	
	default void putMulti(String key, boolean value) {
		String cur = get();
		if(value)
			if(cur.isEmpty())
				put(key);
			else
				put(cur + delimiter + key);
		else {
			ArrayList<String> result = new ArrayList<>();
			for(String s : cur.split(delimiter)) {
				if(!s.contains(key))
					result.add(s);
			}
			put(String.join(delimiter, result));
		}
	}
	
	public default boolean getMulti(String key) {
		return get().contains(key);
	}
	
	public default String[] getAll() {
		return get().split(delimiter);
	}
	
	public default void resetPrefs() throws BackingStoreException {
		getPrefs().clear();
	}

	Preferences getPrefs();

	String getName();

	String getDefault();
	
	/* DEFAULT IMPLEMENTATION (enum Settings implements Setting)

	static Preferences PREFS = Preferences.userNodeForPackage(Settings.class);

	private final String name;
	private final String defaultVal;

	Settings(String name) {
		this(name, "");
	}

	Settings(String name, Object defaultValue) {
		this.name = name;
		defaultVal = defaultValue.toString();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDefault() {
		return defaultVal;
	}

	@Override
	public Preferences getPrefs() {
		return PREFS;
	}
	*/

}
