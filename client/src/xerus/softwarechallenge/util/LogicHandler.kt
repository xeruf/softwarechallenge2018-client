package xerus.softwarechallenge.util

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory
import sc.plugin2018.*
import sc.shared.*
import xerus.ktutil.*
import xerus.ktutil.helpers.Rater
import xerus.ktutil.helpers.Timer
import xerus.softwarechallenge.client
import java.io.File
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
abstract class LogicHandler : IGameHandler {
	
	@F protected val logger: Logger = LoggerFactory.getLogger(this.javaClass) as Logger
	
	@F protected var params = strategy?.split(',')?.map { it.toDouble() }?.toDoubleArray() ?: defaultParams()
	
	@F protected val rand: Random = SecureRandom()
	
	@F protected var currentState = GameState()
	
	@F protected val isDebug = debugLevel == 2
	
	protected inline val currentPlayer: Player
		get() = currentState.currentPlayer
	
	protected inline val currentTurn
		get() = currentState.turn
	
	@F val version = this.javaClass.simpleName + " " + getResource("version")!!.readText()
	
	init {
		logger.warn("$version - Parameter: ${params.joinToString()}")
		if (debugLevel == 2) {
			logger.level = Level.DEBUG
			logger.info("Debug enabled")
		} else if (debugLevel == 1) {
			logger.level = Level.INFO
			logger.info("Info enabled")
		}
		logger.info("JVM args: " + ManagementFactory.getRuntimeMXBean().inputArguments)
	}
	
	override fun onRequestAction() {
		Timer.start()
		validMoves = 0
		invalidMoves = 0
		depth = 0
		depthUsed = 0
		var move: Move? = try {
			currentState.predefinedMove()
		} catch (e: Throwable) {
			logger.error("Error in predefinedMove!", e)
			null
		}
		
		if (move.invalid()) {
			if (move != null)
				logger.error("Invalid predefined Move: ${move.str()}")
			move = try {
				findBestMove()
			} catch (e: Throwable) {
				logger.error("Error in findBestMove!", e)
				null
			}
		}
		
		if (move.invalid()) {
			if (move != null)
				logger.error("Invalid findBestMove: ${move.str()}")
			move = try {
				currentState.quickMove().first
			} catch (e: Throwable) {
				logger.error("Error in quickMove!", e)
				null
			}
		}
		
		if (move.invalid()) {
			logger.info("No valid Move: {} - using simpleMove!", move)
			move = currentState.simpleMove()
		}
		
		if (Timer.runtime() < 100) {
			logger.info("Invoking GC at ${Timer.runtime()}ms")
			System.gc()
		}
		sendAction(move)
		logger.info("Zeit: %sms Moves: %s/%s Tiefe: %s Genutzt: %s".format(Timer.runtime(), validMoves, invalidMoves, depth, depthUsed))
		currentLogDir?.renameTo(gameLogDir!!.resolve("turn$currentTurn - ${move?.str()}"))
		clear()
	}
	
	fun Move?.invalid() = this == null || currentState.test(this, true, false) == null
	
	/** log directory for this game */
	@F val gameLogDir = if (isDebug) Paths.get("games", SimpleDateFormat("MM-dd HH-mm-ss").format(Date()) + " $version").createDirs() else null
	/** log directory for the current turn*/
	val currentLogDir
		get() = gameLogDir?.resolve("turn$currentTurn")?.createDirs()
	
	protected fun GameState.quickMove(): Pair<Move, GameState> {
		val moves = findMoves()
		val best = Rater<Pair<Move, GameState>>()
		for (move in moves) {
			val moveState = clone()
			move.setOrderInActions()
			move.perform(moveState)
			moveState.nextPlayer(false)
			if (best.points < evaluate(moveState)) {
				moveState.nextPlayer()
				best.obj = Pair(move, moveState)
			}
		}
		return best.obj!!
	}
	
	/** if a predefined Move is appropriate then this method can return it, otherwise null */
	protected open fun GameState.predefinedMove(): Move? = null
	
	/** finds relevant moves for this [GameState] */
	protected abstract fun GameState.findMoves(): List<Move>
	
	protected abstract fun GameState.simpleMove(): Move
	
	/** findet den Move der beim aktuellen GameState am besten ist */
	protected abstract fun findBestMove(): Move?
	
