package xerus.util.tools;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

public class FileTools {

	public static File findExisting(File f) {
		while (f != null && !f.exists())
			f = f.getParentFile();
		if (f == null)
			f = new File(System.getProperty("user.dir"));
		return f;
	}

	public static Path getPath(Object o) {
		if(o instanceof Path)
			return (Path) o;
		return Paths.get(o.toString());
	}

	public static String removeExtension(String filename) {
		return filename.substring(0, filename.lastIndexOf("."));
	}

	public static String attachFilename(String path, String attachment) {
		int dot = path.lastIndexOf(".");
		return path.substring(0, dot) + attachment + path.substring(dot);
	}

	// == READ INDEPENDENTLY ==

	/**reads the whole file, trying 3 times
	 * @return Array of the file's lines */
	public static String[] readall(String file) {
		List<String> lines = readall(file, 3, false);
		return lines == null ? null : lines.toArray(new String[0]);
	}

	/**reads the whole file, tries up to {@code maxattempts} times
	 * @param maxattempts maximum number of attempts - if i is set to 0 it will try until it succeeds
	 * @return Array of the file's lines */
	public static List<String> readall(String file, int maxattempts, boolean warn) {
		int attempts = 0;
		while(attempts < maxattempts || maxattempts == 0) {
			try {
				return Files.readAllLines(Paths.get(file));
			} catch (IOException e) {
				attempts++;
				if(warn)
					System.out.println(String.format("Could not access %s at attempt %s", file, attempts));
				Tools.sleep(20 * attempts);
			}
		}
		System.out.println(String.format("Could not access %s after %s attempts", file, attempts));
		return null;
	}

	public static long lines(String file) {
		while(true)
			try {
				return Files.lines(Paths.get(file)).count();
			} catch (IOException e) { }
	}

	private static Consumer<Exception> handler = e -> e.printStackTrace();
	public static void handle(Consumer<Exception> newHandler) {
		handler = newHandler;
	}

	/**reads a specific line from a document
	 * @param line line number, counting starts at 0
	 * @return specified line */
	public static String read(String file, int line) {
		String ausgabe = null;
		try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
			for(int i = 0; i < line+1; i++)
				ausgabe = reader.readLine();
		} catch (IOException e) { handler.accept(e); }
		return ausgabe;
	}

	// == READ STEP-BY-STEP ==

	private static BufferedReader br;

	/** starts the reader on the given file
	 * @return true if successful, false if the file is not found */
	public static boolean openFile(String file) {
		try {
			br = new BufferedReader(new FileReader(file));
			return true;
		} catch(FileNotFoundException e) {
			handler.accept(e);
			return false;
		}
	}

	/**reads a line from the current reader position
	 * @return next line
	 */
	public static String readln() {
		try {
			return br.readLine();
		} catch (IOException e) { handler.accept(e); }
		return null;
	}

	/**advances the reader
	 * @param steps amount of lines to advance */
	public static void advancepos(int steps) {
		try {
			for(int i=0; i<steps; i++)
				br.readLine();
		} catch (IOException e) { handler.accept(e); }
	}

	// == WRITE ==

	/**write {@code text} and a linebreak after each element into the given file
	 * @param file name of the file
	 * @param text the text to append
	 * @param append if the writer should append on an existing file
	 * @returns true if successful, false if the file can't be opened
	 */
	public static boolean write(String file, boolean append, String... text) {
		try(Writer w = new FileWriter(file, append)) {
			for(String line : text)
				w.write(line + "\n");
			return true;
		} catch(IOException e) {
			handler.accept(e);
			return false;
		}
	}

	public static boolean write(String file, int line, String text) {
		String[] cur = readall(file);
		if(line > cur.length-1)
			return write(file, true, StringUtils.repeat("\n", line-cur.length) + text);
		try(Writer w = new FileWriter(file)) {
			for (int i = 0; i < cur.length; i++) {
				if(i == line)
					writeln(w, text);
				else
					writeln(w, cur[i]);
			}
			return true;
		} catch(IOException e) {
			handler.accept(e);
			return false;
		}
	}

	public static void writeln(Writer w, String text) throws IOException {
		w.write(text + System.lineSeparator());
	}

	public static void writeln(Writer w, Object text) throws IOException {
		writeln(w, text.toString());
	}

}
