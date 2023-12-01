package xerus.softwarechallenge.logic2018

import sc.plugin2018.*
import sc.plugin2018.util.GameRuleLogic
import sc.shared.PlayerColor
import xerus.ktutil.*
import xerus.ktutil.helpers.Timer
import xerus.softwarechallenge.util.F
import xerus.softwarechallenge.util.MP
import xerus.softwarechallenge.util.addMove
import xerus.softwarechallenge.util.str
import java.nio.file.Path
import java.util.*
import kotlin.math.pow

object Jumper1_7 : LogicBase() {
	
	override fun evaluate(state: GameState, color: PlayerColor): Double {
		val player = state.getPlayer(color)
		val distanceToGoal = 65.minus(player.fieldIndex).toDouble()
		var points = 100.0 - distanceToGoal
		
		// Salat und Karten
		points -= params[0] * player.salads * (5 - Math.log(distanceToGoal))
		points += params[0] * (player.ownsCardOfType(CardType.EAT_SALAD).to(0.8, 0.0)
				+ player.ownsCardOfType(CardType.TAKE_OR_DROP_CARROTS).to(params[1], 0.0))
		points += player.cards.size
		
		// Karotten
		points += carrotPoints(player.carrots, distanceToGoal) * 4
		val opponent = state.getPlayer(color.opponent())
		points -= carrotPoints(opponent.carrots, 65.minus(opponent.fieldIndex).toDouble())
		points -= (fieldTypeAt(player.fieldIndex) == FieldType.CARROT).toInt()
		
		// Zieleinlauf
		points += goalPoints(player)
		val turnsLeft = 60 - state.turn
		if (turnsLeft < 2 || turnsLeft < 6 && player.carrots > GameRuleLogic.calculateCarrots(distanceToGoal.toInt()) + turnsLeft * 10 + 20)
			points -= distanceToGoal * 100
		return points
	}
	
	/** Uses a function to calculate the worth of the carrots at the given position
	 * @param x carrots
	 * @param y distance to goal
	 * */
	private fun carrotPoints(x: Int, y: Double) =
			(1.1.pow(-((x - y * 5) / (30 + y)).square) * 10 + (x / (100 - y))) * params[1]
	
	/** Salat/Karten, Karotten */
	override fun defaultParams() = doubleArrayOf(15.0, 0.5)
	