	/** called after the Move is sent to allow resetting back to neutral */
	protected open fun clear() {}
	
	/** bewertet die gegebene Situation
	 * @return Einschätzung der gegebenen Situation in Punkten */
	abstract fun evaluate(state: GameState): Double
	
	abstract fun defaultParams(): DoubleArray
	
	@F protected var depth: Int = 0
	@F protected var depthUsed: Int = 0
	
	// GRUNDLAGEN
	
	override fun sendAction(move: Move?) {
		if (move == null) {
			logger.warn("Kein Zug möglich!")
			client.sendMove(Move())
			return
		}
		logger.info("Sende {}", move.str())
		move.setOrderInActions()
		client.sendMove(move)
	}
	
	protected lateinit var myColor: PlayerColor
	
	override fun onUpdate(state: GameState) {
		currentState = state
		val dran = state.currentPlayer
		if (!::myColor.isInitialized && client.color != null) {
			myColor = client.color
			logger.info("Ich bin {}", myColor)
		}
		logger.info("Zug: {} Dran: {} - " + dran.str(), state.turn, dran.playerColor.identify())
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
			"GameState: Zug %d\n - current: %s\n - other: %s".format(turn, currentPlayer.str(), otherPlayer.str())
	
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
	 * tests a Move against this [GameState] and then executes a move for the enemy player
	 *
	 * @param state gegebener State
	 * @param move  der zu testende Move
	 * @param clone if the state should be cloned prior to performing
	 * @return null, wenn der Move fehlerhaft ist, sonst den GameState nach dem Move
	 */
	protected fun GameState.test(move: Move, clone: Boolean = true, moveOther: Boolean = true): GameState? {
		val newState = clone()
		try {
			move.setOrderInActions()
			move.perform(newState)
			val bestState = Rater<GameState>()
			if (moveOther && newState.turn < 60 && newState.otherPlayer.fieldIndex != 64) {
				for (otherMove in newState.findMoves()) {
					val moveState = newState.clone()
					otherMove.setOrderInActions()
					try {
						otherMove.perform(moveState)
						moveState.nextPlayer(false)
						if (bestState.update(moveState, evaluate(moveState)))
							moveState.nextPlayer()
					} catch (exception: Throwable) {
						logger.warn("Fehler bei otherMove: ${otherMove.str()} for ${newState.currentPlayer.str()}: $exception\nnew ${newState.str()}\n" + if (clone) "prev " + this.str() else "Not cloned!")
					}
				}
			}
			
			validMoves++
			return bestState.obj ?: newState.apply {
				turn += 1
				switchCurrentPlayer()
			}
		} catch (e: InvalidMoveException) {
			invalidMoves++
			if (debugLevel > 0)
				logger.warn("FEHLERHAFTER ZUG: {} FEHLER: {}\n" + this.str(), move.str(), e.message)
		}
		return null
	}
	
	protected fun GameState.nextPlayer(forward: Boolean = true) {
		turn += if (forward) 1 else -1
		switchCurrentPlayer()
	}
	
	@F protected var validMoves: Int = 0
	@F protected var invalidMoves: Int = 0
	
	override fun gameEnded(data: GameResult, color: PlayerColor, errorMessage: String?) {
		val scores = data.scores
		val cause = "Ich %s Gegner %s".format(scores[color.ordinal].cause, scores[color.opponent().ordinal].cause)
		if (data.winners.isEmpty())
			logger.warn("Kein Gewinner! Grund: {}", cause)
		val winner = (data.winners[0] as Player).playerColor
		val score = getScore(scores, color)
		if (data.isRegular)
			logger.warn("Spiel beendet! Gewinner: ${winner.identify()} Punkte: $score Gegner: ${getScore(scores, color.opponent())}")
		else
			logger.warn("Spiel unregulaer beendet! Punkte: $score Grund: $cause")
		evolution?.let {
			File("result$it").writeText("${(color == winner).toInt()} $score")
		}
	}
	
	private fun getScore(scores: List<PlayerScore>, color: PlayerColor): Int =
			scores[color.ordinal].values[1].toInt()
	
	override fun onUpdate(arg0: Player, arg1: Player) {}
	
	private fun PlayerColor.identify(): String =
			if (this == myColor) "me" else "other"
	
}
