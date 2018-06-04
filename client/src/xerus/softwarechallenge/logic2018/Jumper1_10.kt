package xerus.softwarechallenge.logic2018

import sc.plugin2018.*
import sc.shared.PlayerColor
import xerus.ktutil.*
import xerus.ktutil.helpers.Timer
import xerus.softwarechallenge.util.F
import xerus.softwarechallenge.util.MP
import xerus.softwarechallenge.util.str
import java.nio.file.Path
import java.util.*
import kotlin.math.pow

object Jumper1_10 : CommonLogic() {
	
	fun points(state: GameState, color: PlayerColor = myColor): Double {
		val player = state.getPlayer(color)
		var points = 120.0 - state.turn * 3
		val distanceToGoal = 65.0 - player.fieldIndex
		
		// Salat und Karten
		//points += saladPoints(player.salads, distanceToGoal)
		points += player.ownsCardOfType(CardType.EAT_SALAD).to(saladParam * 0.8, 0.0)
		points += player.ownsCardOfType(CardType.TAKE_OR_DROP_CARROTS).to(carrotParam * 1.3, 0.0)
		points += player.cards.size * 2
		
		// Karotten
		points += carrotPoints(player.carrots.toDouble(), distanceToGoal) * 3
		points -= carrotPoints(state.getPlayer(color.opponent()))
		
		// Zieleinlauf
		return points + goalPoints(player)
	}
	
	/** Karotten, Salat */
	override fun defaultParams() = doubleArrayOf(2.0, 20.0)
	
	/** sucht den besten Move per Breitensuche basierend auf dem aktuellen GameState */
	override fun findBestMove(): Move? {
		val mp = MP()
		
		var moves = currentState.findMoves()
		for (move in moves) {
			val newState = currentState.test(move, moveOther = false) ?: continue
			if (newState.me.gewonnen())
				return move
			// Evaluation
			val points = points(newState)
			var evaluation = points + newState.me.fieldIndex + saladPoints(newState.me) / 2
			val otherField = fieldTypeAt(currentState.otherPos)
			if (otherField == FieldType.POSITION_1 && newState.me.fieldIndex > currentState.otherPos)
				evaluation += carrotParam / 3
			if (otherField == FieldType.POSITION_2 && newState.me.fieldIndex < currentState.otherPos)
				evaluation += carrotParam
			mp.update(move, evaluation)
			// Queue
			queue.add(Node(move, newState, points, evaluation))
		}
		
		var bestMove = mp.obj
		if (currentTurn > 57)
			return bestMove
		if (queue.size < 2) {
			if (bestMove != null) {
				logger.info("Nur einen validen Zug gefunden: ${bestMove.str()}")
			} else {
				bestMove = moves.first()
				logger.warn("Keinen sinnvollen Zug gefunden, sende ${bestMove.str()}!")
			}
			return bestMove
		}
		
		// Breitensuche
		depth = 1
		var maxDepth = 5.coerceAtMost(60.minus(currentTurn) / 2)
		var node = queue.poll()
		var nodeState: GameState
		var subDir: Path? = null
		var acceptedMoves: Int
		loop@ while (depth < maxDepth && queue.size > 0) {
			acceptedMoves = 0
			depth = node.depth
			val divider = depth.toDouble().pow(0.3)
			do {
				nodeState = node.gamestate.quickMove()?.second ?: continue
				if (Timer.runtime() > 1600)
					break@loop
				if (nodeState.otherPlayer.gewonnen() && nodeState.startPlayerColor == myColor) {
					node = queue.poll() ?: break
					continue
				}
				// todo explore other possible enemy moves
				moves = nodeState.findMoves()
				forRange(0, moves.size) { i ->
					if (Timer.runtime() > 1700)
						return@forRange
					val move = moves[i]
					val newState = nodeState.test(move, i < moves.lastIndex, false) ?: return@forRange
					// Evaluation
					val points = points(newState) / divider + node.points
					val evaluation = points + newState.me.fieldIndex + saladPoints(newState.me) / 2
					if (evaluation < mp.points - 50.0 / divider)
						return@forRange
					val update = mp.update(node.move, evaluation)
					// Debug
					acceptedMoves++
					if (isDebug) {
						subDir = node.dir?.resolve("%.1f: %s ㊝%s ✖%s ${newState.enemy.lastNonSkipAction.str()}"
								.format(evaluation, move.str(), newState.me.strShortest(), newState.enemy.strShortest()))?.createDir()
						if (update) {
							node.dir?.resolve("Best: %.1f - %s".format(evaluation, move.str()))?.createFile()
							println(currentLogDir?.relativize(subDir))
						}
					}
					// Queue
					if (Timer.runtime() > 1000 || newState.me.gewonnen())
						maxDepth = depth
					if (depth < maxDepth)
						queue.add(node.update(newState, points, subDir))
				}
				if (Timer.runtime() > 1600)
					break@loop
				node = queue.poll() ?: break
			} while (depth == node.depth)
			if (bestMove != mp.obj!!) {
				depthUsed = depth
				bestMove = mp.obj!!
			}
			logger.info("Bester Zug bei Tiefe $depth: $mp, accepted $acceptedMoves")
		}
		return bestMove
	}
	
	override fun clear() {
		queue.clear()
	}
	
	private val queue: Queue<Node> = ArrayDeque<Node>(8000)
	
	private class Node(@F val move: Move, @F val gamestate: GameState, @F val points: Double, @F val depth: Int, @F val dir: Path?) {
		
		constructor(move: Move, state: GameState, points: Double, evaluation: Double) : this(move, state, points, 1,
				currentLogDir?.resolve("%.1f - %s".format(evaluation, move.str()))?.createDir())
		
		fun update(newState: GameState, newPoints: Double, dir: Path?) =
				Node(move, newState, newPoints, depth + 1, dir)
		
		override fun toString() = "Node depth %d %s points %.1f".format(depth, move.str(), points)
		
	}
	
}
