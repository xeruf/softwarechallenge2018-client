package xerus.util;

public class Timer {

	private static Timer t;
	private long time;
	
	public Timer() {
		time = System.currentTimeMillis();
	}
	
	public static void start() {
		t = new Timer();
	}
	
	public static long runtime() {
		return t.time();
	}
	
	public long time() {
		return System.currentTimeMillis() - time;
	}
	
	public static long finish() {
		return finish("Time");
	}
	
	public static long finish(String msg) {
		if(t == null)
			return 0;
		long time = runtime();
		t = null;
		System.out.println(parseTime(msg, time));
		return time;
	}
	
	public static String parseTime(String msg, long time) {
		StringBuilder res = new StringBuilder(msg);
		res.append(": ");
		if(time < 10000)
			res.append(time).append("m");
		else
			res.append((time/100) / 10.0);
		return res.append("s").toString();
	}

}
