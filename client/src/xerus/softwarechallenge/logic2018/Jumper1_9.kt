package xerus.softwarechallenge.logic2018

import sc.plugin2018.CardType
import sc.plugin2018.FieldType
import sc.plugin2018.GameState
import sc.plugin2018.Move
import sc.plugin2018.util.GameRuleLogic
import sc.shared.PlayerColor
import xerus.ktutil.createDir
import xerus.ktutil.createFile
import xerus.ktutil.helpers.Timer
import xerus.ktutil.to
import xerus.ktutil.toInt
import xerus.softwarechallenge.util.F
import xerus.softwarechallenge.util.MP
import xerus.softwarechallenge.util.str
import java.nio.file.Path
import java.util.*
import kotlin.math.pow

class Jumper1_9 : Moves2("1.9.0") {
	
	override fun evaluate(state: GameState): Double {
		val player = state.currentPlayer
		val distanceToGoal = 64.minus(player.fieldIndex).toDouble()
		var points = 100.0 - distanceToGoal
		
		// Salat und Karten
		points -= saladParam * player.salads * (5 - Math.log(distanceToGoal))
		points += saladParam * (player.ownsCardOfType(CardType.EAT_SALAD).to(0.8, 0.0)
				+ player.ownsCardOfType(CardType.TAKE_OR_DROP_CARROTS).to(carrotParam, 0.0))
		points += player.cards.size
		
		// Karotten
		points += carrotPoints(player.carrots.toDouble(), distanceToGoal) * 4
		points -= carrotPoints(state.otherPlayer.carrots.toDouble(), 65.minus(state.otherPos).toDouble())
		points -= (fieldTypeAt(player.fieldIndex) == FieldType.CARROT).toInt()
		
		// Zieleinlauf
		points += goalPoints(player)
		val turnsLeft = 60 - state.turn
		if (turnsLeft < 2 || turnsLeft < 6 && player.carrots > GameRuleLogic.calculateCarrots(distanceToGoal.toInt()) + turnsLeft * 10 + 20)
			points -= distanceToGoal * 100
		return points
	}
	
	/** Karotten, Salat/Karten */
	override fun defaultParams() = doubleArrayOf(0.6, 15.0)
	
	override fun findBestMove(): Move? {
		val mp = MP()
		
		var moves = currentState.findMoves()
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
		var maxDepth = 5
		var node = queue.poll()
		var nodeState: GameState
		var subDir: Path?
		loop@ while (depth < maxDepth && Timer.runtime() < 1000 && queue.size > 0) {
			depth = node.depth
			val divider = depth.toDouble().pow(0.3)
			do {
				nodeState = node.gamestate
				moves = nodeState.findMoves()
				for (i in 0..moves.lastIndex) {
					if (Timer.runtime() > 1600)
						break@loop
					val move = moves[i]
					val newState = nodeState.test(move, i < moves.lastIndex) ?: continue
					// Punkte
					val points = evaluate(newState) / divider + node.points
					if (points < mp.points - 100 / divider)
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
	
	override fun clear() {
		queue.clear()
	}
	
	private val queue: Queue<Node> = ArrayDeque<Node>(32000)
	
	private inner class Node(@F val move: Move, @F val gamestate: GameState, @F val points: Double, @F val depth: Int, @F val dir: Path?) {
		
		constructor(move: Move, state: GameState, points: Double) : this(move, state, points, 1,
				currentLogDir?.resolve("%.1f - %s".format(points, move.str()))?.createDir())
		
		fun update(newState: GameState, newPoints: Double, dir: Path?) =
				Node(move, newState, newPoints, depth + 1, dir)
		
		override fun toString() = "Node depth %d %s points %.1f".format(depth, move.str(), points)
		
	}
	
}
