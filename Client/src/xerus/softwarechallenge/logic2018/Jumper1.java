package xerus.softwarechallenge.logic2018;

import java.util.ArrayList;
import java.util.Collection;

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

public class Jumper1 extends LogicBase {

	public Jumper1(Starter client, String params, int debug) {
		super(client, params, debug, "v0.1");
	}

	@Override
	protected Collection<Move> findMoves(GameState state) throws CloneNotSupportedException {
	    ArrayList<Move> possibleMoves = state.getPossibleMoves(); // Enth�lt mindestens ein Element
	    ArrayList<Move> winningMoves = new ArrayList<>();
	    ArrayList<Move> saladMoves = new ArrayList<>();
	    ArrayList<Move> selectedMoves = new ArrayList<>();
	    Player currentPlayer = state.getCurrentPlayer();
	    int fieldindex = currentPlayer.getFieldIndex();
	    for (Move move : possibleMoves) {
	    	for (Action action : move.actions) {
		        if (action instanceof Advance) {
		        	Advance advance = (Advance) action;
		          	if (advance.getDistance() + fieldindex == Constants.NUM_FIELDS - 1) {
		          		// Zug ins Ziel
		          		winningMoves.add(move);
		          	} else if (state.getBoard().getTypeAt(advance.getDistance() + fieldindex) == FieldType.SALAD) {
		          		// Zug auf Salatfeld
		          		saladMoves.add(move);
		          	} else {
		        	  	selectedMoves.add(move);
		          	}
		        } else if (action instanceof Card) {
		        	Card card = (Card) action;
		          	if (card.getType() == CardType.EAT_SALAD) {
		        	  	// Zug auf Hasenfeld und danach Salatkarte
		        	  	saladMoves.add(move);
		          	} // Muss nicht zusätzlich ausgewählt werden, wurde schon durch Advance ausgewählt
		        } else if (action instanceof ExchangeCarrots) {
		        	ExchangeCarrots exchangeCarrots = (ExchangeCarrots) action;
		        	if (exchangeCarrots.getValue() == 10 && currentPlayer.getCarrots() < 30 && fieldindex < 40 && !(currentPlayer.getLastNonSkipAction() instanceof ExchangeCarrots)) {
		        		// Nehme nur Karotten auf, wenn weniger als 30 und nur am Anfang und nicht zwei mal hintereinander
		        		selectedMoves.add(move);
		        	} else if (exchangeCarrots.getValue() == -10 && currentPlayer.getCarrots() > 30 && fieldindex >= 40) {
		        		// abgeben von Karotten ist nur am Ende sinnvoll
		        		selectedMoves.add(move);
		        	}
		        } else if (action instanceof FallBack) {
		        	if(fieldindex > 56) {
		        		if(currentPlayer.getSalads() > 0)
		        			selectedMoves.add(move);
		        	} else if(fieldindex - state.getPreviousFieldByType(FieldType.HEDGEHOG, fieldindex) < 5) {
		        		selectedMoves.add(move);
		        	}
		        } else {
		        	// Salatessen oder Skip
		        	selectedMoves.add(move);
		        }
	    	}
	    }
	    if (!winningMoves.isEmpty()) {
	    	return winningMoves;
	    } else if (!saladMoves.isEmpty()) {
	    	return saladMoves;
	    } else if (!selectedMoves.isEmpty()) {
	    	return selectedMoves;
	    }
	    return possibleMoves;
	}

}
