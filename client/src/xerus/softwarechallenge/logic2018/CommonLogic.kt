@file:Suppress("NOTHING_TO_INLINE")

package xerus.softwarechallenge.logic2018

import sc.plugin2018.*
import sc.plugin2018.util.GameRuleLogic
import sc.shared.PlayerColor
import xerus.ktutil.forRange
import xerus.ktutil.square
import xerus.ktutil.to
import xerus.softwarechallenge.util.F
import xerus.softwarechallenge.util.addMove
import java.util.*
import kotlin.math.pow

abstract class CommonLogic : LogicBase() {
	
	/** Uses a function to gauge the worth of the carrots at the given position
	 * @param x carrots
	 * @param y distance to goal
	 */
	protected fun carrotPoints(x: Double, y: Double) =
			(1.1.pow(-((x - y.pow(1.6)) / (40 + y)).square) * 5 + x / (100 - y)) * carrotParam
	
	protected inline fun carrotPoints(player: Player) = carrotPoints(player.carrots.toDouble(), player.distanceToGoal)
	
	protected fun saladPoints(salads: Int, distanceToGoal: Double) = saladParam * (currentPlayer.salads - salads) * (5 - Math.log(distanceToGoal))
	
	protected inline fun saladPoints(player: Player) = saladPoints(player.salads, player.distanceToGoal)
	
	protected inline val Player.distanceToGoal get() = 65.0 - fieldIndex
	
	@F val carrotParam = params[0]
	@F val saladParam = params[1]
	
	override fun evaluate(state: GameState, color: PlayerColor): Double {
		val player = state.getPlayer(color)
		var points = player.fieldIndex + 120.0 - state.turn * 3
		val distanceToGoal = player.distanceToGoal
		
		// Salat und Karten
		points -= saladParam * player.salads * (5 - Math.log(distanceToGoal))
		points += player.ownsCardOfType(CardType.EAT_SALAD).to(saladParam * 0.8, 0.0)
		points += player.ownsCardOfType(CardType.TAKE_OR_DROP_CARROTS).to(carrotParam * 1.3, 0.0)
		points += player.cards.size * 2
		
		// Karotten
		points += carrotPoints(player.carrots.toDouble(), distanceToGoal) * 3
		points -= carrotPoints(state.getPlayer(color.opponent()))
		
		// Zieleinlauf
		return points + goalPoints(player)
	}
	
