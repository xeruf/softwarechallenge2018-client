package xerus.softwarechallenge.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import sc.plugin2018.*;
import sc.shared.GameResult;
import sc.shared.InvalidMoveException;
import sc.shared.PlayerColor;
import sc.shared.PlayerScore;
import xerus.softwarechallenge.Starter;
import xerus.util.helpers.Timer;
import xerus.util.tools.StringTools;

import java.security.SecureRandom;
import java.util.*;

/**
 * schafft Grundlagen fuer eine Logik
 */
public abstract class LogicHandler extends Timer implements IGameHandler {

	protected final Logger log;
	protected GameState currentGameState;

	private Starter client;

	public LogicHandler(Starter client, String params, int debug, String identifier) {
		this.client = client;
		log = (Logger) LoggerFactory.getLogger(this.getClass());
		log.warn(identifier + " - Parameter: " + params);
		if (debug == 2) {
			log.setLevel(Level.DEBUG);
			log.info("Debug enabled");
		} else if (debug == 1) {
			log.setLevel(Level.INFO);
			log.info("Info enabled");
		}
		this.params = !params.isEmpty() ? StringTools.split(params) : defaultParams();
	}

	@Override
	public void onRequestAction() {
		start();
		gueltigeZuege = 0;
		ungueltigeZuege = 0;
		lastdepth = 0;
		Move move;

		move = findBestMove();
		if (move == null || testmove(currentGameState, move) == null) {
			log.info("Kein gueltiger Move gefunden: {} - Suche simplemove!", move);
			move = simpleMove();
		}

		sendAction(move);
		log.info(String.format("Zeit: %sms Gefundene Moves: %s/%s Kalkulationstiefe: %s Genutzt: %s", runtime() / 1000_000, gueltigeZuege, ungueltigeZuege, depth, lastdepth));
	}

	// region Zugsuche

	/**
	 * Findet den Move der beim aktuellen GameState am besten ist<br>
	 * verweist standardmäßig auf die breitensuche
	 */
	protected Move findBestMove() {
		return breitensuche();
	}

	/**
	 * ermittelt die Wertigkeit der gegebenen Situation
	 *
	 * @return Bewertung der gegebenen Situation
	 */
	protected abstract double evaluate(GameState state);

	protected double[] params;

	/**
	 * Die Standard-Parameter - gibt in der Basisimplementierung ein leeres Array zur�ck
	 */
	protected double[] defaultParams() {
		return new double[]{};
	}

	protected int depth;
	private int lastdepth;

	/**
	 * sucht den besten Move per Breitensuche basierend auf dem aktuellen GameState
	 */
	protected Move breitensuche() {
		// Variablen vorbereiten
		Queue<Node> queue = new LinkedList<>();
		depth = 0;
		MP bestMove = new MP();

		// Queue füllen
		initplayer();
		Collection<Move> moves = findMoves(currentGameState);
		if (log.isDebugEnabled())
			log.debug("Gefundene Zuege:\n{}", toString(moves));
		if (moves.size() == 1)
			return moves.iterator().next();

		for (Move move : moves) {
			GameState newstate = testmove(currentGameState, move);
			if (newstate == null)
				continue;
			if (gewonnen(newstate))
				return move;
			Node newnode = new Node(newstate, move);
			queue.add(newnode);
			double value = evaluate(newstate);
			if (log.isDebugEnabled())
				log.debug("{} Punkte: {}", toString(move), value);
			bestMove.update(move, value);
		}

		// Breitensuche
		Node node;
		breitensuche:
		while (depth < 7 && runtime() < 1700) {
			if ((node = queue.poll()) == null)
				break;
			if (depth != node.depth) {
				depth = node.depth;
				log.debug("Tiefe: " + depth);
			}
			GameState nodestate = node.gamestate;
			initplayer(nodestate);
			findMoves(nodestate);
			// sinnlosen Zug ausschliessen
			if (moves.size() == 0)
				continue;
			for (Move move : moves) {
				if (runtime() > 1750)
					break breitensuche;
				initplayer(nodestate);
				GameState newstate = testmove(nodestate, move);
				if (newstate == null)
					continue;
				Node newnode = node.update(newstate);
				double points = evaluate(node.gamestate) + node.bonus - node.depth;
				// Aussortieren
				if (points + 5 < bestMove.points)
					continue;
				queue.add(newnode);
				// Aktualisierung der Bestpunktzahl
				if (bestMove.update(node.move, points)) {
					lastdepth = depth;
					if (log.isDebugEnabled())
						log.debug("Neuer bester Zug bei Tiefe %s: %s Punkte %s - %s", new Object[]{depth, toString(node.move), points, toString(nodestate.getCurrentPlayer())});
				}
			}
		}
		//if (evaluate(currentGameState) > bestMove.points)
		//	log.warn("Bin wahrscheinlich in Sackgasse!");
		return bestMove.obj;
	}

	/**
	 * stellt mögliche Moves zusammen basierend auf dem gegebenen GameState<br>
	 * Diese Methode wird für die Tiefensuche genutzt<br>
	 * muss überschrieben werden um die {@link #breitensuche} zu nutzen
	 *
	 * @param state gegebener GameState
	 * @return ArrayList mit gefundenen Moves
	 */
	protected Collection<Move> findMoves(GameState state) {
		throw new UnsupportedOperationException("Es wurde keine Methode für das ermitteln der moves für die Tiefensuche definiert!");
	}

	private class Node {

		public GameState gamestate;
		public Move move;
		public int depth;
		public double bonus;

