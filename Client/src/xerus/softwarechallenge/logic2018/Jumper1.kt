package xerus.softwarechallenge.logic2018

import sc.plugin2018.Advance
import sc.plugin2018.Card
import sc.plugin2018.CardType
import sc.plugin2018.EatSalad
import sc.plugin2018.ExchangeCarrots
import sc.plugin2018.FallBack
import sc.plugin2018.FieldType
import sc.plugin2018.GameState
import sc.plugin2018.Move
import sc.plugin2018.util.Constants
import sc.plugin2018.util.GameRuleLogic
import xerus.softwarechallenge.Starter
import java.util.ArrayList

class Jumper1(client: Starter, params: String, debug: Int): LogicBase(client, params, debug, KotlinVersion(1, 1, 0)) {

    override fun findMoves(state: GameState): Collection<Move> {
        val possibleMoves = state.possibleMoves
        val winningMoves = HashSet<Move>()
        val selectedMoves = HashSet<Move>()
        val player = state.currentPlayer
        val fieldIndex = player.fieldIndex
        if (GameRuleLogic.isValidToEat(state))
            return listOf(move(EatSalad()))
        for (move in possibleMoves) {
            for (action in move.actions) {
                when (action) {
                    is Advance ->
                        when {
                            action.distance+fieldIndex == Constants.NUM_FIELDS-1
                            -> winningMoves.add(move) // Zug ins Ziel
                            state.board.getTypeAt(action.distance+fieldIndex) == FieldType.SALAD
                            -> winningMoves.add(move) // Zug auf Salatfeld
                            else -> selectedMoves.add(move)
                        }

                    is Card ->
                        if (action.type == CardType.EAT_SALAD) {
                            // Zug auf Hasenfeld und danach Salatkarte
                            selectedMoves.add(move)
                        } // Muss nicht zusaetzlich ausgewaehlt werden, wurde schon durch Advance ausgewaehlt

                    is ExchangeCarrots ->
                        if (action.value == 10 && player.carrots < (40-fieldIndex/2) && player.lastNonSkipAction !is ExchangeCarrots) {
                            // Nehme nur Karotten auf wenn weniger als 30 und nur am Anfang und nicht zweimal hintereinander
                            selectedMoves.add(move)
                        } else if (action.value == -10 && player.carrots > 18 && fieldIndex >= 40 && player.salads == 0) {
                            // abgeben von Karotten ist nur am Ende sinnvoll
                            selectedMoves.add(move)
                        }

                    is FallBack ->
                        if (fieldIndex > 56) {
                            if (player.salads > 0)
                                selectedMoves.add(move)
                        } else if (fieldIndex-state.getPreviousFieldByType(FieldType.HEDGEHOG, fieldIndex) < 5) {
                            selectedMoves.add(move)
                        }

                    else -> selectedMoves.add(move) // Salat essen oder Skip
                }
            }
        }
        return when {
            winningMoves.isNotEmpty() -> winningMoves
            selectedMoves.isNotEmpty() -> selectedMoves
            else -> possibleMoves
        }
    }

}
