package xerus.util.ui;

public interface LogArea {
	
	void appendText(String s);

	public default void appendln(String str) {
		appendText(str);
		appendln();
	}
	
	default void appendln() {
		appendText("\n");
	}

	default void appendAll(String prefix, String... args) {
		for (String arg : args)
			appendln(prefix + arg);
	}

	default void log(String format, Object... args) {
		appendln(String.format(format, args));
	}
	
}
