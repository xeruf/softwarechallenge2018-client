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
import xerus.ktutil.toInt
import xerus.softwarechallenge.util.F
import xerus.softwarechallenge.util.MP
import xerus.softwarechallenge.util.str
import java.nio.file.Path
import java.util.*
import kotlin.math.pow

class Jumper1_8 : Moves2("1.8.1") {
	
	override fun evaluate(state: GameState): Double {
		val player = state.currentPlayer
		var points = posParam * player.fieldIndex + 30
		val distanceToGoal = 65.minus(player.fieldIndex).toDouble()
		
		// Salat und Karten
		points -= saladParam * player.salads * (-Math.log(distanceToGoal) + 5)
		points += saladParam * 0.5 * (player.ownsCardOfType(CardType.EAT_SALAD).toInt() + player.ownsCardOfType(CardType.TAKE_OR_DROP_CARROTS).toInt())
		points += player.cards.size * 2
		
		// Karotten
		points += carrotPoints(player.carrots.toDouble(), distanceToGoal) * 3
		points -= carrotPoints(state.otherPlayer.carrots.toDouble(), 65.minus(state.otherPos).toDouble())
		points -= (fieldTypeAt(player.fieldIndex) == FieldType.CARROT).toInt()
		
		// Zieleinlauf
		points += goalPoints(player)
		val turnsLeft = 60 - state.turn
		if (turnsLeft < 2 || turnsLeft < 6 && player.carrots > GameRuleLogic.calculateCarrots(distanceToGoal.toInt()) + turnsLeft * 10 + 20)
			points -= distanceToGoal * 100
		return points
	}
	
	/** Karotten, Salat, Weite */
	override fun defaultParams() = doubleArrayOf(2.0, 30.0, 2.0)
	
	/** sucht den besten Move per Breitensuche basierend auf dem aktuellen GameState */
	override fun breitensuche(): Move? {
		val queue: Queue<Node> = ArrayDeque<Node>(20000)
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
		var subDir: Path? = null
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
					val update = mp.update(node.move, points)
					if (update && isDebug)
						node.dir?.resolve("Best: %.1f - %s".format(points, move.str()))?.createFile()
					// Queue
					if (isDebug)
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
	
	/*override fun GameState.test(move: Move, clone: Boolean): GameState? {
		val newState = if (clone) clone() else this
		try {
			move.setOrderInActions()
			move.perform(newState)
			val turnIndex = newState.turn
			val bestState = Rater<GameState>()
			if (turnIndex < 60) {
				for (otherMove in findMoves(newState)) {
					val moveState = newState.clone()
					otherMove.setOrderInActions()
					try {
						otherMove.perform(moveState)
					} catch (exception: Throwable) {
						log.warn("Fehler bei otherMove: ${otherMove.str()} - ${this.otherPlayer.str()}: $exception\n${newState.str()}")
					}
					moveState.turn -= 1
					moveState.switchCurrentPlayer()
					if (bestState.update(moveState, evaluate(moveState))) {
						moveState.turn += 1
						moveState.switchCurrentPlayer()
					}
				}
			}
			
			validMoves++
			return bestState.obj ?: newState.apply {
				turn += 1
				switchCurrentPlayer()
			}
		} catch (e: InvalidMoveException) {
			invalidMoves++
			if (debugLevel > 0)
				log.warn("FEHLERHAFTER ZUG: {} FEHLER: {} " + this.str(), move.str(), e.message)
		}
		return null
	}*/
	
	private inner class Node(@F val move: Move, @F val gamestate: GameState, @F val points: Double, @F val depth: Int, @F val dir: Path?) {
		
		constructor(move: Move, state: GameState, points: Double) : this(move, state, points, 1,
				currentLogDir?.resolve("%.1f - %s".format(points, move.str()))?.createDir())
		
		fun update(newState: GameState, newPoints: Double, dir: Path?) =
				Node(move, newState, newPoints, depth + 1, dir)
		
		override fun toString() = "Node depth %d %s points %.1f".format(depth, move.str(), points)
		
	}
	
}
