package xerus.softwarechallenge.logic2018

import sc.plugin2018.*
import sc.plugin2018.util.GameRuleLogic
import xerus.ktutil.forRange
import xerus.ktutil.square
import xerus.softwarechallenge.util.F
import xerus.softwarechallenge.util.addMove
import java.util.*
import kotlin.math.pow

abstract class Moves2(version: String) : LogicBase(version) {
	
	/** Uses a function to gauge the worth of the carrots at the given position
	 * @param x carrots
	 * @param y distance to goal
	 */
	protected fun carrotPoints(x: Double, y: Double) =
			(1.1.pow(-((x - y.pow(1.6)) / (40 + y)).square) * 5 + x / (100 - y)) * carrotParam
	
	@F val carrotParam = params[0]
	@F val saladParam = params[1]
	@F val posParam = if (params.size > 2) params[2] else -100.0
	
	override fun GameState.findMoves(): List<Move> {
		val player = currentPlayer
		val fieldIndex = player.fieldIndex
		val currentField = fieldTypeAt(fieldIndex)
		if (currentField == FieldType.SALAD && player.lastNonSkipAction !is EatSalad)
			return listOf(Move(EatSalad()))
		
		val moves = ArrayList<Move>()
		if (currentField == FieldType.CARROT) {
			if (fieldIndex > 42 && player.carrots > 74 - fieldIndex)
				moves.addMove(ExchangeCarrots(-10))
			moves.addMove(ExchangeCarrots(10))
		}
		
		val hedgehog = getPreviousFieldByType(FieldType.HEDGEHOG, fieldIndex)
		if (hedgehog != -1 && !isOccupied(hedgehog))
			moves.addMove(FallBack())
		
		val otherPos = otherPos
		forRange(1, GameRuleLogic.calculateMoveableFields(player.carrots).coerceAtMost(64 - player.fieldIndex) + 1) { i ->
			val newPos = fieldIndex + i
			val newType = fieldTypeAt(newPos)
			if (otherPos == newPos || newType == FieldType.HEDGEHOG)
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
					if (CardType.EAT_SALAD in cards && player.hasSalad && (newPos > 42 || otherPos > newPos || player.salads == 1))
						moves.add(advance.addCard(CardType.EAT_SALAD))
					if (CardType.TAKE_OR_DROP_CARROTS in cards) {
						if (CardType.EAT_SALAD !in cards || newPos > otherPos)
							moves.add(advance.addCard(CardType.TAKE_OR_DROP_CARROTS, 20))
						if (newCarrots > 84 - newPos && newPos > 42)
							moves.add(advance.addCard(CardType.TAKE_OR_DROP_CARROTS, -20))
						if (newCarrots > 74 - newPos)
							moves.add(advance.addCard(CardType.TAKE_OR_DROP_CARROTS, 0))
					}
					if (CardType.HURRY_AHEAD in cards && otherPos > newPos && accessible(otherPos + 1)) {
						val hurry = advance.addCard(CardType.HURRY_AHEAD)
						when (fieldTypeAt(otherPos + 1)) {
							FieldType.HARE -> {
								if (cards.size > 1) {
									/* todo bug
									if (CardType.FALL_BACK in cards && fieldTypeAt(otherPos - 1).isNot(FieldType.HEDGEHOG, FieldType.HARE))
									possibleMoves.add(hurry.addCard(CardType.FALL_BACK))*/
									if (otherPos > 41 && CardType.EAT_SALAD in cards)
										moves.add(hurry.addCard(CardType.EAT_SALAD))
									if (CardType.TAKE_OR_DROP_CARROTS in cards) {
										if (newCarrots > 84 - otherPos && otherPos > 41)
											moves.add(hurry.addCard(CardType.TAKE_OR_DROP_CARROTS, -20))
										if (newCarrots > 74 - otherPos)
											moves.add(hurry.addCard(CardType.TAKE_OR_DROP_CARROTS, 0))
										moves.add(hurry.addCard(CardType.TAKE_OR_DROP_CARROTS, 20))
									}
								}
							}
							else -> moves.add(hurry)
						}
					}
					if (CardType.FALL_BACK in cards && otherPos < newPos && accessible(otherPos - 1)) {
						val fall = advance.addCard(CardType.FALL_BACK)
						when (fieldTypeAt(otherPos - 1)) {
							FieldType.HARE -> {
								if (cards.size > 1) {
									/* todo bug
									if (CardType.HURRY_AHEAD in cards && fieldTypeAt(otherPos + 1) == FieldType.SALAD)
									possibleMoves.add(fall.addCard(CardType.HURRY_AHEAD)) */
									if (CardType.EAT_SALAD in cards)
										moves.add(fall.addCard(CardType.EAT_SALAD))
									if (CardType.TAKE_OR_DROP_CARROTS in cards) {
										if (newCarrots > 84 - otherPos && otherPos > 43)
											moves.add(fall.addCard(CardType.TAKE_OR_DROP_CARROTS, -20))
										if (newCarrots > 74 - otherPos)
											moves.add(fall.addCard(CardType.TAKE_OR_DROP_CARROTS, 0))
										if (CardType.EAT_SALAD !in cards)
											moves.add(fall.addCard(CardType.TAKE_OR_DROP_CARROTS, 20))
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
		
		/*if (debugLevel > 1) {
			val double = moves.find { moves.indexOf(it) != moves.lastIndexOf(it) }
			if (double != null)
				log.error("Double Move: $double")
		}*/
		
		return if (moves.isNotEmpty()) moves
		else listOf(Move(Skip()))
	}
	
	override fun GameState.predefinedMove(): Move? {
		val player = currentPlayer
		val pos = player.fieldIndex
		val otherPos = otherPos
		if (pos == 0 && fieldTypeAt(1) == FieldType.POSITION_2 && otherPos != 1)
			return advanceTo(1)
		
		if (fieldTypeAt(pos) == FieldType.SALAD && player.lastNonSkipAction !is EatSalad)
			return Move(EatSalad())
		
		/*if (otherPos < 11 && otherEatingSalad < 2) {
			val pos1 = findField(FieldType.POSITION_1)
			if(player.hasCarrotsTo(pos1))
				return advanceTo(pos1)
		}*/
		
		//if (otherPos == 57 && pos > 57 && otherEatingSalad == 2 && player.hasSalad) return Move(FallBack())
		
		return null
	}
	
}
