package xerus.softwarechallenge.logic2018

import sc.plugin2018.*
import sc.plugin2018.util.GameRuleLogic
import xerus.softwarechallenge.util.addMove

class Jumper1 : LogicBase(KotlinVersion(1, 7, 0)) {
	
	override fun findMoves(state: GameState): List<Move> {
		val player = state.currentPlayer
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
		
		val hedgehog = state.getPreviousFieldByType(FieldType.HEDGEHOG, fieldIndex)
		if (hedgehog != -1 && !state.isOccupied(hedgehog))
			possibleMoves.addMove(FallBack())
		
		val otherPos = state.otherPos()
		moves@ for (i in 1..GameRuleLogic.calculateMoveableFields(player.carrots).coerceAtMost(64 - player.fieldIndex)) {
			val newField = fieldIndex + i
			val newType = fieldTypeAt(newField)
			if (otherPos == newField || newType == FieldType.HEDGEHOG)
				continue
			val advance = Move(Advance(i))
			val newCarrots = player.carrots - GameRuleLogic.calculateCarrots(i)
			when (newType) {
				FieldType.GOAL -> {
					if (newCarrots <= 10 && !player.hasSalad())
						return listOf(advance)
					else
						break@moves
				}
				FieldType.SALAD -> {
					if (player.hasSalad())
						possibleMoves.add(advance)
				}
				FieldType.HARE -> {
					val cards = player.cards
					if (cards.isEmpty())
						continue@moves
					if (cards.contains(CardType.EAT_SALAD)) {
						if (player.hasSalad() && (fieldIndex > 42 || otherPos > newField || player.salads == 1))
							possibleMoves.add(advance.addCard(CardType.EAT_SALAD))
					}
					if (cards.contains(CardType.TAKE_OR_DROP_CARROTS)) {
						if (newCarrots > 30 && newField > 40)
							possibleMoves.add(advance.addCard(CardType.TAKE_OR_DROP_CARROTS, -20))
						possibleMoves.add(advance.addCard(CardType.TAKE_OR_DROP_CARROTS, 20))
					}
					if (cards.contains(CardType.HURRY_AHEAD) && otherPos > newField && state.accessible(otherPos + 1)) {
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
					if (cards.contains(CardType.FALL_BACK) && otherPos < newField && state.accessible(otherPos - 1)) {
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
							else ->
								possibleMoves.add(fall)
						}
					}
				}
				else -> possibleMoves.add(advance)
			}
		}
		
		return if (possibleMoves.isNotEmpty()) possibleMoves
		else listOf(Move(Skip()))
	}
	
}
