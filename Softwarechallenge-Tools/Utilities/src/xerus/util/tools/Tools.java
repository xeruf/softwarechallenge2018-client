package xerus.util.tools;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Predicate;

public class Tools {

	public static interface MultiConsumer<T> {
		void accept(T... args);
	}

	// == COLLECTIONS & ARRAYS ==

	public static <E> E find(Collection<? extends E> collection, Predicate<E> filter) {
		for (E e : collection)
			if (filter.test(e))
				return e;
		return null;
	}

	/** finds one object per Predicate in the given Collection<br>
	 * if multiple objects match the Predicate, it will return one of them
	 * @param collection the collection to search the objects in
	 * @param filter the qualifiers for the objects to be chosen - the order is represented in the indices of the output
	 * @return an Array containing the matched objects, the indices match the order of {@code filter} */
	public static <E> E[] findAll(Collection<E> collection, Predicate<E>... filter) {
		int amount = filter.length;
		E[] res = (E[]) Array.newInstance(collection.iterator().next().getClass(), filter.length);
		int found = 0;
		for (E e : collection)
			for (int i = 0; i < amount; i++)
				if (filter[i].test(e)) {
					res[i] = e;
					found++;
					if (found == amount)
						return res;
					break;
				}
		return res;
	}

	/** finds all objects matching the predicate in the given collection
	 * @param the collection to search the objects in
	 * @param filter condition for the objects that has to be met
	 * @return an Array of all found objects which match the {@code filter} */
	public static <E> E[] findAll(Collection<E> collection, Predicate<E> filter) {
		ArrayList<E> found = new ArrayList();
		for (E e : collection)
			if (filter.test(e))
				found.add(e);
		return (E[]) found.toArray();
	}

	public static <E> E[] find(Collection<? extends E> collection, Predicate<E>... filter) {
		int amount = filter.length;
		E[] res = (E[]) new Object[amount];
		int found = 0;
		for (E e : collection)
			for (int i = 0; i < amount; i++)
				if (filter[i].test(e)) {
					res[i] = e;
					found++;
					if (found == amount)
						return res;
					break;
				}
		return res;
	}

	public static <E> E getLast(List<E> list) {
		return list.get(list.size() - 1);
	}

	public static <E> E getLast(E[] array) {
		return array[array.length - 1];
	}

	public static void removelast(List c) {
		c.remove(c.size() - 1);
	}

	public static <T> T[] removelast(T[] c) {
		List<T> l = Arrays.asList(c);
		removelast(l);
		return (T[]) l.toArray();
	}

	public static <E> ArrayList<E> clone(Collection<? extends E> collection) {
		ArrayList<E> newlist = new ArrayList();
		newlist.addAll(collection);
		return newlist;
	}

	// == NUMBERS ==

	/** rounds a double to {@code n} decimal places
	 * @param number
	 * @param decimals number of digits after the decimal point
	 * @return rounded double */
	private final static int[] powersOf10 = {1, 10, 100, 1000, 10000};
	public static double round(double number, int decimals) {
		if(decimals < 5)
			return Math.rint(number / powersOf10[decimals]) * powersOf10[decimals];
		double c = Math.pow(10, decimals);
		return Math.rint(number * c) / c;
	}

	/** rounds a double to 2 decimal places
	 * @return rounded double */
	public static double round(double number) {
		return round(number, 2);
	}

	public static <E> Set<E>[] powerset(E[] input) {
		int elements = input.length;
		if (elements > 22)
			throw new IllegalArgumentException("Input too big");
		int powerElements = (int) Math.pow(2, elements);
		Set<E>[] p = new HashSet[powerElements];
		for (int i = 0; i < powerElements; i++) {
			String binary = intToBinary(i, elements);
			Set<E> innerSet = new HashSet<>();
			for (int j = 0; j < elements; j++)
				if (binary.charAt(j) == '1')
					innerSet.add(input[j]);
			p[i] = innerSet;
		}
		return p;
	}

	/** Converts the given integer to a String representing a binary number with the specified number of digits */
	public static String intToBinary(int binary, int digits) {
		return String.format("%0" + digits + "d", Integer.valueOf(Integer.toBinaryString(binary)));
	}

	// == RANDOM ==

	/** convenience function to get a String reprasentation of the current localized time
	 * @return localized time in hh:mm:ss format */
	public static String time() {
		long time = System.currentTimeMillis();
		return time(time + TimeZone.getDefault().getOffset(time));
	}

	/** provides a String reprasentation of the given time
	 * @return {@code millis} in hh:mm:ss format */
	public static String time(long millis) {
		long secs = millis / 1000;
		return String.format("%02d:%02d:%02d", (secs % 86400) / 3600, (secs % 3600) / 60, secs % 60);
	}

	public static String byteCountString(long bytes) {
		int unit = 1024;
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		char prefix = (" KMGTPE").charAt(Math.max(exp, 0));
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), prefix);
	}

	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static Thread doInBackground(Runnable r) {
		Thread t = new Thread(r);
		t.start();
		return t;
	}

	public static void writeObject(String filename, Object obj) throws IOException, FileNotFoundException {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
			oos.writeObject(obj);
			oos.flush();
		}
	}

	public static Object readObject(String filename) throws IOException, FileNotFoundException {
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
			return ois.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void dumpStream(InputStream inputstream) {
		if (inputstream == null) {
			System.out.println("The Stream is null!");
			return;
		}
		String cur;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputstream))) {
			while ((cur = reader.readLine()) != null)
				System.out.println(cur);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println();
	}

	public static int byteArrayToInt(byte[] b) {
		return b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16 | (b[0] & 0xFF) << 24;
	}

	public static final byte[] intToByteArray(int value) {
		return new byte[]{(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
	}

	public static void print(Object o) {
		String out = o.toString();
		if(o.getClass().isArray()) {
			int len = Array.getLength(o);
			Object[] objects = new Object[len];
			for(int i=0; i<len; i++)
				objects[i] = Array.get(o, i);
			out = Arrays.toString(objects);
		}
		System.out.println(out);
	}

}
