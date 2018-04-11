package xerus.softwarechallenge.logic2018

import sc.plugin2018.*
import sc.plugin2018.util.GameRuleLogic
import xerus.softwarechallenge.Starter
import xerus.softwarechallenge.util.addMove

class Jumper1(client: Starter, params: String, debug: Int) : LogicBase(client, params, debug, KotlinVersion(1, 6, 1)) {

    override fun findMoves(state: GameState): List<Move> {
        val player = state.currentPlayer
        val fieldIndex = player.fieldIndex
        val currentField = fieldTypeAt(fieldIndex)
        if (currentField == FieldType.SALAD && player.lastNonSkipAction !is EatSalad)
            return listOf(Move(EatSalad()))

        val preferredMoves = ArrayList<Move>()
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
            val advance = Move(Advance(i))
            if (otherPos == newField || newType == FieldType.HEDGEHOG)
                continue
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
                        preferredMoves.add(advance)
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
                            FieldType.SALAD -> preferredMoves.add(hurry)
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
                            FieldType.SALAD -> {
                                if (newField - otherPos < 10)
                                    preferredMoves.add(fall)
                                else
                                    possibleMoves.add(fall)
                            }
                            FieldType.HARE -> {
                                if (cards.size == 1)
                                    continue@moves
                                /*todo bug
                                if (cards.contains(CardType.HURRY_AHEAD) && fieldTypeAt(otherPos + 1) == FieldType.SALAD)
                                    possibleMoves.add(fall.addCard(CardType.HURRY_AHEAD))*/
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

        return when {
            preferredMoves.isNotEmpty() -> preferredMoves
            possibleMoves.isNotEmpty() -> possibleMoves
            else -> {
                log.warn("No moves found for ${state.str()}")
                state.possibleMoves
            }
        }
    }
/*
    fun playCards(state: GameState): Collection<Move> {
        val player = state.currentPlayer
        val cards = player.cards
        if (cards.isEmpty())
            return emptyList()

        val moves = ArrayList<Move>()
        val fieldIndex = player.fieldIndex
        val otherPos = state.otherPos()

        val card = { type: CardType, value: Int -> Move(advance, Card(type, value, 1)) }
        if (cards.contains(CardType.EAT_SALAD)) {
            if (player.hasSalad() && (fieldIndex > 42 || otherPos > newField || player.salads == 1))
                moves.add(card(CardType.EAT_SALAD, 0))
        }
        if (cards.contains(CardType.TAKE_OR_DROP_CARROTS)) {
            if (player.carrots > 30 && fieldIndex > 40)
                moves.add(card(CardType.TAKE_OR_DROP_CARROTS, -20))
            moves.add(card(CardType.TAKE_OR_DROP_CARROTS, 20))
        }
        if (cards.contains(CardType.HURRY_AHEAD) && otherPos > newField && state.accessible(otherPos + 1)) {
            moves.add(card(CardType.HURRY_AHEAD, 0))
        }
        if (cards.contains(CardType.FALL_BACK) && otherPos < newField && otherPos in 2..63) {
            if(!state.accessible(otherPos - 1))
                return emptyList()
            when (state.getTypeAt(otherPos - 1)) {
                FieldType.HEDGEHOG -> {}
            }
        }
    }*/

}
