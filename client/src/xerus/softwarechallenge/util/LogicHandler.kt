package xerus.softwarechallenge.util

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory
import sc.plugin2018.*
import sc.shared.GameResult
import sc.shared.InvalidMoveException
import sc.shared.PlayerColor
import sc.shared.PlayerScore
import xerus.ktutil.createDir
import xerus.ktutil.createDirs
import xerus.ktutil.helpers.Timer
import xerus.ktutil.renameTo
import xerus.softwarechallenge.client
import java.lang.management.ManagementFactory
import java.nio.file.Path
import java.nio.file.Paths
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.sign

var strategy: String? = null
var debugLevel: Int = 1

/** schafft Grundlagen fuer eine Logik */
abstract class LogicHandler(identifier: String) : IGameHandler {
	
	protected val log: Logger = LoggerFactory.getLogger(this.javaClass) as Logger
	
	protected lateinit var currentState: GameState
	
	protected inline val currentPlayer: Player
		get() = currentState.currentPlayer
	
	protected inline val currentTurn
		get() = currentState.turn
	
	protected var params = strategy?.split(',')?.map { it.toDouble() }?.toDoubleArray() ?: defaultParams()
	
	val rand: Random = SecureRandom()
	
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
			null//predefinedMove()
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
			log.info("No valid Move: {} - Using simpleMove!", move)
			move = simpleMove(currentState)
		}
		
		sendAction(move)
		log.info("Zeit: %sms Moves: %s/%s Tiefe: %s Genutzt: %s".format(Timer.runtime(), validMoves, invalidMoves, depth, lastdepth))
		currentLogDir?.renameTo(gameLogDir!!.resolve("turn$currentTurn - ${move?.str()}"))
	}
	
	fun Move?.invalid() = this == null || currentState.test(this) == null
	
	// region Zugsuche
	
	/** kann einen vordefinierten Zug zurückgeben oder null wenn nicht sinnvoll */
	protected open fun predefinedMove(): Move? = null
	
	/**Findet den Move der beim aktuellen GameState am besten ist<br></br>
	 * verweist standardmäßig auf die breitensuche */
	protected open fun findBestMove(): Move? = breitensuche()
	
	/** bewertet die gegebene Situation
	 * @return Einschätzung der gegebenen Situation in Punkten */
	protected abstract fun evaluate(state: GameState): Double
	
	/** Die Standard-Parameter - gibt in der Basisimplementierung ein leeres Array zurück */
	protected open fun defaultParams() = doubleArrayOf()
	
	private var depth: Int = 0
	private var lastdepth: Int = 0
	
	private val gameLogDir = if (log.isDebugEnabled) Paths.get("games", SimpleDateFormat("MM-dd HH-mm-ss").format(Date())).createDirs() else null
	private val currentLogDir
		get() = gameLogDir?.resolve("turn$currentTurn")?.createDirs()
	
	/** sucht den besten Move per Breitensuche basierend auf dem aktuellen GameState */
	private fun breitensuche(): Move? {
		// Variablen vorbereiten
		val queue = LinkedList<Node>()
		val mp = MP()
		var bestMove: Move
		var moves = findMoves(currentState)
		
		for (move in moves) {
			val newState = currentState.test(move) ?: continue
			if (gewonnen(newState))
				return move
			// Punkte
			val points = evaluate(newState)
			mp.update(move, points)
			// Queue
			if (!gewonnen(newState, newState.otherPlayer)) {
				val newnode = Node(move, newState, points)
				queue.add(newnode)
			}
		}
		
		bestMove = mp.obj ?: moves.first()
		if (queue.size < 2) {
			System.gc()
			log.debug("Nur einen validen Zug gefunden: {}", bestMove.str())
			return bestMove
		}
		
		// Breitensuche
		mp.clear()
		depth = 1
		var maxDepth = 5.coerceAtMost(62.minus(currentTurn) / 2)
		var node = queue.poll()
		loop@ while (depth < maxDepth && Timer.runtime() < 1000 && queue.size > 0) {
			depth = node.depth
			val divider = depth.toDouble().pow(0.3)
			do {
				val nodeState = node.gamestate
				moves = findMoves(nodeState)
				for (i in 0..moves.lastIndex) {
					if (Timer.runtime() > 1600)
						break@loop
					val move = moves[i]
					val newState = nodeState.test(move, i < moves.lastIndex) ?: continue
					// Punkte
					val points = evaluate(newState) / divider + node.points
					if (points < mp.points - 50 / divider)
						continue
					mp.update(node.move, points)
					// Queue
					if (newState.turn > 59 || gewonnen(newState))
						maxDepth = depth
					if (!gewonnen(newState, newState.otherPlayer) && depth < maxDepth) {
						val newNode = node.update(newState, points, move)
						queue.add(newNode)
					}
				}
				node = queue.poll() ?: break
			} while (depth == node.depth)
			lastdepth = depth
			bestMove = mp.obj!!
			log.debug("Neuer bester Zug bei Tiefe {}: {}", depth, bestMove.str())
		}
		return bestMove
	}
	
	private inner class Node private constructor(val move: Move, val gamestate: GameState, val points: Double, val depth: Int, val dir: Path?) {
		
		constructor(move: Move, state: GameState, points: Double = 0.0) : this(move, state, points, 1,
				gameLogDir?.resolve("turn$currentTurn")?.resolve("%.1f - %s".format(points, move.str())))
		
		init {
			dir?.createDirs()
		}
		
		fun update(newState: GameState, newPoints: Double, addedMove: Move) =
				Node(move, newState, newPoints, depth + 1, dir?.resolve("%.1f - %s".format(newPoints, addedMove.str())))
		
		override fun toString() = "Node depth %d %s points %.1f".format(depth, move.str(), points)
		
	}
	
	/**
	 * stellt mögliche Moves zusammen basierend auf dem gegebenen GameState
	 *
	 * muss überschrieben werden um die [breitensuche] zu nutzen
	 *
	 * @param state gegebener GameState
	 * @return ArrayList mit gefundenen Moves
	 */
	protected open fun findMoves(state: GameState): List<Move> =
			throw UnsupportedOperationException("Es wurde keine Methode für das ermitteln von moves definiert!")
	
	// GRUNDLAGEN
	
	override fun sendAction(move: Move?) {
		if (move == null) {
			log.warn("Kein Zug möglich!")
			client.sendMove(Move())
			return
		}
		log.debug("Sende {}", move.str())
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
		log.info("Zug: {} Dran: {} - " + dran.str(), state.turn, identify(dran.playerColor))
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
	
	protected abstract fun gewonnen(state: GameState, player: Player = state.currentPlayer): Boolean
	
	abstract fun Player.str(): String
	
	fun GameState.str() =
			"GameState: Zug: %d\n - current: %s\n - other: %s".format(turn, currentPlayer.str(), otherPlayer.str())
	
	protected inline fun fieldTypeAt(index: Int): FieldType = currentState.getTypeAt(index)
	
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
	 * führt jetzt auch einen simplemove für den Gegenspieler aus!
	 *
	 * @param state gegebener State
	 * @param move  der zu testende Move
	 * @param clone if the state should be cloned prior to performing
	 * @return null, wenn der Move fehlerhaft ist, sonst den GameState nach dem Move
	 */
	protected fun GameState.test(move: Move, clone: Boolean = true): GameState? {
		val newState = if (clone) clone() else this
		try {
			move.setOrderInActions()
			move.perform(newState)
			val turnIndex = newState.turn
			if (turnIndex < 60) {
				val simpleMove = simpleMove(newState)
				try {
					simpleMove.perform(newState)
				} catch (exception: Throwable) {
					log.warn("Fehler bei simpleMove ${simpleMove.str()} - ${this.otherPlayer.str()}: $exception\n${newState.str()}")
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
		if (data.winners.isEmpty()) {
			log.warn("Kein Gewinner! Grund: {}", cause)
			// System.exit(0);
		}
		val winner = (data.winners[0] as Player).playerColor
		val myscore = getScore(scores, color)
		if (data.isRegular)
			log.warn("Spiel beendet! Gewinner: %s Punkte: %s Gegner: %s".format(identify(winner), myscore, getScore(scores, color.opponent())))
		else
			log.warn("Spiel unregulaer beendet! Punkte: %s Grund: %s".format(myscore, cause))
		// System.exit((color == winner ? 100 : 0) + myscore)
	}
	
	private fun getScore(scores: List<PlayerScore>, color: PlayerColor): Int =
			scores[color.ordinal].values[1].toInt()
	
	override fun onUpdate(arg0: Player, arg1: Player) {}
	
	private fun identify(color: PlayerColor): String =
			if (color == myColor) "ich" else "nicht ich"
	
}
