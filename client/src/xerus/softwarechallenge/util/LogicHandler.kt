package xerus.softwarechallenge.util

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory
import sc.plugin2018.*
import sc.shared.GameResult
import sc.shared.InvalidMoveException
import sc.shared.PlayerColor
import sc.shared.PlayerScore
import xerus.ktutil.createDirs
import xerus.ktutil.helpers.Timer
import xerus.ktutil.ifTrue
import xerus.ktutil.renameTo
import xerus.ktutil.toInt
import xerus.softwarechallenge.client
import java.io.File
import java.io.FileWriter
import java.lang.management.ManagementFactory
import java.nio.file.Paths
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sign

var strategy: String? = null
var debugLevel: Int = 1
var evolution: Int? = null

/** schafft Grundlagen fuer eine Logik */
abstract class LogicHandler(identifier: String) : IGameHandler {
	
	protected val log: Logger = LoggerFactory.getLogger(this.javaClass) as Logger
	
	@JvmField
	protected var params = strategy?.split(',')?.map { it.toDouble() }?.toDoubleArray() ?: defaultParams()
	
	@JvmField
	protected val rand: Random = SecureRandom()
	
	@JvmField
	protected var currentState = GameState()
	
	protected inline val currentPlayer: Player
		get() = currentState.currentPlayer
	
	protected inline val currentTurn
		get() = currentState.turn
	
	init {
		log.warn("$identifier - Parameter: ${params.joinToString()}")
		if (debugLevel == 2) {
			log.level = Level.DEBUG
			log.info("Debug enabled")
		} else if (debugLevel == 1) {
			log.level = Level.INFO
			log.info("Info enabled")
		}
		log.info("JVM args: " + ManagementFactory.getRuntimeMXBean().inputArguments)
	}
	
	override fun onRequestAction() {
		Timer.start()
		validMoves = 0
		invalidMoves = 0
		depth = 0
		lastdepth = 0
		var move: Move? = try {
			predefinedMove()
		} catch (e: Throwable) {
			log.error("Error in predefinedMove!", e)
			null
		}
		
		if (move.invalid()) {
			if (move != null)
				log.error("Invalid predefined Move: ${move.str()}")
			move = try {
				findBestMove()
			} catch (e: Throwable) {
				log.error("Error in findBestMove!", e)
				null
			}
		}
		
		if (move.invalid()) {
			log.info("No valid Move: {} - using simpleMove!", move)
			move = simpleMove(currentState)
		}
		
		if (Timer.runtime() < 100) {
			log.info("Invoking GC at ${Timer.runtime()}ms")
			System.gc()
		}
		sendAction(move)
		log.info("Zeit: %sms Moves: %s/%s Tiefe: %s Genutzt: %s".format(Timer.runtime(), validMoves, invalidMoves, depth, lastdepth))
		currentLogDir?.renameTo(gameLogDir!!.resolve("turn$currentTurn - ${move?.str()}"))
	}
	
	fun Move?.invalid() = this == null || currentState.test(this) == null
	
	// region Zugsuche
	
	@JvmField
	protected val gameLogDir = (debugLevel == 2).ifTrue { Paths.get("games", SimpleDateFormat("MM-dd HH-mm-ss").format(Date())).createDirs() }
	protected val currentLogDir
		get() = gameLogDir?.resolve("turn$currentTurn")?.createDirs()
	
	/** kann einen vordefinierten Zug zurückgeben oder null wenn nicht sinnvoll */
	protected open fun predefinedMove(state: GameState = currentState): Move? = null
	
	/** findet den Move der beim aktuellen GameState am besten ist */
	protected abstract fun findBestMove(): Move?
	
	protected abstract fun defaultParams(): DoubleArray
	
	@JvmField
	protected var depth: Int = 0
	@JvmField
	protected var lastdepth: Int = 0
	
	// GRUNDLAGEN
	
	override fun sendAction(move: Move?) {
		if (move == null) {
			log.warn("Kein Zug möglich!")
			client.sendMove(Move())
			return
		}
		log.info("Sende {}", move.str())
		move.setOrderInActions()
		client.sendMove(move)
	}
	
	protected lateinit var myColor: PlayerColor
	
	override fun onUpdate(state: GameState) {
		currentState = state
		val dran = state.currentPlayer
		if (!::myColor.isInitialized && client.color != null) {
			myColor = client.color
			log.info("Ich bin {}", myColor)
		}
		log.info("Zug: {} Dran: {} - " + dran.str(), state.turn, dran.playerColor.identify())
	}
	