		/**
		 * erstellt eine neue Node mit dem gegebenen GameState und Move ohne bonus
		 */
		public Node(GameState state, Move m) {
			this(state, m, 0);
		}

		/**
		 * erstellt eine neue Node mit dem gegebenen GameState und Move mit bonus
		 */
		public Node(GameState state, Move m, double bonus) {
			this(state, m, bonus, 1);
		}

		private Node(GameState state, Move m, double b, int d) {
			move = m;
			gamestate = state;
			depth = d;
			bonus = b;
		}

		/**
		 * gibt eine neue Node zurück
		 *
		 * @param newState der neue GameState
		 * @return neue Node mit dem GameState und depth + 1
		 */
		public Node update(GameState newState) {
			return new Node(newState, move, bonus, depth + 1);
		}

	}

	// GRUNDLAGEN

	@Override
	public void sendAction(Move move) {
		if (move == null) {
			log.warn("Kein Zug mehr möglich!");
			client.sendMove(move());
			return;
		}
		log.debug("Sende {}\n", toString(move));
		move.setOrderInActions();
		client.sendMove(move);
	}

	protected void initplayer() {
		Player p = currentGameState.getCurrentPlayer();
	}

	protected void initplayer(GameState nodestate) {
	}

	protected PlayerColor myColor;

	@Override
	public void onUpdate(GameState state) {
		currentGameState = state;
		Player dran = state.getCurrentPlayer();
		if (myColor == null && client.getColor() != null) {
			//display(state);
			myColor = client.getColor();
			log.info("Ich bin {}", myColor);
		}
		log.info("Zug: {} Dran: {} - " + toString(dran), state.getTurn(), isme(dran.getPlayerColor()));
	}

	/*public static void display(GameState state) {
		JFrame frame = new JFrame();
		String fieldString = state.getBoard().toString();
		MyTable table = new ScrollableJTable("Index", "Field").addToComponent(frame, null);
		String[] fields = fieldString.split("index \\d+");
		for (int i = 1; i < fields.length - 1; i++) {
			table.addRow(i + "", fields[i]);
		}
		table.fitColumns(0);
		frame.pack();
		frame.setVisible(true);
	}*/

	@Override
	public void onUpdate(Player arg0, Player arg1) {
	}

	private String isme(PlayerColor color) {
		return color == myColor ? "ich" : "nicht ich";
	}

	public static String toString(Collection<Move> moves) {
		StringBuilder out = new StringBuilder();
		for (Move m : moves)
			out.append("| ").append(toString(m)).append("\n");
		return out.toString();
	}

	public static String toString(Move m) {
		StringBuilder out = new StringBuilder("Move: ");
		for (Action action : m.actions)
			out.append(action.toString()).append(", ");
		return out.substring(0, out.length() - 2);
	}

	public abstract String toString(Player player);

	protected abstract boolean gewonnen(GameState state);

	// Zugmethoden

	/**
	 * constructs a new Move containing the given actions
	 */
	protected Move move(Action... actions) {
		return new Move(Arrays.asList(actions));
	}

	protected boolean perform(Action a, GameState s) {
		try {
			a.perform(s);
			return true;
		} catch (InvalidMoveException e) {
			return false;
		}
	}

	int gueltigeZuege;
	int ungueltigeZuege;

	/**
	 * testet einen Move mit dem gegebenen GameState
	 *
	 * @param state gegebener State
	 * @param m     der zu testende Move
	 * @return null, wenn der Move fehlerhaft ist, sonst den GameState nach dem Move
	 */
	protected GameState testmove(GameState state, Move m) {
		GameState newstate = clone(state);
		try {
			m.setOrderInActions();
			m.perform(newstate);
			newstate.setTurn(newstate.getTurn() + 1);
			newstate.switchCurrentPlayer();
			gueltigeZuege++;
			return newstate;
		} catch (InvalidMoveException e) {
			ungueltigeZuege++;
			if (log.isDebugEnabled()) {
				String message = e.getMessage();
				// if(!message.equals("Die maximale Geschwindigkeit von 6 darf nicht überschritten werden."))
				log.info("FEHLERHAFTER ZUG: {} - {} FEHLER: " + message, toString(state.getCurrentPlayer()), toString(m));
			}
		}
		return null;
	}

	protected abstract Move simpleMove();

	protected GameState clone(GameState s) {
		try {
			return s.clone();
		} catch (CloneNotSupportedException e) {
			log.error("Konnte GameState nicht klonen!");
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void gameEnded(GameResult data, PlayerColor color, String errorMessage) {
		List<PlayerScore> scores = data.getScores();
		String cause = String.format("Ich %s Gegner %s", scores.get(color.ordinal()).getCause(), scores.get(color.opponent().ordinal()).getCause());
		if (data.getWinners().isEmpty()) {
			log.warn("Kein Gewinner! Grund: {}", cause);
			// System.exit(0);
		}
		PlayerColor winner = ((Player) data.getWinners().get(0)).getPlayerColor();
		int myscore = getScore(scores, color);
		if (data.isRegular())
			log.warn(String.format("Spiel beendet! Gewinner: %s Punkte: %s Gegner: %s", isme(winner), myscore, getScore(scores, color.opponent())));
		else
			log.warn(String.format("Spiel unregulaer beendet! Punkte: %s Grund: %s", myscore, cause));
		// System.exit((color == winner ? 100 : 0) + myscore);
	}

	private static int getScore(List<PlayerScore> scores, PlayerColor color) {
		return scores.get(color.ordinal()).getValues().get(1).intValue();
	}

	public final Random rand = new SecureRandom();

}
