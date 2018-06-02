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

object Jumper1_8 : CommonLogic() {
	
	/** Karotten, Salat, Threshold */
	override fun defaultParams() = doubleArrayOf(3.0, 30.0, 50.0)
	
	/** sucht den besten Move per Breitensuche basierend auf dem aktuellen GameState */
	override fun findBestMove(): Move? {
		val mp = MP()
		
		var moves = currentState.findMoves()
		for (move in moves) {
			val newState = currentState.test(move) ?: continue
			if (newState.currentPlayer.gewonnen())
				return move
			// Points
			val points = evaluate(newState)
			mp.update(move, points)
			// Queue
			queue.add(Node(move, newState, points))
		}
		
		var bestMove = mp.obj
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
		mp.clear()
		depth = 1
		var maxDepth = 5
		var node = queue.poll()
		var nodeState: GameState
		var subDir: Path? = null
		var acceptedMoves: Int
		loop@ while (depth < maxDepth && Timer.runtime() < 1000 && queue.size > 0) {
			acceptedMoves = 0
			depth = node.depth
			if (node.gamestate.turn > 57)
				maxDepth = depth
			val divider = depth.toDouble().pow(0.3)
			do {
				nodeState = node.gamestate
				moves = nodeState.findMoves()
				forRange(0, moves.size) { i ->
					val move = moves[i]
					val newState = nodeState.test(move, i < moves.lastIndex) ?: return@forRange
					// Points
					val points = evaluate(newState) / divider + node.points
					if (points < mp.points - params[2] / divider)
						return@forRange
					val update = mp.update(node.move, points)
					// Debug
					acceptedMoves++
					if (isDebug) {
						if (update)
							node.dir?.resolve("Best: %.1f - %s".format(points, move.str()))?.createFile()
						subDir = node.dir?.resolve("%.1f - %s - %s".format(points, move.str(), newState.currentPlayer.strShort()))?.createDir()
					}
					// Queue
					if (newState.currentPlayer.gewonnen())
						maxDepth = depth
					if (depth < maxDepth && !(newState.otherPlayer.gewonnen() && newState.startPlayerColor == myColor))
						queue.add(node.update(newState, points, subDir))
				}
				if (Timer.runtime() > 1700)
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
	
	private val queue: Queue<Node> = ArrayDeque<Node>(16000)
	
	
	private class Node(@F val move: Move, @F val gamestate: GameState, @F val points: Double, @F val depth: Int, @F val dir: Path?) {
		
		constructor(move: Move, state: GameState, points: Double) : this(move, state, points, 1,
				currentLogDir?.resolve("%.1f - %s".format(points, move.str()))?.createDir())
		
		fun update(newState: GameState, newPoints: Double, dir: Path?) =
				Node(move, newState, newPoints, depth + 1, dir)
		
		override fun toString() = "Node depth %d %s points %.1f".format(depth, move.str(), points)
		
	}
	
}