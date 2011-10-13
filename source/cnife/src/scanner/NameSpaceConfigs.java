package scanner;

import java.util.Iterator;
import javax.xml.namespace.NamespaceContext;

public class NameSpaceConfigs implements NamespaceContext {
	public static final String CPP_PREFIX = "cpp";
	public static final String CPP_NS_URI = "http://www.sdml.info/srcML/cpp";

	public Iterator<String> getPrefixes(String namespaceURI) {
		throw new UnsupportedOperationException();
	}

	public String getPrefix(String namespaceURI) {
		throw new UnsupportedOperationException();
	}

	public String getNamespaceURI(String prefix) {
		if (prefix == null)
			throw new NullPointerException("Null prefix");
		if ("src".equals(prefix))
			return "http://www.sdml.info/srcML/src";
		if ("cpp".equals(prefix))
			return "http://www.sdml.info/srcML/cpp";
		if ("xml".equals(prefix)) {
			return "http://www.w3.org/XML/1998/namespace";
		}
		return "";
	}
}
