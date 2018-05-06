package xerus.softwarechallenge.logic2018

import sc.plugin2018.*
import sc.plugin2018.util.GameRuleLogic
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
						if (CardType.EAT_SALAD !in cards || newPos > otherPos)
							possibleMoves.add(advance.addCard(CardType.TAKE_OR_DROP_CARROTS, 20))
						if (newCarrots > 84 - fieldIndex && newPos > 42)
							possibleMoves.add(advance.addCard(CardType.TAKE_OR_DROP_CARROTS, -20))
						if (newCarrots > 74 - fieldIndex)
							possibleMoves.add(advance.addCard(CardType.TAKE_OR_DROP_CARROTS, 0))
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
									if (newCarrots > 84 - fieldIndex && otherPos > 41)
										possibleMoves.add(hurry.addCard(CardType.TAKE_OR_DROP_CARROTS, -20))
									if (CardType.EAT_SALAD !in cards || newPos > otherPos)
										possibleMoves.add(hurry.addCard(CardType.TAKE_OR_DROP_CARROTS, 20))
									if (newCarrots > 74 - fieldIndex)
										possibleMoves.add(advance.addCard(CardType.TAKE_OR_DROP_CARROTS, 0))
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
									if (newCarrots > 84 - fieldIndex && otherPos > 43)
										possibleMoves.add(fall.addCard(CardType.TAKE_OR_DROP_CARROTS, -20))
									if (CardType.EAT_SALAD !in cards || newPos > otherPos)
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
		val otherPos = state.otherPos
		if (pos == 0 && fieldTypeAt(1) == FieldType.POSITION_2 && otherPos != 1)
			return state.advanceTo(1)
		
		if (fieldTypeAt(pos) == FieldType.SALAD && player.lastNonSkipAction !is EatSalad)
			return Move(EatSalad())
		
		/*if (otherPos < 11 && state.otherEatingSalad < 2) {
			val pos1 = findField(FieldType.POSITION_1)
			if(player.hasCarrotsTo(pos1))
				return state.advanceTo(pos1)
		}*/
		
		//if (otherPos == 57 && pos > 57 && state.otherEatingSalad == 2 && player.hasSalad) return Move(FallBack())
		
		return null
	}
	
}
