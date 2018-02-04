package xerus.softwarechallenge.util;

import sc.plugin2018.Move;
import xerus.util.Rater;

/** speichert einen Zug und die dazugehörige Punktzahl */
public class MP extends Rater<Move> {

	/** erzeugt einen leeren MP */
	public MP() {
		super();
	}
	
	/** erzeugt einen neuen MP mit dem gegebenen Zug und den Punkten */
	public MP(Move m, double p) {
		super(m,p);
	}
	
	public String toString() {
		return obj.toString().substring(28) + "Punkte: " + points;
	}
	
}
