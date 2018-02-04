package xerus.softwarechallenge.logic2018;

import java.util.ArrayList;

import sc.plugin2018.Action;
import sc.plugin2018.Advance;
import sc.plugin2018.Card;
import sc.plugin2018.CardType;
import sc.plugin2018.ExchangeCarrots;
import sc.plugin2018.FallBack;
import sc.plugin2018.FieldType;
import sc.plugin2018.GameState;
import sc.plugin2018.Move;
import sc.plugin2018.Player;
import sc.plugin2018.util.Constants;
import xerus.softwarechallenge.Starter;
import xerus.softwarechallenge.util.LogicHandler;

/** enth�lt Grundlagen f�r eine Logik f�r die Softwarechallenge 2018 - Hase und Igel */
public abstract class LogicBase extends LogicHandler {

	public LogicBase(Starter client, String params, int debug, String version) {
		super(client, params, debug, "Jumper " + version);
	}

	@Override
	protected double evaluate(GameState state) {
		double points = state.getPointsForPlayer(currentGameState.getCurrentPlayerColor());
		if (gewonnen(state))
			points += 10000 - state.getCurrentPlayer().getCarrots();
		else
			points += state.getCurrentPlayer().getCarrots() / params[0];
		return points;
	}

	@Override
	protected double[] defaultParams() {
		return new double[]{5};
	}

	@Override
	protected String tostring(Player player) {
		return String.format("Player %s Feld: %s Gemuese: %s/%s", player.getPlayerColor(), player.getFieldIndex(), player.getSalads(), player.getCarrots());
	}

	@Override
	protected boolean gewonnen(GameState state) {
		return state.getCurrentPlayer().inGoal();
	}

	@Override
	protected Move simplemove() throws CloneNotSupportedException {
		GameState state = currentGameState;
		ArrayList<Move> possibleMove = state.getPossibleMoves(); // Enthält mindestens ein Element
		ArrayList<Move> saladMoves = new ArrayList<>();
		ArrayList<Move> winningMoves = new ArrayList<>();
		ArrayList<Move> selectedMoves = new ArrayList<>();
		Player currentPlayer = state.getCurrentPlayer();
		int index = currentPlayer.getFieldIndex();
		for (Move move : possibleMove) {
			for (Action action : move.actions) {
				if (action instanceof Advance) {
					Advance advance = (Advance) action;
					if (advance.getDistance() + index == Constants.NUM_FIELDS - 1) {
						// Zug ins Ziel
						winningMoves.add(move);
					} else if (state.getBoard().getTypeAt(advance.getDistance() + index) == FieldType.SALAD) {
						// Zug auf Salatfeld
						saladMoves.add(move);
					} else {
						// Ziehe Vorwärts, wenn möglich
						selectedMoves.add(move);
					}
				} else if (action instanceof Card) {
					Card card = (Card) action;
					if (card.getType() == CardType.EAT_SALAD) {
						// Zug auf Hasenfeld und danch Salatkarte
						saladMoves.add(move);
					} // Muss nicht zusätzlich ausgewählt werden, wurde schon durch Advance ausgewählt
				} else if (action instanceof ExchangeCarrots) {
					ExchangeCarrots exchangeCarrots = (ExchangeCarrots) action;
					if (exchangeCarrots.getValue() == 10 && currentPlayer.getCarrots() < 30 && index < 40 && !(currentPlayer.getLastNonSkipAction() instanceof ExchangeCarrots)) {
						// Nehme nur Karotten auf, wenn weniger als 30 und nur am Anfang und nicht zwei mal
						// hintereinander
						selectedMoves.add(move);
					} else if (exchangeCarrots.getValue() == -10 && currentPlayer.getCarrots() > 30 && index >= 40) {
						// abgeben von Karotten ist nur am Ende sinnvoll
						selectedMoves.add(move);
					}
				} else if (action instanceof FallBack) {
					if (index > 56 /* letztes Salatfeld */ && currentPlayer.getSalads() > 0) {
						// Falle nur am Ende (index > 56) zurück, außer du musst noch einen Salat loswerden
						selectedMoves.add(move);
					} else if (index <= 56 && index - state.getPreviousFieldByType(FieldType.HEDGEHOG, index) < 5) {
						// Falle zurück, falls sich Rückzug lohnt (nicht zu viele Karotten aufnehmen)
						selectedMoves.add(move);
					}
				} else {
					// Füge Salatessen oder Skip hinzu
					selectedMoves.add(move);
				}
			}
		}
		Move move;
		if (!winningMoves.isEmpty()) {
			log.info("Sende Gewinnzug");
			move = winningMoves.get(rand.nextInt(winningMoves.size()));
		} else if (!saladMoves.isEmpty()) {
			// es gibt die M�glichkeit einen Salat zu essen
			log.info("Sende Zug zum Salatessen");
			move = saladMoves.get(rand.nextInt(saladMoves.size()));
		} else if (!selectedMoves.isEmpty()) {
			move = selectedMoves.get(rand.nextInt(selectedMoves.size()));
		} else {
			move = possibleMove.get(rand.nextInt(possibleMove.size()));
		}
		move.orderActions();
		return move;
	}

}
