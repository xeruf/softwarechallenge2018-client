@file:Suppress("NOTHING_TO_INLINE")

package xerus.softwarechallenge.logic2018

import sc.plugin2018.*
import sc.plugin2018.util.Constants
import sc.plugin2018.util.GameRuleLogic
import xerus.ktutil.nullIfEmpty
import xerus.ktutil.toInt
import xerus.softwarechallenge.Starter
import xerus.softwarechallenge.util.LogicHandler
import xerus.softwarechallenge.util.add
import xerus.softwarechallenge.util.str
import java.util.*
import kotlin.math.pow

/** enthält Grundlagen für eine Logik für die Softwarechallenge 2018 - Hase und Igel  */
abstract class LogicBase(client: Starter, params: String, debug: Int, version: KotlinVersion) : LogicHandler(client, params, debug, "Jumper $version") {
	
	override fun evaluate(state: GameState): Double {
		val player = state.currentPlayer
		var points = params[0] * state.getPointsForPlayer(myColor) + 30
		val distanceToGoal = Constants.NUM_FIELDS.minus(player.fieldIndex).toDouble()
		
		// Salat und Karten
		points -= player.salads * params[1] * (5 - Math.log(distanceToGoal))
		points += (player.ownsCardOfType(CardType.EAT_SALAD).toInt() + player.ownsCardOfType(CardType.TAKE_OR_DROP_CARROTS).toInt()) * params[1] * 0.6
		points += player.cards.size
		// Karotten
		points += carrotPoints(player, distanceToGoal) * 4
		points -= carrotPoints(state.otherPlayer, Constants.NUM_FIELDS.minus(state.otherPos()).toDouble())
		points -= (state.fieldOfCurrentPlayer() == FieldType.CARROT).toInt()
		
		
		// Zieleinlauf
		points += player.inGoal().toInt() * 100000
		val turnsLeft = 60 - state.turn
		if (turnsLeft < 2 || turnsLeft < 6 && player.carrots > GameRuleLogic.calculateCarrots(distanceToGoal.toInt()) + turnsLeft * 10 + 20)
			points -= distanceToGoal * 100
		return points
	}
	
	private inline fun carrotPoints(player: Player, distance: Double) =
			(1.1).pow(-0.2 * (player.carrots.minus(distance * 4).div(30 + distance)).pow(2)).times(params[2])
	// 1.2^(-((x-z*4)/(30+z))^2)*45
	
	//distance.div(8) - (player.carrots.div(distance) - 2 - distance.div(5)).pow(2)
	
	// feld 0  -> mehr Karotten sind besser
	// feld 64(Ziel) -> maximal 10 Karotten
	// -(x/65-fieldIndex-4)²+10+fieldIndex
	// benötigte Karotten für x Felder: 0.5x * (x + 1)
	
	/** Weite, Salat, Karotten */
	override fun defaultParams() = doubleArrayOf(2.0, 20.0, 5.0)
	
	override fun Player.str(): String =
			"Player %s Feld: %s Gemuese: %s/%s Karten: %s LastAction: %s".format(playerColor, fieldIndex, salads, carrots, cards.joinToString { it.name }, lastNonSkipAction?.str())
	
	override fun gewonnen(state: GameState, player: Player) = player.inGoal()
	
