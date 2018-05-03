@file:Suppress("NOTHING_TO_INLINE")

package xerus.softwarechallenge.logic2018

import sc.plugin2018.*
import sc.plugin2018.util.Constants
import sc.plugin2018.util.GameRuleLogic
import xerus.ktutil.nullIfEmpty
import xerus.softwarechallenge.util.LogicHandler
import xerus.softwarechallenge.util.add
import xerus.softwarechallenge.util.str
import java.util.*

/** enthält Grundlagen für eine Logik für die Softwarechallenge 2018 - Hase und Igel
 * - benötigte Karotten für x Felder: 0.5x * (x + 1)
 * - Igel: 11, 15, 19, 24, 30, 37, 43, 50, 56
 * - Salate: 10, 22, 42, 57  */
abstract class LogicBase(version: String) : LogicHandler("Jumper $version") {
	
	override fun Player.str() =
			"$playerColor on $fieldIndex=${fieldTypeAt(fieldIndex)} S:$salads K:$carrots [${cards.joinToString { it.name }}] Last: ${lastNonSkipAction?.str()}"
	
	fun Player.strShort() =
			"$playerColor on $fieldIndex=${fieldTypeAt(fieldIndex)} S:$salads K:$carrots"
	
	inline fun Player.gewonnen() = fieldIndex == 64
	
	/** @return whether the player has one or more salads */
	inline val Player.hasSalad
		get() = salads > 0
	
	/** clones the move and adds a Card Action to it */
	fun Move.addCard(card: CardType, value: Int = 0) = 
			Move(this.actions).add(Card(card, value, 0))
	
	/** checks if the currentPlayer could jump on the Field at the given index */
	fun GameState.accessible(field: Int) =
			field in 1..64 && !isOccupied(field) && when (fieldTypeAt(field)) {
				FieldType.HEDGEHOG -> false
				FieldType.SALAD -> currentPlayer.hasSalad
				FieldType.GOAL -> currentPlayer.carrots <= 10 && currentPlayer.salads == 0
				else -> true
			}
	
	/** checks if the player owns sufficient carrots to move to that field,
	 * that it isn't blocked by the other player and that it is actually before the player */
	fun GameState.canAdvanceTo(field: Int) =
			field > currentPlayer.fieldIndex && field != otherPlayer.fieldIndex && currentPlayer.hasCarrotsTo(field)
	
	
	/** position of the otherPlayer for this GameState */
	inline val GameState.otherPos
		get() = otherPlayer.fieldIndex
	
	/** @return 0 when enemy is not on salad, 1 when he just arrived at salad, 2 if he just ate salad */
	val GameState.otherEatingSalad
		get() = when {
			getTypeAt(otherPos) != FieldType.SALAD -> 0
			otherPlayer.lastNonSkipAction !is EatSalad -> 1
			else -> 2
		}
	
	fun FieldType.isNot(vararg types: FieldType) =
			!types.any { this == it }
	
	inline fun Player.hasCarrotsTo(field: Int) =
			GameRuleLogic.calculateCarrots(field - fieldIndex) <= carrots
	
	inline fun GameState.advanceTo(field: Int) =
			Move(Advance(field - currentPlayer.fieldIndex))
	
	inline fun GameState.playCard(field: Int, card: CardType, value: Int = 0): Move =
			advanceTo(field).addCard(card, value)
	
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
						action.distance + index == 64 -> // Zug ins Ziel
							winningMoves.add(Move)
						state.board.getTypeAt(action.distance + index) == FieldType.SALAD -> // Zug auf Salatfeld
							winningMoves.add(Move)
						else -> // Ziehe Vorwärts, wenn möglich
							selectedMoves.add(Move)
					}
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
			winningMoves[0]
		} else if (!selectedMoves.isEmpty()) {
			selectedMoves[rand.nextInt(selectedMoves.size)]
		} else {
			possibleMoves[rand.nextInt(possibleMoves.size)]
		}
		move.setOrderInActions()
		return move
	}
	
	/**
	 * stellt mögliche Moves zusammen basierend auf dem gegebenen GameState
	 *
	 * @param state gegebener GameState
	 * @return ArrayList mit gefundenen Moves
	 */
	protected open fun findMoves(state: GameState): List<Move> =
			throw UnsupportedOperationException("findMoves is not defined!")
	
	override fun findBestMove() = breitensuche()
	
	protected abstract fun breitensuche(): Move?
	
}
