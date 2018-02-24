package xerus.softwarechallenge.logic2018

import sc.plugin2018.*
import sc.plugin2018.util.Constants
import xerus.softwarechallenge.Starter

class Jumper1(client: Starter, params: String, debug: Int) : LogicBase(client, params, debug, KotlinVersion(1, 3, 1)) {

    override fun findMoves(state: GameState): Collection<Move> {
        val possibleMoves = state.possibleMoves
        val preferredMoves = HashSet<Move>()
        val selectedMoves = HashSet<Move>()
        val player = state.currentPlayer
        val fieldIndex = player.fieldIndex
        for (move in possibleMoves) {
            for (action in move.actions) {
                when (action) {
                    is Advance ->
                        when {
                            action.distance + fieldIndex == Constants.NUM_FIELDS - 1
                            -> return listOf(move)
                            state.board.getTypeAt(action.distance + fieldIndex) == FieldType.SALAD
                            -> preferredMoves.add(move) // Zug auf Salatfeld
                            else -> selectedMoves.add(move)
                        }

                    is Card -> {
                        when (action.type) {
                            CardType.EAT_SALAD -> {
                                // Zug auf Hasenfeld und danach Salatkarte
                                if (fieldIndex > 42 || state.otherPlayer.fieldIndex > fieldIndex || player.salads == 1)
                                    selectedMoves.add(move)
                                else
                                    selectedMoves.remove(move)
                            }
                            CardType.TAKE_OR_DROP_CARROTS -> {
                                if (action.value == 0)
                                    selectedMoves.remove(move)
                            }
                            CardType.HURRY_AHEAD -> {
                                if(state.board.getTypeAt(state.otherPlayer.fieldIndex + 1) == FieldType.SALAD)
                                    preferredMoves.add(move)
                            }
                            else -> {}
                        // Muss nicht zusaetzlich ausgewaehlt werden, wurde schon durch Advance ausgewaehlt
                        }
                    }


                    is ExchangeCarrots ->
                        if (action.value == 10 && player.carrots < (40 - fieldIndex / 2) && player.lastNonSkipAction !is ExchangeCarrots) {
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
                        } else if (fieldIndex - state.getPreviousFieldByType(FieldType.HEDGEHOG, fieldIndex) < 5) {
                            selectedMoves.add(move)
                        }

                    else -> selectedMoves.add(move) // Salat essen oder Skip
                }
            }
        }
        return when {
            preferredMoves.isNotEmpty() -> preferredMoves
            selectedMoves.isNotEmpty() -> selectedMoves
            else -> possibleMoves
        }
    }

}
