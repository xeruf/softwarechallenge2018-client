package xerus.softwarechallenge.logic2018

import sc.plugin2018.GameState
import sc.plugin2018.Move
import xerus.ktutil.createDir
import xerus.ktutil.forRange
import xerus.ktutil.helpers.Timer
import xerus.softwarechallenge.util.F
import xerus.softwarechallenge.util.MP
import xerus.softwarechallenge.util.str
import java.nio.file.Path
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow

object Jumper3 : CommonLogic() {
	
	/** Karotten, Salat */
	override fun defaultParams() = doubleArrayOf(3.0, 20.0)
	
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
			queue.add(rootNode.rootChild(move, newState, points))
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
		logger.info("Found: ${queue.joinToString()}")
		
		// Breitensuche
		var acceptedMoves = 0
		var subDir: Path? = null
		var divider = 1.0
		depth = 1
		var maxDepth = 5.coerceAtMost(60.minus(currentTurn) / 2)
		var node = queue.pop()
		var best = 0.0
		loop@ while (depth < maxDepth && queue.size > 0) {
			val nodeState = node.popState()
			nodeState.findMoves().mapNotNullTo(ArrayList()) { move ->
				val moveState = nodeState.clone()
				move.setOrderInActions()
				try {
					move.perform(moveState)
					var bestPoints = evaluate(moveState, myColor.opponent())
					val newState = moveState.quickMove(false)!!.second
					for (move2 in newState.findMoves()) {
						val moveState2 = newState.clone()
						move2.setOrderInActions()
						move2.perform(moveState2)
						bestPoints = bestPoints.coerceAtLeast(evaluate(moveState2, myColor.opponent()))
					}
					Node(move, moveState, bestPoints, node.depth, node.dir)
				} catch (exception: Throwable) {
					logger.error("Fehler bei enemy move: ${move.str()} caused $exception\n" + nodeState.str())
					null
				}
			}.apply { sortDescending() }.take(2).forEach { stateNode ->
				node.children.add(stateNode)
				val state = stateNode.popState()
				var bestLocal = Double.NEGATIVE_INFINITY
				moves = state.findMoves()
				forRange(0, moves.size) { i ->
					if (Timer.runtime() > 1700)
						return@forRange
					val move = moves[i]
					val newState = state.test(move, i < moves.lastIndex, false) ?: return@forRange
					// Evaluation
					val points = evaluate(newState) / divider + node.points
					if (points > bestLocal)
						bestLocal = points
					if (points < best - 40.0 / divider)
						return@forRange
					if (points > best)
						best = points
					// Debug
					acceptedMoves++
					if (isDebug) subDir = node.dir?.resolve("%.1f: %s ㊝%s ✖%s %s".format(points, move.str(), newState.me.strShortest(), newState.enemy.strShortest(), newState.enemy.lastNonSkipAction.str()))?.createDir()
					// Queue
					if (Timer.runtime() > 1000 || newState.me.gewonnen())
						maxDepth = depth + 1
					if (depth < maxDepth && !state.enemy.gewonnen())
						queue.add(stateNode.child(newState, points, subDir, move))
				}
				stateNode.points = bestLocal
			}
			if (Timer.runtime() > 1600)
				break
			node = queue.pop()
			if (node.depth != depth) {
				val bestNode = rootNode.calculateBest()
				if (mp.obj != bestNode.move)
					depthUsed = depth
				mp.update(bestNode.move, bestNode.points)
				logger.info("Best move for depth $depth: $mp, accepted $acceptedMoves")
				depth = node.depth
				acceptedMoves = 0
				divider = depth.toDouble().pow(0.3)
			}
		}
		return mp.obj
	}
	
	private val queue = ArrayDeque<Node>(8000)
	
	override fun clear() {
		rootNode.children.clear()
		queue.clear()
	}
	
	private val rootNode = Node(Move(), null, 0.0, 0, null)
	
	private class Node(@F val move: Move, @F var state: GameState?, @F var points: Double, @F val depth: Int, @F val dir: Path?) : Comparable<Node> {
		
		var children = ArrayList<Node>()
		
		fun rootChild(move: Move, state: GameState, points: Double) =
				child(state, points, currentLogDir?.resolve("%.1f - %s".format(points, move.str()))?.createDir(), move)
		
		fun child(newState: GameState, newPoints: Double, dir: Path?, move: Move = this.move) =
				Node(move, newState, newPoints, depth + 1, dir).also { children.add(it) }
		
		fun popState() = state!!.also { state = null }
		
		fun calculateBest(): Node {
			return if (children.firstOrNull()?.children?.isEmpty() != false) this
			else children.maxBy { child ->
				child.children.map { it.calculateBest().points }.average().also {
					child.points = it
					//if (isDebug && child.depth == 1) logger.debug("$child - ${child.children}")
				}
			}!!
		}
		
		override fun toString() = "Node{%d for %s Points %.1f}".format(depth, move.str(), points)
		override fun compareTo(other: Node) = points.compareTo(other.points)
	}
	
}
