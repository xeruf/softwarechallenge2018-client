package xerus.softwarechallenge;

import xerus.softwarechallenge.logic2018.Jumper1;
import jargs.gnu.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.plugin2018.AbstractClient;
import sc.plugin2018.IGameHandler;
import sc.shared.SharedConfiguration;

/**
 * Hauptklasse des Clients, die ueber Konsolenargumente gesteuert werden kann.
 * Sie veranlasst eine Verbindung zum Spielserver und waehlt eine Strategie.
 */
public class Starter extends AbstractClient {
	
	public Starter(String host, int port, String reservation, String strategy, int debuglevel)
			throws Exception {
		// client starten
		super(host, port);
		
		// strategie auswaehlen und zuweisen
		IGameHandler logic = new Jumper1(this, strategy, debuglevel);
		setHandler(logic);
		
		// einem spiel beitreten
		if (reservation == null || reservation.isEmpty()) {
			joinAnyGame();
		} else {
			joinPreparedGame(reservation);
		}
		
	}
	
	public static void main(String[] args) {
		System.setProperty("file.encoding", "UTF-8");
		
		// you may use this code to enable debug output:
		Logger rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		((ch.qos.logback.classic.Logger) rootLogger).setLevel(ch.qos.logback.classic.Level.WARN);
		
		// parameter definieren
		CmdLineParser parser = new CmdLineParser();
		CmdLineParser.Option hostOption = parser.addStringOption('h', "host");
		CmdLineParser.Option portOption = parser.addIntegerOption('p', "port");
		CmdLineParser.Option strategyOption = parser.addStringOption('s', "strategy");
		CmdLineParser.Option reservationOption = parser.addStringOption('r', "reservation");
		CmdLineParser.Option debugOption = parser.addIntegerOption('d', "debug");
		
		try {
			// Parameter auslesen
			parser.parse(args);
		} catch(CmdLineParser.OptionException e) {
			// Bei Fehler die Hilfe anzeigen
			showHelp(e.getMessage());
			System.exit(2);
		}
		
		// Parameter laden
		String host = (String) parser.getOptionValue(hostOption, "localhost");
		int port = (Integer) parser.getOptionValue(portOption, SharedConfiguration.DEFAULT_PORT);
		String reservation = (String) parser.getOptionValue(reservationOption, "");
		String strategy = (String) parser.getOptionValue(strategyOption, "");
		int debuglevel = (Integer) parser.getOptionValue(debugOption, 1);
		
		// einen neuen client erzeugen
		try {
			new Starter(host, port, reservation, strategy, debuglevel);
		} catch(Exception e) {
			System.err.println("Beim Starten den Clients ist ein Fehler aufgetreten:");
			e.printStackTrace();
		}
		
	}
	
	private static void showHelp(String errorMsg) {
		System.out.println(errorMsg);
		System.out.println("Bitte das Programm mit folgenden Parametern (optional) aufrufen: \n"
				+ "java -jar mississippi_queen_player.jar [{-h,--host} hostname]\n"
				+ "							   [{-p,--port} port]\n"
				+ "							   [{-r,--reservation} reservierung]\n"
				+ "							   [{-s,--strategy} strategie]");
		System.out.println();
		System.out.println("Beispiel: \n"
				+ "java -jar mississippi_queen_player.jar --host 127.0.0.1 --port 10500 --reservation MQ --strategy RANDOM");
		System.out.println();
	}
	
	@Override
	public void onGameObserved(String arg0) {
	}
	
}