	/*public static void display(GameState state) {
		JFrame frame = new JFrame();
		String fieldString = state.getBoard().toString();
		MyTable table = new ScrollableJTable("Index", "Field").addToComponent(frame, null);
		String[] fields = fieldString.split("index \\d+");
		for (int i = 1; i < fields.length - 1; i++) {
			table.addRow(i + "", fields[i]);
		}
		table.fitColumns(0);
		frame.pack();
		frame.setVisible(true);
	}*/
	
	abstract fun Player.str(): String
	
	fun GameState.str() =
			"GameState: Zug: %d\n - current: %s\n - other: %s".format(turn, currentPlayer.str(), otherPlayer.str())
	
	protected fun fieldTypeAt(index: Int): FieldType = currentState.getTypeAt(index)
	
	fun findField(type: FieldType, startIndex: Int = currentPlayer.fieldIndex + 1): Int {
		var index = startIndex
		while (fieldTypeAt(index) != type)
			index++
		return index
	}
	
	/** searches for the given [FieldType] around the [startIndex], back and forth starting in the front
	 * @return index of the nearest [Field] matching [type] in any direction */
	fun findCircular(type: FieldType, startIndex: Int): Int {
		var index = startIndex
		var dif = 1
		while (fieldTypeAt(index) != type) {
			index += dif
			dif = -(dif + dif.sign)
		}
		return index
	}
	
	// Zugmethoden
	
	/** performs the [action] on this [GameState]
	 * @return false if it fails */
	protected fun GameState.perform(action: Action): Boolean =
			try {
				action.perform(this)
				true
			} catch (e: InvalidMoveException) {
				false
			}
	
	/**
	 * tests a Move on the given [state]
	 *
	 * führt jetzt auch einen Move für den Gegenspieler aus!
	 *
	 * @param state gegebener State
	 * @param move  der zu testende Move
	 * @param clone if the state should be cloned prior to performing
	 * @return null, wenn der Move fehlerhaft ist, sonst den GameState nach dem Move
	 */
	protected open fun GameState.test(move: Move, clone: Boolean = true): GameState? {
		val newState = if (clone) clone() else this
		try {
			move.setOrderInActions()
			move.perform(newState)
			val turnIndex = newState.turn
			if (turnIndex < 60) {
				val simpleMove = simpleMove(newState)
				if (newState.currentPlayerColor == myColor)
					log.error("SEARCHING SIMPLEMOVE FOR ME!")
				try {
					simpleMove.perform(newState)
				} catch (exception: Throwable) {
					log.warn("Fehler bei simpleMove: ${simpleMove.str()} - ${this.otherPlayer.str()}: $exception\n${newState.str()}")
					newState.turn = turnIndex + 1
					newState.switchCurrentPlayer()
				}
			}
			
			validMoves++
			return newState
		} catch (e: InvalidMoveException) {
			invalidMoves++
			if (log.isDebugEnabled)
				log.warn("FEHLERHAFTER ZUG: {} FEHLER: {} " + this.str(), move.str(), e.message)
		}
		return null
	}
	
	private var validMoves: Int = 0
	private var invalidMoves: Int = 0
	
	protected abstract fun simpleMove(state: GameState): Move
	
	override fun gameEnded(data: GameResult, color: PlayerColor, errorMessage: String?) {
		val scores = data.scores
		val cause = "Ich %s Gegner %s".format(scores[color.ordinal].cause, scores[color.opponent().ordinal].cause)
		if (data.winners.isEmpty())
			log.warn("Kein Gewinner! Grund: {}", cause)
		val winner = (data.winners[0] as Player).playerColor
		val score = getScore(scores, color)
		if (data.isRegular)
			log.warn("Spiel beendet! Gewinner: %s Punkte: %s Gegner: %s".format(winner.identify(), score, getScore(scores, color.opponent())))
		else
			log.warn("Spiel unregulaer beendet! Punkte: %s Grund: %s".format(score, cause))
		evolution?.let {
			File("evolution/result$it").writeText("${(color == winner).toInt()} $score")
		}
	}
	
	private fun getScore(scores: List<PlayerScore>, color: PlayerColor): Int =
			scores[color.ordinal].values[1].toInt()
	
	override fun onUpdate(arg0: Player, arg1: Player) {}
	
	private fun PlayerColor.identify(): String =
			if (this == myColor) "me" else "other"
	
}
