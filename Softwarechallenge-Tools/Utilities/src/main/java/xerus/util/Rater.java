package xerus.util;

public class Rater<X> {
	
	public X obj;
	public double points;
	protected boolean inv;
	
	public Rater() {
		this(null, 0);
	}
	
	/**erstellt einen invertierten Rater
	 * @param invert nicht genutzt */
	public Rater(boolean invert) {
		this(null, Double.MAX_VALUE, true);
	}
	
	public Rater(X obj, double p) {
		this(obj, p, false);
	}

	public Rater(X obj, double p, boolean invert) {
		this.obj = obj;
		points = p;
		inv = invert;
	}

	/**ersetzt die Werte durch die gegebenen, falls die Punkte höher als die vorherigen sind
	 * @param newobj das neue Objekt
	 * @param newpoints die Punkte */
	public boolean update(X newobj, double newpoints) {
		if((!inv && newpoints > points) || (inv && newpoints < points)) {
			obj = newobj;
			points = newpoints;
			return true;
		}
		return false;
	}
	
	public boolean update(X newobj, double newpoints, boolean bonus) {
		if(bonus) newpoints++;
		return update(newobj, newpoints);
	}
	
	public boolean hasobj() {
		return obj != null;
	}

	public String toString() {
		return obj.toString()+" Punkte: "+points;
	}

	public boolean equals(Object obj) {
		if(obj.getClass() != this.getClass())
			return false;
		Rater m = (Rater)obj;
		return m.points == this.points && m.obj.equals(obj);
	}
	
	public int hashCode() {
		return obj.hashCode()*9 + (int)points;
	}
	
}
