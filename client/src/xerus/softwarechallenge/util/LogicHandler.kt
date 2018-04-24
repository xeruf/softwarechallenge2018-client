package xerus.softwarechallenge.util

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory
import sc.plugin2018.*
import sc.shared.GameResult
import sc.shared.InvalidMoveException
import sc.shared.PlayerColor
import sc.shared.PlayerScore
import xerus.ktutil.create
import xerus.ktutil.helpers.Timer
import xerus.softwarechallenge.Starter
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.lang.management.ManagementFactory
import java.nio.file.Paths
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.sign

/** schafft Grundlagen fuer eine Logik */
abstract class LogicHandler(private val client: Starter, params: String, debug: Int, identifier: String): IGameHandler {
	
	protected val log: Logger = LoggerFactory.getLogger(this.javaClass) as Logger
	protected lateinit var currentState: GameState
	protected val currentPlayer: Player
		get() = currentState.currentPlayer
	
	protected var params = if (params.isNotEmpty()) params.split(',').map { it.toDouble() }.toDoubleArray() else defaultParams()
	
	val rand: Random = SecureRandom()
	
	init {
		log.warn("$identifier - Parameter: $params")
		if (debug == 2) {
			log.level = Level.DEBUG
			log.info("Debug enabled")
		} else if (debug == 1) {
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
	
	private val gameLog = if (log.isDebugEnabled) Paths.get("games", SimpleDateFormat("MM-dd-HH-mm-ss").format(Date())).create() else Paths.get("")
	
	/** sucht den besten Move per Breitensuche basierend auf dem aktuellen GameState */
	private fun breitensuche(): Move? {
		// Variablen vorbereiten
		val queue = LinkedList<Node>()
		val mp = MP()
		var bestMove: Move
		
		// Queue füllen
		var moves = findMoves(currentState)
		if (moves.size == 1) {
			val move = moves.iterator().next()
			log.debug("Nur einen Zug gefunden: {}", move.str())
			return move
		}
		val debugFile = gameLog.resolve("turn${currentState.turn}.md").toFile()
		var debug: BufferedWriter? = null
		if (log.isDebugEnabled) {
			log.debug("Gefundene Zuege:\n{}", moves.str())
			debug = FileOutputStream(debugFile, true).bufferedWriter()
			debug.appendln(currentPlayer.str())
		}
		
		for (move in moves) {
			val newState = currentState.test(move) ?: continue
			if (gewonnen(newState))
				return move
			// Punkte
			val points = evaluate(newState)
			mp.update(move, points)
			// Queue
			if (!gewonnen(newState, newState.otherPlayer)) {
				val newnode = Node(newState, move, points)
				queue.add(newnode)
				// Debug
				debug?.appendln(newnode.toString())
			}
		}
		bestMove = mp.obj ?: moves.first()
		if (queue.size == 1) {
			log.debug("Nur einen validen Zug gefunden: {}", bestMove.str())
			debugFile.delete()
			return bestMove
		}
		
		// Breitensuche
		mp.clear()
		depth = 1
		var maxDepth = 5.coerceAtMost(62.minus(currentState.turn) / 2)
		var node = queue.poll()
		loop@ while (depth < maxDepth && Timer.runtime() < 1000 && queue.size > 0) {
			depth = node.depth
			val multiplicator = depth.toDouble().pow(0.4)
			do {
				val nodeState = node.gamestate
				debug?.appendln("##### $node ${nodeState.str()}")
				moves = findMoves(nodeState)
				for (i in 0..moves.lastIndex) {
					if (Timer.runtime() > 1600)
						break@loop
					val move = moves[i]
					val newState = nodeState.test(move, i < moves.lastIndex) ?: continue
					// Punkte
					val points = evaluate(newState) / multiplicator + node.points
					if (points < mp.points - 60)
						continue
					if (mp.update(node.move, points))
						debug?.append(" - Best")
					debug?.appendln(" - %s - %.1f".format(move.str(), points))
					// Queue
					if (newState.turn > 59 || gewonnen(newState))
						maxDepth = depth
					if (!gewonnen(newState, newState.otherPlayer) && depth < maxDepth) {
						val newNode = node.update(newState)
						newNode.points = points
						queue.add(newNode)
					}
				}
				node = queue.poll() ?: break
			} while (depth == node.depth)
			lastdepth = depth
			bestMove = mp.obj!!
			log.debug("Neuer bester Zug bei Tiefe {}: {}", depth, bestMove.str())
			println("$depth: ${Timer.runtime()}")
		}
		debug?.appendln("### Chose ${bestMove.str()}")
		debug?.close()
		return bestMove
	}
	
	private class Node private constructor(var gamestate: GameState, var move: Move, var points: Double, var depth: Int) {
		
		/** erstellt eine neue Node mit dem gegebenen GameState und Move mit optionalen points */
		constructor(state: GameState, m: Move, bonus: Double = 0.0): this(state, m, bonus, 1)
		
		/**
		 * gibt eine neue Node zurück
		 *
		 * @param newState der neue GameState
		 * @return neue Node mit dem GameState und depth + 1
		 */
		fun update(newState: GameState) =
				Node(newState, move, points, depth + 1)
		
		override fun toString() = "Node Tiefe %d fuer %s points %.1f".format(depth, move.str(), points)
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
	
	fun findField(type: FieldType, startIndex: Int = currentState.currentPlayer.fieldIndex + 1): Int {
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
