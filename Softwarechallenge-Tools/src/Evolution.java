import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.IllegalOptionValueException;
import jargs.gnu.CmdLineParser.UnknownOptionException;
import xerus.util.SysoutListener;
import xerus.util.tools.FileTools;
import xerus.util.tools.StringTools;
import xerus.util.tools.Tools;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Evolution {

	private static String basepath;
	private static String ailoc;
	private static String datafile;
	private static String bestfile;

	private static boolean debug;

	public static void main(String[] args) throws IOException, InterruptedException, UnknownOptionException, IllegalOptionValueException {
		CmdLineParser parser = new CmdLineParser();
		CmdLineParser.Option option = parser.addIntegerOption('l', "line");
		CmdLineParser.Option path = parser.addStringOption('p', "path");
		CmdLineParser.Option debugOption = parser.addBooleanOption('d', "debug");
		parser.parse(args);

		basepath = (String) parser.getOptionValue(path, System.getProperty("user.dir") + "/");
		debug = (Boolean) parser.getOptionValue(debugOption, false);
		Process server = startServer();
		ailoc = basepath + "Elbdampfer3.4.jar";
		datafile = basepath + "strategies.csv";
		bestfile = basepath + "strategies_best.csv";
		FileTools.handle(e -> System.out.println(e.getMessage()));

		try {
			new Evolution((Integer) parser.getOptionValue(option, 0));
			while (true)
				new Evolution();
		} finally {
			server.destroy();
		}
	}

	private static Process startServer() throws IOException {
		ProcessBuilder serverbuilder = new ProcessBuilder(basepath + "testserver/start.bat");
		serverbuilder.directory(new File(basepath + "testserver"));
		serverbuilder.redirectErrorStream(true);
		return serverbuilder.start();
	}

	Strategy strategy;
	int line;

	public Evolution(int lineparam) throws InterruptedException {
		String[] file = FileTools.readall(datafile, 0, true).toArray(new String[0]);
		line = lineparam;
		if (lineparam == 0) {
			line = file.length;
			strategy = new Strategy(file[0]);
		} else {
			System.out.println("Lese von Zeile " + line);
			strategy = new Strategy(false, file[line]);
		}
		try {
			while (strategy.games < 200) {
				Process AI = startAIs();
				started = false;
				SysoutListener.addObserver(e -> {
					if (e.contains("Ich bin")) working();
				});
				Thread.sleep(6000);
				if (!started)
					buildAI().start();
				if (AI.waitFor(2, TimeUnit.MINUTES))
					strategy.write(AI.exitValue());
			}
			strategy.writeEnd("Finished");
		} catch (IOException e) {
			strategy.writeEnd("Error:" + e.getMessage());
		}
	}

	private boolean started;

	public void working() {
		started = true;
	}

	public Evolution() throws InterruptedException {
		this(0);
	}

	private Process startAIs() throws IOException {
		buildAI().start();
		ProcessBuilder pb = buildAI();
		if (debug)
			Collections.addAll(pb.command(), "-s", strategy.joinparams());
		else
			Collections.addAll(pb.command(), "-s", strategy.joinparams(), "-d", "0");
		return pb.inheritIO().start();
	}

	private static ProcessBuilder buildAI() {
		return new ProcessBuilder("java", "-jar", ailoc);
	}

	private class Strategy {
		double[] params;
		double[] variation;
		double winrate;
		int games;
		int won;
		int score;

		Strategy(String input) {
			this(true, input);
		}

		Strategy(boolean mutate, String info) {
			String[] infos = info.split(";");
			params = StringTools.split(infos[0]);
			variation = StringTools.split(infos[1]);
			if (mutate)
				mutate();
			else {
				c = 2;
				score = parseInt(infos);
				games = parseInt(infos);
				won = parseInt(infos);
			}
			write(0);
		}

		void mutate() {
			for (int i = 0; i < params.length; i++) {
				variation[i] = Tools.round((Math.random() * 2 - 1) * variation[i]);
				params[i] += variation[i];
			}
			games = 0;
			won = 0;
			score = 0;
		}

		int bestline;

		void write(int exitvalue) {
			if (exitvalue > 0) {
				games++;
				if (exitvalue / 100 >= 1)
					won++;
				score += exitvalue % 100;
			}

			String towrite = toString();
			if (FileTools.write(datafile, line, towrite))
				System.out.println(String.format("%s auf Zeile %s geschrieben ", towrite, line));
			if (games > 30) {
				if (winrate < 0.45)
					resetStrategy();
				if (games > 100) {
					if (winrate < 0.5) {
						resetStrategy();
					} else if (winrate > 0.52) {
						if (bestline == 0) {
							List<String> file = FileTools.readall(bestfile, 0, false);
							bestline = file.size();
						}
						FileTools.write(bestfile, bestline, towrite);
					}
				}
			}
		}

		void resetStrategy() {
			String file = null;
			while (file == null)
				file = FileTools.read(datafile, 0);
			strategy = new Strategy(file);
		}

		void writeEnd(String msg) {
			System.out.println("Fertig!");
			while (!FileTools.write(datafile, line, toString() + ";" + msg))
				Tools.sleep(10);
		}

		@Override
		public String toString() {
			winrate = Tools.round((double) won / games);
			return StringTools.join(joinparams(), StringTools.join(",", variation), "" + winrate, "" + score, "" + games, "" + won, Tools.time());
		}

		String joinparams() {
			return StringTools.join(",", params);
		}

		int c;

		private int parseInt(String[] s) {
			c++;
			if (s.length > c)
				return Integer.parseInt(s[c]);
			return 0;
		}

	}

}
