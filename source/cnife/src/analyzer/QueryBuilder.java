package analyzer;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import scanner.NameSpaceConfigs;

/**
 * Die QueryBuilder-Klasse ist ein Singleton, das die Konstruktion von
 * XPath-Queries vereinfacht, indem der Namespace und etwaige
 * Factory-Methoden innerhalb der Klasse benutzt werden.
 * 
 * @author Christopher Resch
 * @version 0.2
 *
 */
public class QueryBuilder {

	/**
	 * Singleton-Instanz
	 */
	private static QueryBuilder qb = null;
	
	/**
	 * Verknüpfung zum Environment für XPath
	 */
	private XPath xpath = null;
	
	/**
	 * Singleton instance
	 * @return liefert die Instanz des QueryBuilders
	 */
	public static QueryBuilder instance() {
		if (qb == null) {
			qb = makeInstance();
		}
		return qb;
	}
	
	/**
	 * threadsafe Instantiierungsmethode für das Singleton-Pattern
	 * @return liefert eine neue Instanz des QueryBuilders
	 */
	private static synchronized QueryBuilder makeInstance() {
		return new QueryBuilder();
	}
	
	/**
	 * privater Konstruktor des Singleton-Patterns
	 */
	private QueryBuilder() {
		XPathFactory xpFactory = XPathFactory.newInstance();
		xpath = xpFactory.newXPath();
		xpath.setNamespaceContext(new NameSpaceConfigs());
		

	}
	
	/**
	 * liefert eine fertige kompilierte <code>java.xml.xpath.XPathExpression</code>
	 * @param query	ein XpathQuery-String
	 * @return	eine fertige XPathExpression
	 * @throws XPathExpressionException
	 * 		wird geworfen, wenn die Expression nicht kompilierbar ist
	 */
	public XPathExpression getExpression(String query) throws XPathExpressionException {
		return xpath.compile(query);
	}
}
