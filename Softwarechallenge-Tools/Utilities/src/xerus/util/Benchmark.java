package xerus.util;

public abstract class Benchmark {

	public static void test(Runnable... torun) {
		for (Runnable method : torun) {
			System.gc();
			Timer.start();
			method.run();
			Timer.finish();
		}
	}

	public static void test(int param, method... torun) {
		for (method method : torun) {
			System.gc();
			output(method.test(param), param);
		}
	}
	
	public static int optimise(int param, method consumer) {
		int precision = 2;
		long time = consumer.test(param);
		output(time, param, precision);
		do {
			System.gc();
			long time1 = consumer.test(param + param/precision);
			if(time1<time) {
				param += param/precision;
				time = time1;
				continue; }
			time1 = consumer.test(param - param/precision);
			if(time1<time) {
				param -= param/precision;
				time = time1;
				continue; }
			precision *= 2;
			output(time, param, precision);
		} while (precision<65);
		return param;
	}
	
	private static void output(long... values) {
		StringBuilder sb = new StringBuilder("Zeit: " + values[0]);
		if(values.length>1) {
			sb.append("Parameter: ").append(values[1]);
			if(values.length>2)
				sb.append("Präzision: ").append(values[2]);
		}
		System.out.println(sb.toString());
	}
	
	public interface method {
		
		public default long test(int param) {
			Timer.start();
			perform(param);
			return Timer.runtime();
		}
		
		abstract void perform(int param);
		
	}

}
