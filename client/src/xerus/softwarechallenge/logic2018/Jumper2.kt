@file:Suppress("NOTHING_TO_INLINE")

package xerus.softwarechallenge.logic2018

import sc.plugin2018.*
import sc.plugin2018.util.GameRuleLogic
import sc.shared.PlayerColor
import xerus.ktutil.*
import xerus.ktutil.helpers.Timer
import xerus.softwarechallenge.util.MP
import xerus.softwarechallenge.util.addMove
import xerus.softwarechallenge.util.str
import java.nio.file.Path
import java.util.*
import kotlin.math.pow

class Jumper2 : LogicBase("2.0.0") {
	
	/** bewertet die gegebene Situation
	 * @return Einschätzung der gegebenen Situation in Punkten */
	fun evaluate(state: GameState): Double {
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
							CardType.TAKE_OR_DROP_CARROTS -> params[1] / 2
							CardType.EAT_SALAD -> params[0] / 2
							else -> 2.0
						}
					else
						0.0
				} + when (state.fieldOfCurrentPlayer()) {
					FieldType.GOAL -> 100.0
					FieldType.CARROT -> -1.0
					FieldType.SALAD -> +params[0]
					else -> 0.0
				}
			} + (currentPlayer.salads - state.currentPlayer.salads) * params[0]
	
	/** Uses a function to gauge the worth of the carrots at the given position
	 * @param x carrots
	 * @param y distance to goal
	 */
	private inline fun carrotPoints(x: Double, y: Double) =
			(1.1.pow(-((x - y.pow(1.6)) / (40 + y)).square) * 5 + x / (100 - y)) * params[1]
	
	// 1.7: (1.1.pow(-((x - y * 5) / (30 + y)).square) * 10 + (x / (100 - y)))
	// 1.6: (1.01).pow((player.carrots.minus(distance * 4)).square / (- 30 - distance))
	// 1.5: 1.2.pow(-((x-d*4)/(30+d)).square)
	// 1.5: 1.01.pow(-(x-d*4).square/(100+d))
	// 1.4: -(x/65-fieldIndex-4)²+10+fieldIndex
	
	/** Salat, Karotten */
	override fun defaultParams() = doubleArrayOf(10.0, 10.0)
	
	/** sucht den besten Move per Breitensuche basierend auf dem aktuellen GameState */
	override fun breitensuche(): Move? {
		val queue = LinkedList<Node>()
		val mp = MP()
		
		var moves = findMoves(currentState)
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
					evaluation -= params[1] / 3
			} else if (otherField == FieldType.POSITION_2)
				if (currentState.otherPos > newState.currentPlayer.fieldIndex)
					evaluation -= params[1]
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
			//val divider = depth.toDouble().pow(0.3)
			do {
				nodeState = node.gamestate
				moves = findMoves(nodeState)
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
						else //if (!newState.otherPlayer.gewonnen())
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
	
	override fun findMoves(state: GameState): List<Move> {
		val player = state.currentPlayer
		val fieldIndex = player.fieldIndex
		val currentField = fieldTypeAt(fieldIndex)
		if (currentField == FieldType.SALAD && player.lastNonSkipAction !is EatSalad)
			return listOf(Move(EatSalad()))
		
		val possibleMoves = ArrayList<Move>()
		if (currentField == FieldType.CARROT) {
			if (fieldIndex > 42 && player.carrots > 74 - fieldIndex)
				possibleMoves.addMove(ExchangeCarrots(-10))
			if (player.carrots < 40)
				possibleMoves.addMove(ExchangeCarrots(10))
		}
		
		val hedgehog = state.getPreviousFieldByType(FieldType.HEDGEHOG, fieldIndex)
		if (hedgehog != -1 && !state.isOccupied(hedgehog))
			possibleMoves.addMove(FallBack())
		
		val otherPos = state.otherPos
		moves@ for (i in 1..GameRuleLogic.calculateMoveableFields(player.carrots).coerceAtMost(64 - player.fieldIndex)) {
			val newPos = fieldIndex + i
			val newType = fieldTypeAt(newPos)
			if (otherPos == newPos || newType == FieldType.HEDGEHOG)
				continue
			val advance = Move(Advance(i))
			val newCarrots = player.carrots - GameRuleLogic.calculateCarrots(i)
			when (newType) {
				FieldType.GOAL -> {
					if (newCarrots <= 10 && !player.hasSalad)
						return listOf(advance)
					else
						break@moves
				}
				FieldType.SALAD -> {
					if (player.hasSalad)
						possibleMoves.add(advance)
				}
				FieldType.HARE -> {
					val cards = player.cards
					if (cards.isEmpty())
						continue@moves
					if (CardType.EAT_SALAD in cards && player.hasSalad && (fieldIndex > 42 || otherPos > newPos || player.salads == 1))
						possibleMoves.add(advance.addCard(CardType.EAT_SALAD))
					if (CardType.TAKE_OR_DROP_CARROTS in cards) {
						if (newCarrots > 30 && newPos > 40)
							possibleMoves.add(advance.addCard(CardType.TAKE_OR_DROP_CARROTS, -20))
						if (CardType.EAT_SALAD !in cards || newPos > otherPos)
							possibleMoves.add(advance.addCard(CardType.TAKE_OR_DROP_CARROTS, 20))
					}
					if (CardType.HURRY_AHEAD in cards && otherPos > newPos && state.accessible(otherPos + 1)) {
						val hurry = advance.addCard(CardType.HURRY_AHEAD)
						when (fieldTypeAt(otherPos + 1)) {
							FieldType.SALAD -> possibleMoves.add(hurry)
							FieldType.HARE -> {
								if (cards.size == 1)
									continue@moves
								/* todo bug
								if (CardType.FALL_BACK in cards && fieldTypeAt(otherPos - 1).isNot(FieldType.HEDGEHOG, FieldType.HARE))
									possibleMoves.add(hurry.addCard(CardType.FALL_BACK))*/
								if (CardType.TAKE_OR_DROP_CARROTS in cards) {
									if (newCarrots > 30 && otherPos + 1 > 40)
										possibleMoves.add(hurry.addCard(CardType.TAKE_OR_DROP_CARROTS, -20))
									possibleMoves.add(hurry.addCard(CardType.TAKE_OR_DROP_CARROTS, 20))
								}
							}
							else -> possibleMoves.add(hurry)
						}
					}
					if (CardType.FALL_BACK in cards && otherPos < newPos && state.accessible(otherPos - 1)) {
						val fall = advance.addCard(CardType.FALL_BACK)
						when (fieldTypeAt(otherPos - 1)) {
							FieldType.HARE -> {
								if (cards.size == 1)
									continue@moves
								/* todo bug
								if (CardType.HURRY_AHEAD in cards && fieldTypeAt(otherPos + 1) == FieldType.SALAD)
									possibleMoves.add(fall.addCard(CardType.HURRY_AHEAD)) */
								if (CardType.TAKE_OR_DROP_CARROTS in cards) {
									if (newCarrots > 30 && otherPos - 1 > 40)
										possibleMoves.add(fall.addCard(CardType.TAKE_OR_DROP_CARROTS, -20))
									possibleMoves.add(fall.addCard(CardType.TAKE_OR_DROP_CARROTS, 20))
								}
							}
							else -> possibleMoves.add(fall)
						}
					}
				}
				else -> possibleMoves.add(advance)
			}
		}
		
		return if (possibleMoves.isNotEmpty()) possibleMoves
		else listOf(Move(Skip()))
	}
	
	override fun predefinedMove(state: GameState): Move? {
		val player = state.currentPlayer
		val pos = player.fieldIndex
		if (pos == 0 && fieldTypeAt(1) == FieldType.POSITION_2)
			return state.advanceTo(1)
		
		if (fieldTypeAt(pos) == FieldType.SALAD && player.lastNonSkipAction !is EatSalad)
			return Move(EatSalad())
		
		//val otherPos = state.otherPos
		/*if (otherPos < 11 && state.otherEatingSalad < 2) {
			val pos1 = findField(FieldType.POSITION_1)
			if(player.hasCarrotsTo(pos1))
				return state.advanceTo(pos1)
		}*/
		
		//if (otherPos == 57 && pos > 57 && state.otherEatingSalad == 2 && player.hasSalad) return Move(FallBack())
		
		return null
	}
	
	private inner class Node private constructor(val move: Move, val gamestate: GameState, val points: Double, val depth: Int, val dir: Path?) {
		
		constructor(move: Move, state: GameState, points: Double, evaluation: Double) : this(move, state, points, 1,
				currentLogDir?.resolve("%.1f - %s".format(evaluation, move.str()))?.createDir())
		
		/** @return a new Node with adjusted values */
		fun update(newState: GameState, newPoints: Double, dir: Path?) =
				Node(move, newState, newPoints, depth + 1, dir)
		
		override fun toString() = "Node depth %d %s points %.1f".format(depth, move.str(), points)
		
	}
	
}
