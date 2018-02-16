package xerus.softwarechallenge.logic2018

import java.util.ArrayList

import sc.plugin2018.Action
import sc.plugin2018.Advance
import sc.plugin2018.Card
import sc.plugin2018.CardType
import sc.plugin2018.ExchangeCarrots
import sc.plugin2018.FallBack
import sc.plugin2018.FieldType
import sc.plugin2018.GameState
import sc.plugin2018.Move
import sc.plugin2018.Player
import sc.plugin2018.util.Constants
import xerus.softwarechallenge.Starter
import xerus.softwarechallenge.util.LogicHandler

/** enthält Grundlagen für eine Logik für die Softwarechallenge 2018 - Hase und Igel  */
abstract class LogicBase(client: Starter, params: String, debug: Int, version: String): LogicHandler(client, params, debug, "Jumper "+version) {

    override fun evaluate(state: GameState): Double {
        var points = state.getPointsForPlayer(currentGameState.currentPlayerColor).toDouble()
        points += if (gewonnen(state))
            Double.MAX_VALUE - state.currentPlayer.carrots * 5
        else
            state.currentPlayer.carrots.div(params[0])
        return points
    }

    override fun defaultParams() = doubleArrayOf(5.0)

    override fun tostring(player: Player): String =
        String.format("Player %s Feld: %s Gemuese: %s/%s", player.playerColor, player.fieldIndex, player.salads, player.carrots)

    override fun gewonnen(state: GameState) =
            state.currentPlayer.inGoal()

    override fun simplemove(): Move {
        val state = currentGameState
        val possibleMove = state.possibleMoves // EnthÃ¤lt mindestens ein Element
        val saladMoves = ArrayList<Move>()
        val winningMoves = ArrayList<Move>()
        val selectedMoves = ArrayList<Move>()
        val currentPlayer = state.currentPlayer
        val index = currentPlayer.fieldIndex
        for (move in possibleMove) {
            for (action in move.actions) {
                if (action is Advance) {
                    if (action.distance+index==Constants.NUM_FIELDS-1) {
                        // Zug ins Ziel
                        winningMoves.add(move)
                    } else if (state.board.getTypeAt(action.distance+index)==FieldType.SALAD) {
                        // Zug auf Salatfeld
                        saladMoves.add(move)
                    } else {
                        // Ziehe VorwÃ¤rts, wenn mÃ¶glich
                        selectedMoves.add(move)
                    }
                } else if (action is Card) {
                    if (action.type==CardType.EAT_SALAD) {
                        // Zug auf Hasenfeld und danch Salatkarte
                        saladMoves.add(move)
                    } // Muss nicht zusÃ¤tzlich ausgewÃ¤hlt werden, wurde schon durch Advance ausgewÃ¤hlt
                } else if (action is ExchangeCarrots) {
                    if (action.value==10 && currentPlayer.carrots<30 && index<40 && currentPlayer.lastNonSkipAction !is ExchangeCarrots) {
                        // Nehme nur Karotten auf, wenn weniger als 30 und nur am Anfang und nicht zwei mal
                        // hintereinander
                        selectedMoves.add(move)
                    } else if (action.value==-10 && currentPlayer.carrots>30 && index>=40) {
                        // abgeben von Karotten ist nur am Ende sinnvoll
                        selectedMoves.add(move)
                    }
                } else if (action is FallBack) {
                    if (index>56 /* letztes Salatfeld */ && currentPlayer.salads>0) {
                        // Falle nur am Ende (index > 56) zurÃ¼ck, auÃer du musst noch einen Salat loswerden
                        selectedMoves.add(move)
                    } else if (index<=56 && index-state.getPreviousFieldByType(FieldType.HEDGEHOG, index)<5) {
                        // Falle zurÃ¼ck, falls sich RÃ¼ckzug lohnt (nicht zu viele Karotten aufnehmen)
                        selectedMoves.add(move)
                    }
                } else {
                    // FÃ¼ge Salatessen oder Skip hinzu
                    selectedMoves.add(move)
                }
            }
        }
        val move = if (!winningMoves.isEmpty()) {
            log.info("Sende Gewinnzug")
            winningMoves[LogicHandler.rand.nextInt(winningMoves.size)]
        } else if (!saladMoves.isEmpty()) {
            // es gibt die Möglichkeit einen Salat zu essen
            log.info("Sende Zug zum Salatessen")
            saladMoves[LogicHandler.rand.nextInt(saladMoves.size)]
        } else if (!selectedMoves.isEmpty()) {
            selectedMoves[LogicHandler.rand.nextInt(selectedMoves.size)]
        } else {
            possibleMove[LogicHandler.rand.nextInt(possibleMove.size)]
        }
        move.orderActions()
        return move
    }

}
