@file:Suppress("NOTHING_TO_INLINE")

package xerus.softwarechallenge.logic2018

import sc.plugin2018.*
import sc.plugin2018.util.GameRuleLogic
import xerus.softwarechallenge.util.*
import java.util.*

/** enthält Grundlagen für eine Logik für die Softwarechallenge 2018 - Hase und Igel
 * - benötigte Karotten für x Felder: 0.5x * (x + 1)
 * - Igel: 11, 15, 19, 24, 30, 37, 43, 50, 56
 * - Salate: 10, 22, 42, 57  */
abstract class LogicBase : LogicHandler() {
	
	@F val skip = listOf(Move(Skip()))
	
	override fun Player.str() =
			this.strShort() + " [${cards.joinToString { it.name }}] Last: ${lastNonSkipAction?.str()}"
	
	protected inline fun Player.strShort() =
			"$playerColor on $fieldIndex=${fieldTypeAt(fieldIndex)} S:$salads K:$carrots"
	
	protected inline fun Player.gewonnen() = fieldIndex == 64
	
	/** @return whether the player has one or more salads */
	protected inline val Player.hasSalad
		get() = salads > 0
	
	protected inline fun goalPoints(player: Player) = if (player.fieldIndex == 64) 10000 - player.carrots * 10 else 0
	
	/** clones the move and adds a Card Action to it */
	protected fun Move.addCard(card: CardType, value: Int = 0) =
			Move(this.actions).add(Card(card, value, 0))
	
	/** checks if the currentPlayer could jump on the Field at the given index */
	protected fun GameState.accessible(field: Int, carrots: Int = currentPlayer.carrots) =
			field in 1..64 && !isOccupied(field) && when (fieldTypeAt(field)) {
				FieldType.HEDGEHOG -> false
				FieldType.HARE -> currentPlayer.cards.size > 0
				FieldType.SALAD -> currentPlayer.hasSalad
				FieldType.GOAL -> carrots <= 10 && currentPlayer.salads == 0
				else -> true
			}
	
	/** returns true if
	 * - the player owns sufficient carrots to move to that field
	 * - it isn't blocked by the other player
	 * - it is ahead of the player */
	protected fun GameState.canAdvanceTo(field: Int) =
			field > currentPlayer.fieldIndex && field != otherPlayer.fieldIndex && currentPlayer.hasCarrotsTo(field)
	
	
	/** position of the otherPlayer for this GameState */
	protected inline val GameState.otherPos
		get() = otherPlayer.fieldIndex
	
	/** @return 0 when enemy is not on salad, 1 when he just arrived at salad, 2 if he just ate salad */
	protected val GameState.otherEatingSalad
		get() = when {
			getTypeAt(otherPos) != FieldType.SALAD -> 0
			otherPlayer.lastNonSkipAction !is EatSalad -> 1
			else -> 2
		}
	
	protected inline fun Player.hasCarrotsTo(field: Int) =
			GameRuleLogic.calculateCarrots(field - fieldIndex) <= carrots
	
	protected inline fun GameState.advanceTo(field: Int) =
			Move(Advance(field - currentPlayer.fieldIndex))
	
	protected inline fun GameState.playCard(field: Int, card: CardType, value: Int = 0): Move =
			advanceTo(field).addCard(card, value)
	
	override fun GameState.simpleMove(): Move {
		val possibleMoves = findMoves()
		val winningMoves = ArrayList<Move>(4)
		val selectedMoves = ArrayList<Move>()
		val player = currentPlayer
		val index = player.fieldIndex
		for (Move in possibleMoves) {
			for (action in Move.actions) {
				if (action is Advance) {
					when {
						action.distance + index == 64 -> // Zug ins Ziel
							winningMoves.add(Move)
						board.getTypeAt(action.distance + index) == FieldType.SALAD -> // Zug auf Salatfeld
							winningMoves.add(Move)
						else -> // Ziehe Vorwärts, wenn möglich
							selectedMoves.add(Move)
					}
				} else if (action is ExchangeCarrots) {
					if (action.value == 10 && player.carrots < 30 && index < 40 && player.lastNonSkipAction !is ExchangeCarrots) {
						// Nehme nur Karotten auf, wenn weniger als 30 und nur am Anfang und nicht zwei mal hintereinander
						selectedMoves.add(Move)
					} else if (action.value == -10 && player.carrots > 30 && index >= 40) {
						// abgeben von Karotten ist nur am Ende sinnvoll
						selectedMoves.add(Move)
					}
				} else if (action is FallBack) {
					if (index > 56 /* letztes Salatfeld */ && player.salads > 0) {
						// Falle nur am Ende (index > 56) zurück, außer du musst noch einen Salat loswerden
						selectedMoves.add(Move)
					} else if (index <= 56 && index - getPreviousFieldByType(FieldType.HEDGEHOG, index) < 5) {
						// Falle zurück, falls sich Rückzug lohnt (nicht zu viele Karotten aufnehmen)
						selectedMoves.add(Move)
					}
				} else {
					// Füge Salatessen oder Skip hinzu
					selectedMoves.add(Move)
				}
			}
		}
		val move = when {
			winningMoves.isNotEmpty() -> winningMoves[0]
			selectedMoves.isNotEmpty() -> selectedMoves[rand.nextInt(selectedMoves.size)]
			else -> possibleMoves[rand.nextInt(possibleMoves.size)]
		}
		move.setOrderInActions()
		return move
	}
	
}
