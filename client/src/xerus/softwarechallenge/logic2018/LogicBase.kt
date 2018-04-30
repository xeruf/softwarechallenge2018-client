@file:Suppress("NOTHING_TO_INLINE")

package xerus.softwarechallenge.logic2018

import sc.plugin2018.*
import sc.plugin2018.util.Constants
import sc.plugin2018.util.GameRuleLogic
import xerus.ktutil.nullIfEmpty
import xerus.ktutil.to
import xerus.softwarechallenge.util.LogicHandler
import xerus.softwarechallenge.util.add
import xerus.softwarechallenge.util.str
import java.nio.file.Path
import java.util.*
import kotlin.math.pow

/** enthält Grundlagen für eine Logik für die Softwarechallenge 2018 - Hase und Igel
  * - Igel: 11, 15, 19, 24, 30, 37, 43, 50, 56
  * - Salate: 10, 22, 42, 57  */
abstract class LogicBase(version: String) : LogicHandler("Jumper $version") {
	
	override fun Player.str(): String =
			"Player %s Feld: %s Gemuese: %s/%s Karten: %s LastAction: %s".format(playerColor, fieldIndex, salads, carrots, cards.joinToString { it.name }, lastNonSkipAction?.str())
	
	override fun gewonnen(state: GameState, player: Player) = player.inGoal()
	
	/** @return whether the player has one or more salads */
	inline fun Player.hasSalad() = salads > 0
	
	/** clones the move and adds a Card Action to it */
	fun Move.addCard(card: CardType, value: Int = 0) = Move(this.actions).add(Card(card, value, 0))
	
	/** checks if the currentPlayer could jump on the Field at the given index */
	fun GameState.accessible(field: Int): Boolean {
		val type = fieldTypeAt(field)
		return field in 1..64 && !isOccupied(field) && type != FieldType.HEDGEHOG && (type != FieldType.SALAD || currentPlayer.hasSalad()) && (type != FieldType.GOAL || (currentPlayer.carrots <= 10 && currentPlayer.salads == 0))
	}
	
	/** @return position of the otherPlayer for this GameState */
	inline fun GameState.otherPos() = otherPlayer.fieldIndex
	
	/** @return 0 when enemy is not on salad, 1 when he just arrived at salad, 2 if he just ate salad */
	fun GameState.otherEatingSalad(): Int {
		if (getTypeAt(otherPos()) != FieldType.SALAD)
			return 0
		if (otherPlayer.lastNonSkipAction !is EatSalad)
			return 1
		return 2
	}
	
	fun FieldType.isNot(vararg types: FieldType) =
			!types.any { this == it }
	
	/** checks if the player owns sufficient carrots to move to that field, that it isn't blocked by the other player and that it is actually before the player */
	private fun canAdvanceTo(field: Int) =
			field > currentPlayer.fieldIndex && field != currentState.otherPlayer.fieldIndex
					&& GameRuleLogic.calculateCarrots(field - currentPlayer.fieldIndex) <= currentPlayer.carrots
	
	
	fun advanceTo(field: Int, fieldIndex: Int = currentPlayer.fieldIndex) =
			Move(Advance(field - fieldIndex))
	
	fun playCard(fieldIndex: Int, card: CardType): Move =
			advanceTo(fieldIndex).apply { actions.add(Card(card)) }
	
	override fun simpleMove(state: GameState): Move {
		val possibleMoves = findMoves(state).nullIfEmpty() ?: state.possibleMoves
		val winningMoves = ArrayList<Move>()
		val selectedMoves = ArrayList<Move>()
		val currentPlayer = state.currentPlayer
		val index = currentPlayer.fieldIndex
		for (Move in possibleMoves) {
			for (action in Move.actions) {
				if (action is Advance) {
					when {
						action.distance + index == Constants.NUM_FIELDS - 1 -> // Zug ins Ziel
							winningMoves.add(Move)
						state.board.getTypeAt(action.distance + index) == FieldType.SALAD -> // Zug auf Salatfeld
							winningMoves.add(Move)
						else -> // Ziehe Vorwärts, wenn möglich
							selectedMoves.add(Move)
					}
				} else if (action is Card) {
					if (action.type == CardType.EAT_SALAD) {
						// Zug auf Hasenfeld und danach Salatkarte
						winningMoves.add(Move)
					} // Muss nicht zusätzlich ausgewählt werden, wurde schon durch Advance ausgewählt
				} else if (action is ExchangeCarrots) {
					if (action.value == 10 && currentPlayer.carrots < 30 && index < 40 && currentPlayer.lastNonSkipAction !is ExchangeCarrots) {
						// Nehme nur Karotten auf, wenn weniger als 30 und nur am Anfang und nicht zwei mal hintereinander
						selectedMoves.add(Move)
					} else if (action.value == -10 && currentPlayer.carrots > 30 && index >= 40) {
						// abgeben von Karotten ist nur am Ende sinnvoll
						selectedMoves.add(Move)
					}
				} else if (action is FallBack) {
					if (index > 56 /* letztes Salatfeld */ && currentPlayer.salads > 0) {
						// Falle nur am Ende (index > 56) zurück, außer du musst noch einen Salat loswerden
						selectedMoves.add(Move)
					} else if (index <= 56 && index - state.getPreviousFieldByType(FieldType.HEDGEHOG, index) < 5) {
						// Falle zurück, falls sich Rückzug lohnt (nicht zu viele Karotten aufnehmen)
						selectedMoves.add(Move)
					}
				} else {
					// Füge Salatessen oder Skip hinzu
					selectedMoves.add(Move)
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
		move.setOrderInActions()
		return move
	}
	
}
