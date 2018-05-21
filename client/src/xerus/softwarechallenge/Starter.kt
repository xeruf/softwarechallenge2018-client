package xerus.softwarechallenge

import jargs.gnu.CmdLineParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sc.plugin2018.AbstractClient
import sc.plugin2018.IGameHandler
import sc.shared.SharedConfiguration
import xerus.ktutil.getResource
import xerus.ktutil.nullIfEmpty
import xerus.ktutil.reflectField
import xerus.softwarechallenge.util.debugLevel
import xerus.softwarechallenge.util.evolution
import xerus.softwarechallenge.util.strategy

lateinit var client: Client

fun main(args: Array<String>) {
	System.setProperty("file.encoding", "UTF-8")
	
	val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
	rootLogger.level = ch.qos.logback.classic.Level.WARN
	
	val parser = CmdLineParser()
	val hostOption = parser.addStringOption('h', "host")
	val portOption = parser.addIntegerOption('p', "port")
	val reservationOption = parser.addStringOption('r', "reservation")
	
	val clientOption = parser.addStringOption('c', "client")
	val strategyOption = parser.addStringOption('s', "strategy")
	val debugOption = parser.addIntegerOption('d', "debug")
	val evolutionOption = parser.addIntegerOption('e', "evolution")
	
	try {
		parser.parse(args)
	} catch (e: CmdLineParser.OptionException) {
		showHelp(e.toString())
		System.exit(2)
	}
	
	// Parameter laden
	val host = parser.getOptionValue(hostOption, "localhost") as String
	val port = parser.getOptionValue(portOption, SharedConfiguration.DEFAULT_PORT) as Int
	val reservation = parser.getOptionValue(reservationOption) as String?
	strategy = parser.getOptionValue(strategyOption) as String?
	debugLevel = parser.getOptionValue(debugOption, 1) as Int
	evolution = parser.getOptionValue(evolutionOption) as Int?
	
	val clientClass = (parser.getOptionValue(clientOption) as String?)
	val handler = Class.forName("xerus.softwarechallenge.logic2018.${clientClass 
			?: getResource("activeclient")?.readText()?.nullIfEmpty() ?: "Jumper1_8"}").kotlin.objectInstance as IGameHandler
	
	// einen neuen Client erzeugen
	try {
		client = Client(host, port, reservation, handler)
	} catch (e: Exception) {
		System.err.println("Beim Starten den Clients ist ein Fehler aufgetreten: " + e.message)
		System.exit(2)
	}
	
}

private fun showHelp(errorMsg: String?) {
	println("""$errorMsg
Usage: 
java -jar mississippi_queen_player.jar
				[{-h,--host} hostname]
				[{-p,--port} port]
				[{-r,--reservation} reservierung]
				[{-s,--strategy} strategie]
				[{-c,--client} client]
				
Example:
java -jar player.jar --host 127.0.0.1 --port 10500 --reservation MQ --strategy 10.0,5.0 --client Jumper1_6
""")
}


class Client(host: String, port: Int, reservation: String?, handler: IGameHandler) : AbstractClient(host, port) {
	
	init {
		setHandler(handler)
		if (reservation == null) {
			joinAnyGame()
		} else {
			joinPreparedGame(reservation)
		}
	}
	
	override fun onGameObserved(arg0: String) {}
	
}
