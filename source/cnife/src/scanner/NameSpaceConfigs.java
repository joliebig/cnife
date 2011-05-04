package scanner;

import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

public class NameSpaceConfigs implements NamespaceContext {
	public static final String CPP_PREFIX = "cpp";
	public static final String CPP_NS_URI = "http://www.sdml.info/srcML/cpp";
	public static final String SRC_PREFIX = "src";
	public static final String SRC_NS_URI = "http://www.sdml.info/srcML/src";
		
	@Override
	public Iterator<String> getPrefixes(String namespaceURI) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPrefix(String namespaceURI) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getNamespaceURI(String prefix) {
		if (prefix == null) {
			throw new NullPointerException("Null prefix");
		} else if (SRC_PREFIX.equals(prefix)) {
			return SRC_NS_URI;
		} else if (CPP_PREFIX.equals(prefix)) {
			return CPP_NS_URI;
		} else if ("xml".equals(prefix)) {
			return XMLConstants.XML_NS_URI;
		}
		return XMLConstants.NULL_NS_URI;
	}
}
