package xerus.util.javafx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.scene.Node;
import javafx.scene.Parent;

public class JFXTools {

	public static Collection<Node> findByStyleClass(Parent root, String className) {
		Collection<Node> found = new ArrayList<>();
		findByClassRec(root, node -> node.getStyleClass().contains(className), found);
		return found;
	}

	public static <T> Collection<T> findByClass(Parent root, Class<T> c) {
		Collection<Node> found = new ArrayList<>();
		findByClassRec(root, node -> c.isInstance(node), found);
		return (Collection<T>) found;
	}

	public static void findByClassRec(Parent node, Predicate<Node> p, Collection<Node> col) {
		if(p.test(node))
			col.add(node);
		for(Node child : node.getChildrenUnmodifiable()) {
			if(child instanceof Parent)
				findByClassRec((Parent)child, p, col);
			else
				if(p.test(child))
					col.add(child);
		}
	}

	// CSS Metadata

	public static void printCSS(Node n) {
		printCSS(n, "");
	}

	public static void printCSS(Node n, String filter) {
		for(CssMetaData style : n.getCssMetaData())
			if(style.getProperty().contains(filter))
				System.out.println(toString(style, n));
	}

	public static void printCSS(Collection<CssMetaData<? extends Styleable, ?>> styles, String filter) {
		for(CssMetaData style : styles)
			if(style.getProperty().contains(filter))
				System.out.println(style);
	}

	public static String toString(CssMetaData style, Node n) {
		StringBuilder res = new StringBuilder(String.format("%20s: %s", style.getProperty(), style.getInitialValue(n)));
		List<CssMetaData> properties = style.getSubProperties();
		if(properties != null && properties.size() > 0) {
			res.append(" - sub: [");
			for(CssMetaData prop : properties) {
				res.append(toString(prop, n));
				res.append(", ");
			}
			res.setLength(res.length() - 2);
			res.append(']');
		}
		return res.toString();
	}

}
