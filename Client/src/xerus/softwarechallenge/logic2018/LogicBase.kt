package xerus.softwarechallenge.logic2018

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
import java.util.ArrayList

/** enthält Grundlagen für eine Logik für die Softwarechallenge 2018 - Hase und Igel  */
abstract class LogicBase(client: Starter, params: String, debug: Int, version: String): LogicHandler(client, params, debug, "Jumper " + version) {

    override fun evaluate(state: GameState): Double {
        var points = params[0] * state.getPointsForPlayer(myColor).toDouble()
        val player = state.currentPlayer
        points -= player.salads * 5
        val distanceToGoal = 65 - player.fieldIndex
        points += 10 + distanceToGoal/2 - (player.carrots/distanceToGoal - 2-distanceToGoal/5)
        return points
    }

    // feld 0  -> mehr Karotten sind besser
    // feld 64(Ziel) -> maximal 10 Karotten
    // -(x/65-fieldIndex-4)²+10+fieldIndex
    // benötigte Karotten für x Felder: 0,5x2 + 0,5x

    override fun defaultParams() = doubleArrayOf(1.4, 5.0)

    override fun toString(player: Player): String =
            "Player %s Field: %s Greenstuff: %s/%s".format(player.playerColor, player.fieldIndex, player.salads, player.carrots)

    override fun gewonnen(state: GameState) =
            state.currentPlayer.inGoal()

    override fun simpleMove(): Move {
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
                    when {
                        action.distance+index==Constants.NUM_FIELDS-1 -> // Zug ins Ziel
                            winningMoves.add(move)
                        state.board.getTypeAt(action.distance+index)==FieldType.SALAD -> // Zug auf Salatfeld
                            saladMoves.add(move)
                        else -> // Ziehe Vorwärts, wenn möglich
                            selectedMoves.add(move)
                    }
                } else if (action is Card) {
                    if (action.type==CardType.EAT_SALAD) {
                        // Zug auf Hasenfeld und danch Salatkarte
                        saladMoves.add(move)
                    } // Muss nicht zusätzlich ausgewählt werden, wurde schon durch Advance ausgewählt
                } else if (action is ExchangeCarrots) {
                    if (action.value==10 && currentPlayer.carrots<30 && index<40 && currentPlayer.lastNonSkipAction !is ExchangeCarrots) {
                        // Nehme nur Karotten auf, wenn weniger als 30 und nur am Anfang und nicht zwei mal hintereinander
                        selectedMoves.add(move)
                    } else if (action.value==-10 && currentPlayer.carrots>30 && index>=40) {
                        // abgeben von Karotten ist nur am Ende sinnvoll
                        selectedMoves.add(move)
                    }
                } else if (action is FallBack) {
                    if (index>56 /* letztes Salatfeld */ && currentPlayer.salads>0) {
                        // Falle nur am Ende (index > 56) zurück, außer du musst noch einen Salat loswerden
                        selectedMoves.add(move)
                    } else if (index<=56 && index-state.getPreviousFieldByType(FieldType.HEDGEHOG, index)<5) {
                        // Falle zurück, falls sich Rückzug lohnt (nicht zu viele Karotten aufnehmen)
                        selectedMoves.add(move)
                    }
                } else {
                    // Füge Salatessen oder Skip hinzu
                    selectedMoves.add(move)
                }
            }
        }
        val move = if (!winningMoves.isEmpty()) {
            log.info("Sende Gewinnzug")
            winningMoves[rand.nextInt(winningMoves.size)]
        } else if (!saladMoves.isEmpty()) {
            log.info("Sende Zug zum Salatessen")
            saladMoves[rand.nextInt(saladMoves.size)]
        } else if (!selectedMoves.isEmpty()) {
            selectedMoves[rand.nextInt(selectedMoves.size)]
        } else {
            possibleMove[rand.nextInt(possibleMove.size)]
        }
        move.orderActions()
        return move
    }

}
