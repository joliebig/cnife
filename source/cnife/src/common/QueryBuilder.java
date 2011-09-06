package common;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import scanner.NameSpaceConfigs;

public class QueryBuilder {
	private static QueryBuilder qb = null;

	private XPath xpath = null;

	public static QueryBuilder instance() {
		if (qb == null) {
			qb = makeInstance();
		}
		return qb;
	}

	private static synchronized QueryBuilder makeInstance() {
		return new QueryBuilder();
	}

	private QueryBuilder() {
		XPathFactory xpFactory = XPathFactory.newInstance();
		this.xpath = xpFactory.newXPath();
		this.xpath.setNamespaceContext(new NameSpaceConfigs());
	}

	public XPathExpression getExpression(String query)
			throws XPathExpressionException {
		return this.xpath.compile(query);
	}
}
