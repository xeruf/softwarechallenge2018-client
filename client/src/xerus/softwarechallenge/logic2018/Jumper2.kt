@file:Suppress("NOTHING_TO_INLINE")

package xerus.softwarechallenge.logic2018

import sc.plugin2018.*
import sc.shared.PlayerColor
import xerus.ktutil.createDir
import xerus.ktutil.createFile
import xerus.ktutil.helpers.Timer
import xerus.softwarechallenge.util.F
import xerus.softwarechallenge.util.MP
import xerus.softwarechallenge.util.str
import java.nio.file.Path
import java.util.*

object Jumper2 : CommonLogic("2.0.0") {
	
	override fun evaluate(state: GameState): Double {
		val player = state.currentPlayer
		return player.fieldIndex + carrotPoints(player.carrots.toDouble(), 64.minus(player.fieldIndex).toDouble())
	}
	
	fun points(actions: List<Action>, state: GameState) =
			if (actions[0] is EatSalad) {
				1.0
			} else {
				actions.sumByDouble {
					if (it is Card)
						-when (it.type) {
							CardType.TAKE_OR_DROP_CARROTS -> carrotParam / 2
							CardType.EAT_SALAD -> saladParam / 2
							else -> 2.0
						}
					else
						0.0
				} + when (state.fieldOfCurrentPlayer()) {
					FieldType.GOAL -> 100.0
					FieldType.CARROT -> -1.0
					FieldType.SALAD -> saladParam
					else -> 0.0
				}
			} + (currentPlayer.salads - state.currentPlayer.salads) * saladParam
	
	// 1.7: (1.1.pow(-((x - y * 5) / (30 + y)).square) * 10 + (x / (100 - y)))
	// 1.6: (1.01).pow((player.carrots.minus(distance * 4)).square / (- 30 - distance))
	// 1.5: 1.2.pow(-((x-d*4)/(30+d)).square)
	// 1.5: 1.01.pow(-(x-d*4).square/(100+d))
	// 1.4: -(x/65-fieldIndex-4)²+10+fieldIndex
	
	/** Karotten, Salat */
	override fun defaultParams() = doubleArrayOf(10.0, 10.0)
	
	/** sucht den besten Move per Breitensuche basierend auf dem aktuellen GameState */
	override fun findBestMove(): Move? {
		val mp = MP()
		
		var moves = currentState.findMoves()
		for (move in moves) {
			val newState = currentState.test(move) ?: continue
			if (newState.currentPlayer.gewonnen())
				return move
			// Evaluation
			val points = points(move.actions, newState)
			var evaluation = points + evaluate(newState)
			val otherField = fieldTypeAt(currentState.otherPos)
			if (otherField == FieldType.POSITION_1) {
				if (newState.currentPlayer.fieldIndex > currentState.otherPos)
					evaluation -= carrotParam / 3
			} else if (otherField == FieldType.POSITION_2)
				if (currentState.otherPos > newState.currentPlayer.fieldIndex)
					evaluation -= carrotParam
			mp.update(move, evaluation)
			// Queue
			if (!newState.otherPlayer.gewonnen() || myColor == PlayerColor.BLUE)
				queue.add(Node(move, newState, points, evaluation))
		}
		
		var bestMove = mp.obj ?: moves.first()
		if (queue.size < 2) {
			log.info("Nur einen validen Zug gefunden: ${bestMove.str()}")
			return bestMove
		}
		
		// Breitensuche
		mp.clear()
		depth = 1
		var maxDepth = 4
		var node = queue.poll()
		var nodeState: GameState
		var subDir: Path?
		loop@ while (depth < maxDepth && Timer.runtime() < 1000 && queue.size > 0) {
			depth = node.depth
			do {
				nodeState = node.gamestate
				moves = nodeState.findMoves()
				for (i in 0..moves.lastIndex) {
					if (Timer.runtime() > 1600)
						break@loop
					val move = moves[i]
					val newState = nodeState.test(move, i < moves.lastIndex) ?: continue
					// Evaluation
					val points = node.points + points(move.actions, newState)
					val evaluation = points + evaluate(newState)
					if (evaluation < mp.points - 50)
						continue
					if (mp.update(node.move, evaluation))
						node.dir?.resolve("Best: %.1f - %s".format(evaluation, move.str()))?.createFile()
					// Queue
					subDir = node.dir?.resolve("%.1f - %s - %s".format(evaluation, move.str(), newState.currentPlayer.strShort()))?.createDir()
					if (depth < maxDepth) {
						if (newState.currentPlayer.gewonnen() || Timer.runtime() > 1000)
							maxDepth = depth
						else if (!newState.otherPlayer.gewonnen())
							queue.add(node.update(newState, points, subDir))
					}
				}
				node = queue.poll() ?: break
			} while (depth == node.depth)
			lastdepth = depth
			bestMove = mp.obj!!
			log.info("Neuer bester Zug bei Tiefe $depth: $mp")
		}
		return bestMove
	}
	
	override fun clear() {
		queue.clear()
	}
	
	private val queue: Queue<Node> = ArrayDeque<Node>(32000)
	
	private class Node(@F val move: Move, @F val gamestate: GameState, @F val points: Double, @F val depth: Int, @F val dir: Path?) {
		
		constructor(move: Move, state: GameState, points: Double, evaluation: Double) : this(move, state, points, 1,
				currentLogDir?.resolve("%.1f - %s".format(evaluation, move.str()))?.createDir())
		
		fun update(newState: GameState, newPoints: Double, dir: Path?) =
				Node(move, newState, newPoints, depth + 1, dir)
		
		override fun toString() = "Node depth %d %s points %.1f".format(depth, move.str(), points)
		
	}
	
}
