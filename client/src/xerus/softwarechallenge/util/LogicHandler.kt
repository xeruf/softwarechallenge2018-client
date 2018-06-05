@file:Suppress("NOTHING_TO_INLINE")

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
			try {
				move = currentState.quickMove()?.first
			} catch (e: Throwable) {
				logger.error("Error in quickMove!", e)
			}
		}
		
		if (move.invalid()) {
			logger.error("No valid quickMove: {} - using simpleMove!", move)
			move = currentState.simpleMove()
		}
		
		if (Timer.runtime() < 100) {
			logger.info("Invoking GC at ${Timer.runtime()}ms")
			System.gc()
		}
		sendAction(move)
		logger.info("Zeit: %sms Moves: %s/%s Tiefe: %s Genutzt: %s".format(Timer.runtime(), validMoves, invalidMoves, depth, depthUsed))
		currentLogDir?.renameTo(gameLogDir!!.resolve("turn$currentTurn - ${move?.str()} - ${currentPlayer.str()}"))
		clear()
	}
	
	fun Move?.invalid() = this == null || currentState.test(this, true, false) == null
	
	/** log directory for this game */
	@F val gameLogDir = if (isDebug) Paths.get("games", SimpleDateFormat("MM-dd HH-mm-ss").format(Date()) + " $version").createDirs() else null
	/** log directory for the current turn*/
	val currentLogDir
		get() = gameLogDir?.resolve("turn$currentTurn")?.createDirs()
	
	protected fun GameState.quickMove(recurse: Boolean = true): Pair<Move, GameState>? {
		val moves = findMoves()
		val best = Rater<Pair<Move, GameState>>()
		var done: Boolean
		for (move in moves) {
			val moveState = clone()
			move.setOrderInActions()
			try {
				move.perform(moveState)
				done = false
				if (turn < 58 && recurse) {
					val newState = moveState.quickMove(false)?.second
					if (newState != null) {
						done = true
						for (move2 in newState.findMoves()) {
							val moveState2 = newState.clone()
							move2.setOrderInActions()
							try {
								move2.perform(moveState2)
								best.update(Pair(move, moveState), evaluate(moveState2, currentPlayerColor))
							} catch (exception: Exception) {
								logger.error("Fehler bei quickMove2: ${move2.str()} caused $exception\n" + str())
							}
						}
					} else logger.error("Fehler bei quickMove2: No quickmove found!\n" + str())
				}
				if (!done)
					best.update(Pair(move, moveState), evaluate(moveState, currentPlayerColor))
			} catch (exception: Exception) {
				logger.error("Fehler bei quickMove: ${move.str()} caused $exception\n" + str())
			}
		}
		return best.obj
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
	abstract fun evaluate(state: GameState, color: PlayerColor = myColor): Double
	
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
			logger.info("Ich bin {}, " + state.str(), myColor)
		}
		logger.info("Zug: {} Dran: {} - " + dran.str(), state.turn, dran.playerColor.identify())
	}
	
	fun Player.str() = strShort() + " [${cards.joinToString { it.name }}] Last: ${lastNonSkipAction?.str()}"
	
	protected inline fun Player.strShort() =
			"$playerColor on $fieldIndex=${fieldTypeAt(fieldIndex).str()} ❦$carrots ❀$salads"
	
	protected inline fun Player.strShortest() =
			"$fieldIndex=${fieldTypeAt(fieldIndex).str()} ❦$carrots"
	
	fun GameState.str() =
			"GameState: Turn $turn ${this.board.track.joinToString(", ", "Track[", "]") { "${it.index} ${it.type}" }}\n" +
					" - current: ${currentPlayer.str()}\n" +
					" - other: ${otherPlayer.str()}"
	
	fun GameState.strShort() =
			"GameState: Turn $turn - Current: ${currentPlayer.strShort()} - Other: ${otherPlayer.strShort()}"
	
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
	 * tests a Move against this [GameState] and then executes a move for the enemy player if requested
	 *
	 * @param clone if the state should be cloned prior to performing
	 * @param moveOther
	 * @return null if there was an error, otherwise the GameState after the performing the Move
	 */
	protected fun GameState.test(move: Move, clone: Boolean = true, moveOther: Boolean = true): GameState? {
		val newState = clone()
		try {
			move.setOrderInActions()
			move.perform(newState)
			val bestState = Rater<GameState>()
			if (moveOther) {
				if (newState.turn < 60 && newState.otherPlayer.fieldIndex != 64) {
					for (otherMove in newState.findMoves()) {
						val moveState = newState.clone()
						otherMove.setOrderInActions()
						try {
							otherMove.perform(moveState)
							bestState.update(moveState, evaluate(moveState, myColor.opponent()))
						} catch (exception: Throwable) {
							logger.error("Fehler bei otherMove: ${otherMove.str()} for ${newState.currentPlayer.str()}: $exception\nnew ${newState.str()}\n" + if (clone) "prev " + this.str() else "Not cloned!")
							newState.nextPlayer()
						}
					}
				} else {
					newState.nextPlayer(newState.turn < 60)
				}
			}
			
			validMoves++
			return bestState.obj ?: newState
		} catch (e: InvalidGameStateException) {
			logger.error("$e ${move.str()} current: $currentTurn")
		} catch (e: InvalidMoveException) {
			if (debugLevel > 0)
				logger.error("FEHLERHAFTER ZUG: {} FEHLER: {}\n" + this.str(), move.str(), e.message)
		} catch (e: Throwable) {
			logger.error("Testing ${move.str()} failed!", e)
		}
		invalidMoves++
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
		val regular = data.isRegular
		if (regular)
			logger.warn("Spiel beendet! Gewinner: ${winner.identify()} Punkte: $score Gegner: ${getScore(scores, color.opponent())}")
		else
			logger.warn("Spiel unregulaer beendet! Punkte: $score Grund: $cause")
		evolution?.let {
			File("result$it").writeText("${regular.to((color == winner).toInt(), -1)} $score")
		}
	}
	
	private fun getScore(scores: List<PlayerScore>, color: PlayerColor): Int =
			scores[color.ordinal].values[1].toInt()
	
	override fun onUpdate(arg0: Player, arg1: Player) {}
	
	private fun PlayerColor.identify(): String =
			if (this == myColor) "me" else "other"
	
}