	override fun GameState.findMoves(): List<Move> {
		val player = currentPlayer
		val index = player.fieldIndex
		val currentField = fieldTypeAt(index)
		if (currentField == FieldType.SALAD && player.lastNonSkipAction !is EatSalad)
			return listOf(Move(EatSalad()))
		
		val allMoves = ArrayList<Move>()
		val moves = ArrayList<Move>()
		if (currentField == FieldType.CARROT) {
			if (shouldDropCarrots(10, player.carrots, index))
				moves.addMove(ExchangeCarrots(-10))
			moves.addMove(ExchangeCarrots(10))
		}
		
		fun newMove(move: Move, condition: Boolean) {
			if (condition)
				moves.add(move)
			else if (moves.isEmpty())
				allMoves.add(move)
		}
		
		val hedgehog = getPreviousFieldByType(FieldType.HEDGEHOG, index)
		if (hedgehog != -1 && !isOccupied(hedgehog))
			moves.addMove(FallBack())
		
		val otherPos = otherPos
		forRange(1, GameRuleLogic.calculateMoveableFields(player.carrots).coerceAtMost(64 - index) + 1) { i ->
			val newPos = index + i
			val newType = fieldTypeAt(newPos)
			if ((otherPos == newPos && newPos != 64) || newType == FieldType.HEDGEHOG)
				return@forRange
			val advance = Move(Advance(i))
			val newCarrots = player.carrots - GameRuleLogic.calculateCarrots(i)
			when (newType) {
				FieldType.GOAL -> {
					if (newCarrots <= 10 && !player.hasSalad)
						return listOf(advance)
				}
				FieldType.SALAD -> {
					if (player.hasSalad)
						moves.add(advance)
				}
				FieldType.HARE -> {
					val cards = player.cards
					if (cards.isEmpty())
						return@forRange
					if (CardType.EAT_SALAD in cards && player.hasSalad)
						newMove(advance.addCard(CardType.EAT_SALAD), newPos > 42 || newPos < otherPos || player.salads == 1)
					if (CardType.TAKE_OR_DROP_CARROTS in cards) {
						newMove(advance.addCard(CardType.TAKE_OR_DROP_CARROTS, 20), CardType.EAT_SALAD !in cards || newPos > otherPos)
						if (shouldDropCarrots(20, newCarrots, newPos))
							moves.add(advance.addCard(CardType.TAKE_OR_DROP_CARROTS, -20))
						if (shouldDropCarrots(0, newCarrots, newPos))
							moves.add(advance.addCard(CardType.TAKE_OR_DROP_CARROTS, 0))
					}
					if (CardType.HURRY_AHEAD in cards && otherPos > newPos && accessible(otherPos + 1, newCarrots)) {
						val hurry = advance.addCard(CardType.HURRY_AHEAD)
						when (fieldTypeAt(otherPos + 1)) {
							FieldType.HARE -> {
								if (cards.size > 1) {
									if (CardType.FALL_BACK in cards && accessible(otherPos - 1, newCarrots)) {
										val newField = fieldTypeAt(otherPos - 1)
										if (newField != FieldType.HARE)
											moves.add(hurry.addCard(CardType.FALL_BACK))
									}
									if (CardType.TAKE_OR_DROP_CARROTS in cards) {
										moves.add(hurry.addCard(CardType.TAKE_OR_DROP_CARROTS, 20))
										if (shouldDropCarrots(20, newCarrots, otherPos + 1))
											moves.add(hurry.addCard(CardType.TAKE_OR_DROP_CARROTS, -20))
										if (shouldDropCarrots(0, newCarrots, otherPos + 1))
											moves.add(hurry.addCard(CardType.TAKE_OR_DROP_CARROTS, 0))
									}
								}
							}
							else -> moves.add(hurry)
						}
					}
					if (CardType.FALL_BACK in cards && otherPos < newPos && accessible(otherPos - 1, newCarrots)) {
						val fall = advance.addCard(CardType.FALL_BACK)
						when (fieldTypeAt(otherPos - 1)) {
							FieldType.HARE -> {
								if (cards.size > 1) {
									if (CardType.HURRY_AHEAD in cards && accessible(otherPos + 1, newCarrots)) {
										val newField = fieldTypeAt(otherPos + 1)
										if (newField != FieldType.HARE)
											newMove(fall.addCard(CardType.HURRY_AHEAD), newField == FieldType.SALAD)
									}
									if (CardType.EAT_SALAD in cards && player.hasSalad)
										moves.add(fall.addCard(CardType.EAT_SALAD))
									if (CardType.TAKE_OR_DROP_CARROTS in cards) {
										if (CardType.EAT_SALAD !in cards)
											moves.add(fall.addCard(CardType.TAKE_OR_DROP_CARROTS, 20))
										if (shouldDropCarrots(20, newCarrots, otherPos - 1))
											moves.add(fall.addCard(CardType.TAKE_OR_DROP_CARROTS, -20))
										if (shouldDropCarrots(0, newCarrots, otherPos - 1))
											moves.add(fall.addCard(CardType.TAKE_OR_DROP_CARROTS, 0))
									}
								}
							}
							else -> moves.add(fall)
						}
					}
				}
				else -> moves.add(advance)
			}
		}
		
		return when {
			moves.isNotEmpty() -> moves
			allMoves.isNotEmpty() -> allMoves
			else -> skip
		}
	}
	
	override fun GameState.predefinedMove(): Move? {
		val player = currentPlayer
		val pos = player.fieldIndex
		val otherPos = otherPos
		if (pos == 0 && fieldTypeAt(1) == FieldType.POSITION_2 && otherPos != 1)
			return advanceTo(1)
		
		return null
	}
	
}
