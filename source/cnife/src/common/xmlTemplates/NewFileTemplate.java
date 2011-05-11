package common.xmlTemplates;

import analyzer.QueryBuilder;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import refactoring.RefactoringDocument;

public class NewFileTemplate extends RefactoringDocument {
	private static String TEMPLATE_FILE = "./vorlage.h.xml";

	private NewFileTemplate() {
		super(null);
	}

	public static NewFileTemplate instantiate() throws SAXException,
			IOException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);

		NewFileTemplate templ = new NewFileTemplate();
		templ.xmlDoc = factory.newDocumentBuilder().parse(TEMPLATE_FILE);

		return templ;
	}

	public void setClassName(String className) throws XPathExpressionException {
		XPathExpression expr = QueryBuilder.instance().getExpression(
				CLASS_NODE_QUERY);
		Node result = (Node) expr.evaluate(this.xmlDoc, XPathConstants.NODE);
		result.setTextContent(className);
	}

	public void setClassName(Document doc) throws XPathExpressionException {
		XPathExpression expr = QueryBuilder.instance().getExpression(
				CLASS_NODE_QUERY);
		Node result = (Node) expr.evaluate(doc, XPathConstants.NODE);
		setClassName(result.getTextContent());
	}

	public static void main(String[] args) throws SAXException, IOException,
			ParserConfigurationException, XPathExpressionException {
		NewFileTemplate templ = instantiate();
		templ.setClassName("Test");
		System.out.println(templ.xmlDoc.getFirstChild().getTextContent());
	}
}
