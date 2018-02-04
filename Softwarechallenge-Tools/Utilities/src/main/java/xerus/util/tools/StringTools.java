package xerus.util.tools;

import java.util.Collection;
import java.util.function.BiPredicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class StringTools {

	public final static BiPredicate<String, String> equals = (s1, s2) -> s1.equals(s2);

	/** returns the last char of the CharSequence */
	public static char lastchar(CharSequence s) {
		return s.charAt(s.length() - 1);
	}

	/** counts the occurences of a char in a String
	 * @param s String to search through
	 * @param search char to count */
	public static int count(String s, char search) {
		int amount = 0;
		for (char c : s.toCharArray())
			if (c == search)
				amount++;
		return amount;
	}

	/** splits a concatenation of doubles into an Array assuming "{@literal ,}" as separator
	 * @return double-Array containing the parsed values */
	public static double[] split(String s) {
		return split(s, ",");
	}

	/** splits a concatenation of doubles into an Array
	 * @param delimiter separator between the values
	 * @return double-Array containing the parsed values */
	public static double[] split(String s, String delimiter) {
		String[] values = s.split(delimiter);
		double[] out = new double[values.length];
		for (int i = 0; i < values.length; i++)
			out[i] = Double.parseDouble(values[i]);
		return out;
	}

	/** returns a Pair with the first element representing the first elemnt of the split and the second the rest of the String */
	public static Pair<String, String> splitoffFirst(String s, String delimiter) {
		int ind = s.indexOf(delimiter);
		return Pair.of(s.substring(0, ind), s.substring(ind + 1, s.length()));
	}

	/** wraps the given String in formatting tags and then html tags
	 * @param format format of the formatting tags<br>bold will be parsed to b and italic to i*/
	public static String html(String string, String format) {
		switch (format) {
		case "bold":
			//format = "b";
			break;
		case "italic":
			//format = "i";
			break;
		}
		return String.format("<html><%2$s>%1$s</%2$s></html>", string, format);
	}

	public static String bold(String string) {
		return html(string, "b");
	}

	/** joins the {@code elements} with the separator "{@literal ;}"
	 * @param elements
	 * @return the concatenated String */
	public static String join(CharSequence... elements) {
		return String.join(";", elements);
	}

	public static String join(String delimiter, int... numbers) {
		StringBuilder sb = new StringBuilder();
		for (int i : numbers)
			sb.append(delimiter + i);
		return sb.substring(delimiter.length());
	}

	/** joins the doubles with the given delimiter, each rounded to two decimal places */
	public static String join(String delimiter, double... params) {
		StringBuilder sb = new StringBuilder();
		for (double d : params)
			sb.append(delimiter + Tools.round(d));
		return sb.substring(delimiter.length());
	}

	public static String join(CharSequence delimiter, Collection objects) {
		StringBuilder sb = new StringBuilder();
		for (Object o : objects)
			sb.append(delimiter + o.toString());
		return sb.substring(delimiter.length());
	}

	public static String joinNatural(CharSequence... sequences) {
		String out = String.join(", ", sequences);
		int i = out.lastIndexOf(",");
		return out.substring(0, i) + " & " + out.substring(i + 2);
	}

	public static boolean contains(Object base, String value) {
		return StringUtils.containsIgnoreCase(base.toString(), value);
	}

	/** checks for if one of the Strings contains the other */
	public static boolean containsEach(String s1, String s2) {
		return s1.length() > s2.length() ? s1.contains(s2) : s2.contains(s1);
	}

}
