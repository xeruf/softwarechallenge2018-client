package xerus.softwarechallenge.logic2018

import sc.plugin2018.GameState
import sc.plugin2018.Move
import xerus.ktutil.createDir
import xerus.ktutil.createFile
import xerus.ktutil.forRange
import xerus.ktutil.helpers.Timer
import xerus.softwarechallenge.util.F
import xerus.softwarechallenge.util.MP
import xerus.softwarechallenge.util.str
import java.nio.file.Path
import java.util.*
import kotlin.math.pow

object Jumper1_9 : CommonLogic() {
	
	/** Karotten, Salat, Threshold */
	override fun defaultParams() = doubleArrayOf(4.05, 32.79, 55.21)
	
	/** sucht den besten Move per Breitensuche basierend auf dem aktuellen GameState */
	override fun findBestMove(): Move? {
		val mp = MP()
		
		var moves = currentState.findMoves()
		for (move in moves) {
			val newState = currentState.test(move, moveOther = false) ?: continue
			if (newState.getPlayer(myColor).gewonnen())
				return move
			// Points
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
		var maxDepth = 5.coerceAtMost(61.minus(currentTurn) / 2)
		var node = queue.poll()
		var subDir: Path? = null
		var acceptedMoves: Int
		loop@ while (depth < maxDepth && Timer.runtime() < 1000 && queue.size > 0) {
			acceptedMoves = 0
			depth = node.depth
			val divider = depth.toDouble().pow(0.3)
			do {
				val nodeState = node.gamestate.quickMove().second
				if(nodeState.otherPlayer.gewonnen() && nodeState.startPlayerColor == myColor) {
					node = queue.poll() ?: break
					continue
				}
				// todo explore other possible enemy moves
				moves = nodeState.findMoves()
				forRange(0, moves.size) { i ->
					val move = moves[i]
					val newState = nodeState.test(move, i < moves.lastIndex, false) ?: return@forRange
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
						subDir = node.dir?.resolve("%.1f - %s to %s".format(points, move.str(), newState.getPlayer(myColor).strShort()))?.createDir()
					}
					// Queue
					if (newState.getPlayer(myColor).gewonnen())
						maxDepth = depth
					if (depth < maxDepth)
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
	
	private val queue: Queue<Node> = ArrayDeque<Node>(8000)
	
	private class Node(@F val move: Move, @F val gamestate: GameState, @F val points: Double, @F val depth: Int, @F val dir: Path?) {
		
		constructor(move: Move, state: GameState, points: Double) : this(move, state, points, 1,
				currentLogDir?.resolve("%.1f - %s".format(points, move.str()))?.createDir())
		
		fun update(newState: GameState, newPoints: Double, dir: Path?) =
				Node(move, newState, newPoints, depth + 1, dir)
		
		override fun toString() = "Node depth %d %s points %.1f".format(depth, move.str(), points)
		
	}
	
}