	// Igel: 11, 15, 19, 24, 30, 37, 43, 50, 56
	// Salate: 10, 22, 42, 57
	override fun predefinedMove(): Move? {
		val pos = currentPlayer.fieldIndex
		val otherPos = currentState.otherPos()
		
		if (canAdvanceTo(10))
			return advanceTo(10)
		
		if (pos == 10) {
			if (currentPlayer.lastNonSkipAction !is EatSalad)
				return Move(EatSalad())
			else {
				val pos2 = findField(FieldType.POSITION_2)
				if (otherPos > 10 && canAdvanceTo(pos2))
					return advanceTo(pos2)
				val pos1 = findField(FieldType.POSITION_1)
				if (otherPos < 11 && canAdvanceTo(pos1))
					return advanceTo(pos1)
				val hare = findField(FieldType.HARE, 12)
				if (otherPos != hare && currentState.turn < 6)
					return advanceTo(hare).add(Card(CardType.TAKE_OR_DROP_CARROTS, 20, 1))
			}
		}
		
		if (otherPos == 22 && pos < 22) {
			val pos21 = findField(FieldType.POSITION_2)
			if (canAdvanceTo(pos21)) {
				val pos2circular = findCircular(FieldType.POSITION_2, 11 + pos / 2)
				if (currentState.otherEatingSalad() == 2) {
					if (pos2circular < 22 && canAdvanceTo(pos2circular))
						return advanceTo(pos2circular)
					if (pos21 < 22)
						return advanceTo(pos21)
				} else {
					val pos22 = findField(FieldType.POSITION_2, 20)
					if (pos22 < 22 && pos < pos21 && pos21 != pos22)
						return advanceTo(pos21)
					if (pos22 == 21 && canAdvanceTo(21) && pos in arrayOf(12, 16, 20))
						return advanceTo(pos22)
				}
				if (pos > 11)
					return Move(FallBack())
			}
		}
		
		// todo eat last salad
		if (otherPos == 57 && pos > 57 && currentState.otherEatingSalad() == 2 && currentPlayer.hasSalad())
			return Move(FallBack())
		
		when (currentState.turn) {
		// region Rot
			6 -> {
				if (canAdvanceTo(22))
					return advanceTo(22)
				if (otherPos < 11) {
					val pos1 = findField(FieldType.POSITION_1)
					if (canAdvanceTo(pos1))
						return advanceTo(pos1)
				}
				val pos2 = findField(FieldType.POSITION_2, 16)
				if (otherPos > 19) {
					if (pos < pos2)
						return advanceTo(pos2)
					else if (otherPos == 22)
						return Move(FallBack())
				}
			}
			8 -> {
				if (canAdvanceTo(22))
					return advanceTo(22)
				if (otherPos == 22) {
					val pos2 = findField(FieldType.POSITION_2)
					if (pos2 < 21)
						return advanceTo(pos2)
					else if (fieldTypeAt(pos) != FieldType.HEDGEHOG)
						return Move(FallBack())
				}
			}
		// endregion
		// region Blau
			1 -> {
				if (otherPos == 10) {
					val field2 = findField(FieldType.POSITION_2)
					return if ((field2 < 5 && findField(FieldType.HARE, field2) < 10) || field2 < findField(FieldType.HARE))
						advanceTo(field2)
					else {
						val hare = findCircular(FieldType.HARE, field2 / 2)
						playCard(hare, CardType.EAT_SALAD)
					}
				}
			}
			3 -> {
				if (otherPos == 10) {
					return if (currentState.fieldOfCurrentPlayer() != FieldType.POSITION_2)
						advanceTo(findField(FieldType.POSITION_2, pos))
					else {
						val hare = findCircular(FieldType.HARE, (10 + pos) / 2)
						playCard(hare, CardType.EAT_SALAD)
					}
				}
			}
		// endregion
		}
		return null
	}
	
	/** @return whether the player has one or more salads */
	inline fun Player.hasSalad() = salads > 0
	
	/** clones the move and adds a Card Action to it */
	fun Move.addCard(card: CardType, value: Int = 0) = Move(this.actions).add(Card(card, value, 0))
	
	/** checks if the currentPlayer could jump on the Field at the given index */
	fun GameState.accessible(field: Int): Boolean {
		val type = fieldTypeAt(field)
		return field in 1..64 && !isOccupied(field) && type != FieldType.HEDGEHOG && (type != FieldType.SALAD || currentPlayer.hasSalad()) && (type != FieldType.GOAL || (currentPlayer.carrots <= 10 && currentPlayer.salads == 0))
	}
	
	//fun Field.isBlocked(state: GameState) = isType(FieldType.HEDGEHOG) || state.otherPlayer.fieldIndex == index || isType(FieldType.GOAL) && state.currentPlayer.salads > 0 && state.currentPlayer.carrots > 10
	
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
