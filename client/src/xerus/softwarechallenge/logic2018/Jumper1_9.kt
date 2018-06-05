package xerus.softwarechallenge.logic2018

import sc.plugin2018.CardType
import sc.plugin2018.GameState
import sc.plugin2018.Move
import xerus.ktutil.*
import xerus.ktutil.helpers.Timer
import xerus.softwarechallenge.util.F
import xerus.softwarechallenge.util.MP
import xerus.softwarechallenge.util.str
import java.nio.file.Path
import java.util.*
import kotlin.math.pow

object Jumper1_9 : CommonLogic() {
	
	/** Karotten, Salat, Threshold */
	override fun defaultParams() = doubleArrayOf(3.0, 30.0)
	
	/** sucht den besten Move per Breitensuche basierend auf dem aktuellen GameState */
	override fun findBestMove(): Move? {
		val mp = MP()
		
		var moves = currentState.findMoves()
		for (move in moves) {
			val newState = currentState.test(move, moveOther = false) ?: continue
			if (newState.me.gewonnen())
				return move
			// Evaluation
			val points = evaluate(newState)
			mp.update(move, points)
			// Queue
			queue.add(Node(move, newState, points))
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
		var maxDepth = 5.coerceAtMost(59.minus(currentTurn) / 2)
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
				// todo explore other possible enemy moves
				moves = nodeState.findMoves()
				forRange(0, moves.size) { i ->
					if (Timer.runtime() > 1700)
						return@forRange
					val move = moves[i]
					val newState = nodeState.test(move, i < moves.lastIndex, false) ?: return@forRange
					// Evaluation
					val points = evaluate(newState) / divider + node.points
					if (points < mp.points - 50.0 / divider)
						return@forRange
					val update = mp.update(node.move, points)
					// Debug
					acceptedMoves++
					if (isDebug) {
						subDir = node.dir?.resolve("%.1f: %s ㊝%s ✖%s ${newState.enemy.lastNonSkipAction.str()}"
								.format(points, move.str(), newState.me.strShortest(), newState.enemy.strShortest()))?.createDir()
						if (update) {
							node.dir?.resolve("Best: %.1f - %s".format(points, move.str()))?.createFile()
							println(currentLogDir?.relativize(subDir))
						}
					}
					// Queue
					if (Timer.runtime() > 1000 || newState.me.gewonnen())
						maxDepth = depth
					if (depth < maxDepth && !nodeState.enemy.gewonnen())
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
	
	private val queue = ArrayDeque<Node>(8000)
	
	override fun clear() = queue.clear()
	
	private class Node(@F val move: Move, @F val gamestate: GameState, @F val points: Double, @F val depth: Int, @F val dir: Path?): Comparable<Node> {
		
		constructor(move: Move, state: GameState, points: Double) : this(move, state, points, 1,
				currentLogDir?.resolve("%.1f - %s".format(points, move.str()))?.createDir())
		
		fun update(newState: GameState, newPoints: Double, dir: Path?) =
				Node(move, newState, newPoints, depth + 1, dir)
		
		override fun toString() = "Node{%d for %s P %.1f}".format(depth, move.str(), points)
		override fun compareTo(other: Node) = points.compareTo(other.points)
	}
	
}
