package xerus.softwarechallenge.logic2018

import sc.plugin2018.*
import sc.plugin2018.util.GameRuleLogic
import sc.shared.PlayerColor
import xerus.ktutil.createDir
import xerus.ktutil.createFile
import xerus.ktutil.helpers.Timer
import xerus.ktutil.square
import xerus.ktutil.toInt
import xerus.softwarechallenge.util.MP
import xerus.softwarechallenge.util.addMove
import xerus.softwarechallenge.util.str
import java.nio.file.Path
import java.util.*
import kotlin.math.pow

class Jumper1_8 : Moves2("1.8.1") {
	
	fun evaluate(state: GameState): Double {
		val player = state.currentPlayer
		var points = params[0] * player.fieldIndex + 30
		val distanceToGoal = 65.minus(player.fieldIndex).toDouble()
		
		// Salat und Karten
		points -= player.salads * params[1] * (-Math.log(distanceToGoal) + 5)
		points += (player.ownsCardOfType(CardType.EAT_SALAD).toInt() + player.ownsCardOfType(CardType.TAKE_OR_DROP_CARROTS).toInt()) * params[1] * 0.6
		points += player.cards.size
		
		// Karotten
		points += carrotPoints(player.carrots.toDouble(), distanceToGoal) * 3
		points -= carrotPoints(state.otherPlayer.carrots.toDouble(), 65.minus(state.otherPos).toDouble())
		points -= (state.fieldOfCurrentPlayer() == FieldType.CARROT).toInt()
		
		// Zieleinlauf
		points += player.inGoal().toInt() * 1000
		val turnsLeft = 60 - state.turn
		if (turnsLeft < 2 || turnsLeft < 6 && player.carrots > GameRuleLogic.calculateCarrots(distanceToGoal.toInt()) + turnsLeft * 10 + 20)
			points -= distanceToGoal * 100
		return points
	}
	
	/** Karotten, Weite, Salat */
	override fun defaultParams() = doubleArrayOf(2.0, 2.0, 30.0)
	
	/** sucht den besten Move per Breitensuche basierend auf dem aktuellen GameState */
	override fun breitensuche(): Move? {
		val queue = LinkedList<Node>()
		val mp = MP()
		
		var moves = findMoves(currentState)
		for (move in moves) {
			val newState = currentState.test(move) ?: continue
			if (newState.currentPlayer.gewonnen())
				return move
			// Punkte
			val points = evaluate(newState)
			mp.update(move, points)
			// Queue
			if (!newState.otherPlayer.gewonnen() || myColor == PlayerColor.BLUE)
				queue.add(Node(move, newState, points))
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
			val multiplicator = depth.toDouble().pow(0.4)
			do {
				nodeState = node.gamestate
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
						node.dir?.resolve("Best: %.1f - %s".format(points, move.str()))?.createFile()
					// Queue
					subDir = node.dir?.resolve("%.1f - %s - %s".format(points, move.str(), newState.currentPlayer.strShort()))?.createDir()
					if (newState.turn > 59 || newState.currentPlayer.gewonnen())
						maxDepth = depth
					if (depth < maxDepth && !newState.otherPlayer.gewonnen())
						queue.add(node.update(newState, points, subDir))
				}
				node = queue.poll() ?: break
			} while (depth == node.depth)
			lastdepth = depth
			bestMove = mp.obj!!
			log.info("Neuer bester Zug bei Tiefe $depth: $mp")
		}
		return bestMove
	}
	
	private inner class Node private constructor(val move: Move, val gamestate: GameState, val points: Double, val depth: Int, val dir: Path?) {
		
		constructor(move: Move, state: GameState, points: Double) : this(move, state, points, 1,
				currentLogDir?.resolve("%.1f - %s".format(points, move.str()))?.createDir())
		
		/** @return a new Node with adjusted values */
		fun update(newState: GameState, newPoints: Double, dir: Path?) =
				Node(move, newState, newPoints, depth + 1, dir)
		
		override fun toString() = "Node depth %d %s points %.1f".format(depth, move.str(), points)
		
	}
	
}
