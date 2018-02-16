package xerus.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.logging.*;

import xerus.util.tools.Tools;

public class SimpleLogger {

	private static Logger logger = Logger.getLogger("sysout");;

	public static Logger getLogger() {
		return getLogger(Level.INFO);
	}

	public static Logger getLogger(String level) {
		return getLogger(Level.parse(level));
	}

	public static Logger getLogger(Level loglevel) {
		logger.setLevel(loglevel);
		if(logger.getHandlers().length == 0) {
			logger.addHandler(createHandler(System.out));
			logger.setUseParentHandlers(false);
			LogManager.getLogManager().addLogger(logger);
			System.out.println("SimpleLogger created at level " + loglevel);
		} else {
			System.out.println("SimpleLogger set to level " + loglevel);
		}
		return logger;
	}

	public static void addOutputstream(OutputStream out) {
		logger.addHandler(createHandler(out));
	}

	public static StreamHandler createHandler(OutputStream out) {
		return new StreamHandler(out, new ShortFormatter()) {
			@Override
			public synchronized void publish(final LogRecord record) {
				super.publish(record);
				flush();
			}
		};
	}

	private static class ShortFormatter extends Formatter {

		@Override
		public String format(LogRecord record) {
			return String.format("%s %s %s%s", Tools.time(), record.getLevel().toString(), formatMessage(record), System.getProperty("line.separator"));
		}

	}

	public static class SysoutListener {

		private static ArrayList<SysoutObserver> observers = new ArrayList<>();
		static {
			coverSysout();
			System.out.println("SysoutListener initialized");
		}

		public static void addObserver(SysoutObserver observer) {
			if(observers == null) {
				observers = new ArrayList<>();
				coverSysout();
			}
			observers.add(observer);
		}

		public static void removeObserver(SysoutObserver observer) {
			if(observers == null)
				return;
			observers.remove(observer);
		}

		private static void fireSysout(String message) {
			for (int i = 0; i < observers.size(); i++) {
				SysoutObserver observer = observers.get(i);
				java.awt.EventQueue.invokeLater(() -> observer.handle(message));
			}
		}

		private static void coverSysout() {
			System.setOut(new PrintStream(System.out) {
				@Override
				public void println(String s) {
					super.println(s);
					fireSysout(s);
				}
			});
		}

	}

	public interface SysoutObserver {
		public void handle(String message);
	}

}
