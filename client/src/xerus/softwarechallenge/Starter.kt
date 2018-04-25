package xerus.softwarechallenge

import xerus.softwarechallenge.logic2018.Jumper1
import jargs.gnu.CmdLineParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sc.plugin2018.AbstractClient
import sc.shared.SharedConfiguration
import xerus.softwarechallenge.util.debugLevel
import xerus.softwarechallenge.util.strategy

lateinit var client: Client

fun main(args: Array<String>) {
	System.setProperty("file.encoding", "UTF-8")
	
	val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
	(rootLogger as ch.qos.logback.classic.Logger).level = ch.qos.logback.classic.Level.WARN
	
	val parser = CmdLineParser()
	val hostOption = parser.addStringOption('h', "host")
	val portOption = parser.addIntegerOption('p', "port")
	val strategyOption = parser.addStringOption('s', "strategy")
	val reservationOption = parser.addStringOption('r', "reservation")
	val debugOption = parser.addIntegerOption('d', "debug")
	
	try {
		parser.parse(args)
	} catch (e: CmdLineParser.OptionException) {
		showHelp(e.message)
		System.exit(2)
	}
	
	// Parameter laden
	val host = parser.getOptionValue(hostOption, "localhost") as String
	val port = parser.getOptionValue(portOption, SharedConfiguration.DEFAULT_PORT) as Int
	val reservation = parser.getOptionValue(reservationOption, null) as String?
	strategy = parser.getOptionValue(strategyOption, null) as String?
	debugLevel = parser.getOptionValue(debugOption, 1) as Int
	
	// einen neuen Client erzeugen
	try {
		client = Client(host, port, reservation)
	} catch (e: Exception) {
		System.err.println("Beim Starten den Clients ist ein Fehler aufgetreten: " + e.message)
		System.exit(1)
	}
	
}

class Client(host: String, port: Int, reservation: String?) : AbstractClient(host, port) {
	
	init {
		setHandler(Jumper1())
		
		if (reservation == null) {
			joinAnyGame()
		} else {
			joinPreparedGame(reservation)
		}
	}
	
	override fun onGameObserved(arg0: String) {}
	
}

private fun showHelp(errorMsg: String?) {
	println(errorMsg)
	println("Bitte das Programm mit folgenden Parametern (optional) aufrufen: \n"
			+ "java -jar mississippi_queen_player.jar [{-h,--host} hostname]\n"
			+ "							   [{-p,--port} port]\n"
			+ "							   [{-r,--reservation} reservierung]\n"
			+ "							   [{-s,--strategy} strategie]")
	println()
	println("Beispiel: \n" + "java -jar player.jar --host 127.0.0.1 --port 10500 --reservation MQ --strategy RANDOM")
	println()
}
