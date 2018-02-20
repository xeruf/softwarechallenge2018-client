package xerus.softwarechallenge.logic2018

import sc.plugin2018.*
import sc.plugin2018.util.Constants
import sc.plugin2018.util.GameRuleLogic
import xerus.ktutil.toInt
import xerus.softwarechallenge.Starter
import xerus.softwarechallenge.util.LogicHandler
import xerus.softwarechallenge.util.isType
import xerus.softwarechallenge.util.str
import java.util.*
import kotlin.math.pow
import kotlin.math.sign

/** enthält Grundlagen für eine Logik für die Softwarechallenge 2018 - Hase und Igel  */
abstract class LogicBase(client: Starter, params: String, debug: Int, version: KotlinVersion) : LogicHandler(client, params, debug, "Jumper v" + version) {

    override fun evaluate(state: GameState): Double {
        val player = state.currentPlayer
        var points = 100.0
        points += params[0] * state.getPointsForPlayer(myColor)
        // Salat und Karten
        points -= player.salads * params[1]
        points += (player.ownsCardOfType(CardType.EAT_SALAD).toInt() + player.ownsCardOfType(CardType.TAKE_OR_DROP_CARROTS).toInt()) * params[1] * 0.7
        points += player.cards.size
        // Karotten
        val distanceToGoal = 65.minus(player.fieldIndex).toDouble()
        points += distanceToGoal / 8 - (player.carrots / distanceToGoal - 2 - distanceToGoal / 5).pow(2)

        points += player.inGoal().toInt() * 1000
        val turnsLeft = 60 - state.turn
        if (turnsLeft < 4 || player.carrots > GameRuleLogic.calculateCarrots(distanceToGoal.toInt()) + turnsLeft * 10 + 20)
            points -= distanceToGoal * 100
        return points
    }

    // feld 0  -> mehr Karotten sind besser
    // feld 64(Ziel) -> maximal 10 Karotten
    // -(x/65-fieldIndex-4)²+10+fieldIndex
    // benötigte Karotten für x Felder: 0,5x2 + 0,5x

    // params: Weite Salat
    override fun defaultParams() = doubleArrayOf(2.0, 10.0)

    fun Field.isBlocked(state: GameState) = isType(FieldType.HEDGEHOG) || state.otherPlayer.fieldIndex == index || isType(FieldType.GOAL) && state.currentPlayer.salads > 0 && state.currentPlayer.carrots > 10

    override fun Player.str(): String =
            "Player %s Feld: %s Gemuese: %s/%s Karten: %s".format(playerColor, fieldIndex, salads, carrots, cards.joinToString { it.name })

    override fun gewonnen(state: GameState) =
            state.currentPlayer.inGoal()

    override fun findBestMove(): Move? {
        when (currentGameState.turn) {
            0 -> return advanceTo(10)
        // region die ersten drei Züge, wenn ich Blau bin
            1 -> {
                return if (currentGameState.otherPlayer.fieldIndex == 10) {
                    val field2 = findField(FieldType.POSITION_2)
                    if ((field2 < 5 && findField(FieldType.HARE, field2) < 10) || field2 < findField(FieldType.HARE))
                        advanceTo(field2)
                    else {
                        // Mitte zwischen zweier-Feld und Start nehmen, langsam nach außen suchen
                        var field = field2 / 2
                        var dif = 1
                        while (currentGameState.board.getTypeAt(field) != FieldType.HARE) {
                            field += dif
                            dif = -(dif + dif.sign)
                        }
                        playCard(field, CardType.EAT_SALAD)
                    }
                } else {
                    advanceTo(10)
                }
            }
            3 -> {
                if (currentGameState.otherPlayer.fieldIndex == 10) {
                    val pos = currentGameState.currentPlayer.fieldIndex
                    return if (currentGameState.fieldOfCurrentPlayer() != FieldType.POSITION_2)
                        advanceTo(findField(FieldType.POSITION_2, currentGameState.currentPlayer.fieldIndex))
                    else {
                        // Mitte zwischen Salat-Feld und Position nehmen, langsam nach außen suchen
                        var field = (10 + pos) / 2 + pos
                        var dif = 1
                        while (currentGameState.board.getTypeAt(field) != FieldType.HARE) {
                            field += dif
                            dif = -(dif + dif.sign)
                        }
                        playCard(field, CardType.EAT_SALAD)
                    }
                }
            }
            5 -> {
                if (currentGameState.currentPlayer.fieldIndex != 10)
                    return advanceTo(10)
            }
        // endregion
        }
        return super.findBestMove()
    }

    protected fun advanceTo(field: Int) =
            move(Advance(field - currentGameState.currentPlayer.fieldIndex))

    fun playCard(fieldIndex: Int, card: CardType): Move =
            advanceTo(fieldIndex).apply { actions.add(Card(card)) }

    override fun simpleMove(state: GameState): Move {
        val possibleMoves = state.possibleMoves // Enthält mindestens ein Element
        val winningMoves = ArrayList<Move>()
        val selectedMoves = ArrayList<Move>()
        val currentPlayer = state.currentPlayer
        val index = currentPlayer.fieldIndex
        if (possibleMoves.firstOrNull()?.actions?.first() is Skip)
            log.warn(GameRuleLogic.isValidToSkip(state).toString() + " - " + state.currentPlayer.str())
        for (move in possibleMoves) {
            for (action in move.actions) {
                if (action is Advance) {
                    when {
                        action.distance + index == Constants.NUM_FIELDS - 1 -> // Zug ins Ziel
                            winningMoves.add(move)
                        state.board.getTypeAt(action.distance + index) == FieldType.SALAD -> // Zug auf Salatfeld
                            winningMoves.add(move)
                        else -> // Ziehe Vorwärts, wenn möglich
                            selectedMoves.add(move)
                    }
                } else if (action is Card) {
                    if (action.type == CardType.EAT_SALAD) {
                        // Zug auf Hasenfeld und danach Salatkarte
                        winningMoves.add(move)
                    } // Muss nicht zusätzlich ausgewählt werden, wurde schon durch Advance ausgewählt
                } else if (action is ExchangeCarrots) {
                    if (action.value == 10 && currentPlayer.carrots < 30 && index < 40 && currentPlayer.lastNonSkipAction !is ExchangeCarrots) {
                        // Nehme nur Karotten auf, wenn weniger als 30 und nur am Anfang und nicht zwei mal hintereinander
                        selectedMoves.add(move)
                    } else if (action.value == -10 && currentPlayer.carrots > 30 && index >= 40) {
                        // abgeben von Karotten ist nur am Ende sinnvoll
                        selectedMoves.add(move)
                    }
                } else if (action is FallBack) {
                    if (index > 56 /* letztes Salatfeld */ && currentPlayer.salads > 0) {
                        // Falle nur am Ende (index > 56) zurück, außer du musst noch einen Salat loswerden
                        selectedMoves.add(move)
                    } else if (index <= 56 && index - state.getPreviousFieldByType(FieldType.HEDGEHOG, index) < 5) {
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
            winningMoves[rand.nextInt(winningMoves.size)]
        } else if (!selectedMoves.isEmpty()) {
            selectedMoves[rand.nextInt(selectedMoves.size)]
        } else {
            possibleMoves[rand.nextInt(possibleMoves.size)]
        }
        move.orderActions()
        return move
    }

}