	/** sucht den besten Move per Breitensuche basierend auf dem aktuellen GameState */
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
			logger.info("Nur einen validen Zug gefunden: ${bestMove.str()}")
			return bestMove
		}
		
		// Breitensuche
		mp.clear()
		depth = 1
		var maxDepth = 5
		var node = queue.poll()
		var subDir: Path? = null
		loop@ while (depth < maxDepth && Timer.runtime() < 1000 && queue.size > 0) {
			depth = node.depth
			val divider = depth.toDouble().pow(0.3)
			do {
				val nodeState = node.gamestate
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
			depthUsed = depth
			bestMove = mp.obj!!
			logger.info("Neuer bester Zug bei Tiefe $depth: $mp")
		}
		return bestMove
	}
	
	val afterHedgehog = intArrayOf(12, 16, 20)
	override fun GameState.predefinedMove(): Move? {
		val player = currentPlayer
		val pos = player.fieldIndex
		val otherPos = otherPos
		
		if (canAdvanceTo(10))
			return advanceTo(10)
		
		if (pos == 10) {
			if (player.lastNonSkipAction !is EatSalad)
				return Move(EatSalad())
			else {
				val pos2 = findField(FieldType.POSITION_2)
				if (otherPos > 10 && canAdvanceTo(pos2))
					return advanceTo(pos2)
				val pos1 = findField(FieldType.POSITION_1)
				if (otherPos < 11 && canAdvanceTo(pos1))
					return advanceTo(pos1)
				val hare = findField(FieldType.HARE, 12)
				if (otherPos != hare && turn < 6)
					return playCard(hare, CardType.TAKE_OR_DROP_CARROTS, 20)
			}
		}
		
		if (otherPos == 22 && pos < 22) {
			val pos21 = findField(FieldType.POSITION_2)
			if (canAdvanceTo(pos21)) {
				val pos2circular = findCircular(FieldType.POSITION_2, 11 + pos / 2)
				if (otherEatingSalad == 2) {
					if (pos2circular < 22 && canAdvanceTo(pos2circular))
						return advanceTo(pos2circular)
					if (pos21 < 22)
						return advanceTo(pos21)
				} else {
					val pos22 = findField(FieldType.POSITION_2, 20)
					if (pos22 < 22 && pos < pos21 && pos21 != pos22)
						return advanceTo(pos21)
					if (pos22 == 21 && canAdvanceTo(21) && pos in afterHedgehog)
						return advanceTo(pos22)
				}
				if (pos > 11)
					return Move(FallBack())
			}
		}
		
		// eat last salad
		if (otherPos == 57 && pos > 57 && otherEatingSalad == 2 && player.hasSalad)
			return Move(FallBack())
		
		when (turn) {
		// region Rot
			6 -> {
				if (canAdvanceTo(22))
					return advanceTo(22)
				if (otherPos < 11) {
					val pos1 = findField(FieldType.POSITION_1)
					if (canAdvanceTo(pos1))
						return advanceTo(pos1)
				}
				val pos2 = findField(FieldType.POSITION_2, 16)
				if (otherPos > 19) {
					if (pos < pos2)
						return advanceTo(pos2)
					else if (otherPos == 22)
						return Move(FallBack())
				}
			}
			8 -> {
				if (canAdvanceTo(22))
					return advanceTo(22)
				if (otherPos == 22) {
					val pos2 = findField(FieldType.POSITION_2)
					if (pos2 < 21)
						return advanceTo(pos2)
					else if (fieldTypeAt(pos) != FieldType.HEDGEHOG)
						return Move(FallBack())
				}
			}
		// endregion
		// region Blau
			1 -> {
				if (otherPos == 10) {
					val field2 = findField(FieldType.POSITION_2)
					return if ((field2 < 5 && findField(FieldType.HARE, field2) < 10) || field2 < findField(FieldType.HARE))
						advanceTo(field2)
					else {
						val hare = findCircular(FieldType.HARE, field2 / 2)
						playCard(hare, CardType.EAT_SALAD)
					}
				}
			}
			3 -> {
				if (otherPos == 10) {
					return if (fieldTypeAt(pos) != FieldType.POSITION_2)
						advanceTo(findField(FieldType.POSITION_2, pos))
					else {
						val hare = findCircular(FieldType.HARE, (10 + pos) / 2)
						playCard(hare, CardType.EAT_SALAD)
					}
				}
			}
		// endregion
		}
		return null
	}
	
	override fun GameState.findMoves(): List<Move> {
		val player = currentPlayer
		val fieldIndex = player.fieldIndex
		val currentField = fieldTypeAt(fieldIndex)
		if (currentField == FieldType.SALAD && player.lastNonSkipAction !is EatSalad)
			return listOf(Move(EatSalad()))
		
		val possibleMoves = ArrayList<Move>()
		if (currentField == FieldType.CARROT) {
			if (player.carrots > 20 && fieldIndex > 40)
				possibleMoves.addMove(ExchangeCarrots(-10))
			if (player.carrots < 60)
				possibleMoves.addMove(ExchangeCarrots(10))
		}
		
		val hedgehog = getPreviousFieldByType(FieldType.HEDGEHOG, fieldIndex)
		if (hedgehog != -1 && !isOccupied(hedgehog))
			possibleMoves.addMove(FallBack())
		
		val otherPos = otherPos
		moves@ for (i in 1..GameRuleLogic.calculateMoveableFields(player.carrots).coerceAtMost(64 - player.fieldIndex)) {
			val newField = fieldIndex + i
			val newType = fieldTypeAt(newField)
			if (otherPos == newField || newType == FieldType.HEDGEHOG)
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
					if (cards.contains(CardType.EAT_SALAD)) {
						if (player.hasSalad && (fieldIndex > 42 || otherPos > newField || player.salads == 1))
							possibleMoves.add(advance.addCard(CardType.EAT_SALAD))
					}
					if (cards.contains(CardType.TAKE_OR_DROP_CARROTS)) {
						if (newCarrots > 30 && newField > 42)
							possibleMoves.add(advance.addCard(CardType.TAKE_OR_DROP_CARROTS, -20))
						possibleMoves.add(advance.addCard(CardType.TAKE_OR_DROP_CARROTS, 20))
					}
					if (cards.contains(CardType.HURRY_AHEAD) && otherPos > newField && accessible(otherPos + 1)) {
						val hurry = advance.addCard(CardType.HURRY_AHEAD)
						when (fieldTypeAt(otherPos + 1)) {
							FieldType.SALAD -> possibleMoves.add(hurry)
							FieldType.HARE -> {
								if (cards.size == 1)
									continue@moves
								/* todo bug
								if (cards.contains(CardType.FALL_BACK) && fieldTypeAt(otherPos - 1).isNot(FieldType.HEDGEHOG, FieldType.HARE))
									possibleMoves.add(hurry.addCard(CardType.FALL_BACK))*/
								if (cards.contains(CardType.TAKE_OR_DROP_CARROTS)) {
									if (newCarrots > 30 && otherPos + 1 > 40)
										possibleMoves.add(hurry.addCard(CardType.TAKE_OR_DROP_CARROTS, -20))
									possibleMoves.add(hurry.addCard(CardType.TAKE_OR_DROP_CARROTS, 20))
								}
							}
							else -> possibleMoves.add(hurry)
						}
					}
					if (cards.contains(CardType.FALL_BACK) && otherPos < newField && accessible(otherPos - 1)) {
						val fall = advance.addCard(CardType.FALL_BACK)
						when (fieldTypeAt(otherPos - 1)) {
							FieldType.HARE -> {
								if (cards.size == 1)
									continue@moves
								/* todo bug
								if (cards.contains(CardType.HURRY_AHEAD) && fieldTypeAt(otherPos + 1) == FieldType.SALAD)
									possibleMoves.add(fall.addCard(CardType.HURRY_AHEAD)) */
								if (cards.contains(CardType.TAKE_OR_DROP_CARROTS)) {
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
		else skip
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
