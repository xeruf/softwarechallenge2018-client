package xerus.util.tools;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ConnectionTools {

	public static HttpURLConnection createConnection(String url) throws MalformedURLException, IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setRequestProperty("Accept-Charset", "UTF-8");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
		return connection;
	}
	
	public static HttpURLConnection Post(HttpURLConnection connection, String... params) throws IOException {
		connection.setDoOutput(true); // Triggers POST.
		try (OutputStream output = connection.getOutputStream()) {
		    output.write(String.join("&", params).getBytes("UTF-8"));
		}
		return connection;
	}
	
	public static HttpURLConnection createPostConnection(String url, String... params) throws MalformedURLException, IOException {
		return Post(createConnection(url), params);
	}
	
	public static void dumpResponse(HttpURLConnection connection) {
		try {
			if(connection.getResponseCode()%100 != 5) {
				for (Entry<String, List<String>> e : connection.getHeaderFields().entrySet()) {
					System.out.println(e);
				}
			}
			Tools.dumpStream(connection.getInputStream());
		} catch (IOException e1) {
			Tools.dumpStream(connection.getErrorStream());
		}
	}
	
	public static class HTTPQuery {
		private Map<String, List<String>> query;
		
		public HTTPQuery(String... queries) {
			query = new HashMap<>();
			addQueries(queries);
		}

		public HTTPQuery addQuery(String key, String... vals) {
			List<String> val = new ArrayList<>(Arrays.asList(vals));
			if(query.containsKey(key))
				query.get(key).addAll(val);
			else
				query.put(key, val);
			return this;
		}
		
		public HTTPQuery addQueries(String... queries) {
			for (String s : queries)
				addQuery(s.split("=")[0], s.split("=")[1]);
			return this;
		}
		
		public HTTPQuery joinQuery(HTTPQuery other) {
			for (Entry<String, List<String>> en : other.query.entrySet())
				addQuery(en.getKey(), en.getValue().toArray(new String[0]));
			return this;
		}
		
		public String getQuery() {
			if(query.isEmpty())
				return "";
			String q = "?";
			for (Entry<String, List<String>> e : query.entrySet()) {
				q += e.getKey() + "=" + StringTools.join(",", e.getValue()) + "&";
			}
			q = q.substring(0, q.length() - 1);
			return q;
		}

		public String toString() {
			return getQuery();
		}
		
	}

}
