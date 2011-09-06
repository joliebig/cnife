package common.xmlTemplates;

import java.io.IOException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import common.QueryBuilder;

public class FunctionBuilder {
	private static String TEMPLATE_FILE = "./function_vorlage.xml";
/*	private static final String DECL_NODE_QUERY = "/src:unit/src:function";
	private static final String CALL_NODE_QUERY = "/src:unit/src:expr_stmt";
	private static final String NAME_NODES = "/src:unit/(src:function union src:expr_stmt/src:expr/src:call)/src:name";
	private static final String TYPE_NODE = "/src:unit/src:function/src:type/src:name";
	private static final String DECL_PARAMETERS = "src:unit/src:function/src:parameterlist/src:param";
	private static final String CALL_PARAMETERS = "src:unit/src:expr_stmt/src:expr/src:call/src:argument_list/src:argument";*/
	private Node functionDeclNode = null;
	private Node functionCallNode = null;
	private Document templateDoc = null;
	private int numberOfAttributes = 0;

	private boolean callInited = false;

	public FunctionBuilder() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			this.templateDoc = factory.newDocumentBuilder()
					.parse(TEMPLATE_FILE);

			XPathExpression expr = QueryBuilder.instance().getExpression(
					"/src:unit/src:function");
			this.functionDeclNode = ((Node) expr.evaluate(this.templateDoc,
					XPathConstants.NODE));

			expr = QueryBuilder.instance().getExpression(
					"/src:unit/src:expr_stmt");
			this.functionCallNode = ((Node) expr.evaluate(this.templateDoc,
					XPathConstants.NODE));
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	public void setFunctionName(String name) {
		try {
			XPathExpression expr = QueryBuilder
					.instance()
					.getExpression(
							"/src:unit/(src:function union src:expr_stmt/src:expr/src:call)/src:name");
			NodeList list = (NodeList) expr.evaluate(this.templateDoc,
					XPathConstants.NODESET);

			for (int i = 0; i < list.getLength(); i++) {
				list.item(i).setTextContent(name.trim());
			}
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
	}

	public void setStatic() {
		try {
			XPathExpression expr = QueryBuilder.instance().getExpression(
					"/src:unit/src:function/src:type/src:name");
			Node typeName = (Node) expr.evaluate(this.templateDoc,
					XPathConstants.NODE);

			Node tmp = typeName.cloneNode(true);
			tmp.setTextContent("static");
			Text separator = this.templateDoc.createTextNode(" ");
			typeName.getParentNode().insertBefore(separator, typeName);
			typeName.getParentNode().insertBefore(tmp, separator);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
	}

	public void appendParameter(Node type, Node name) {
		appendDeclParameter(type, name);
		appendCallParameter(name);
		this.numberOfAttributes += 1;
	}

	private void appendDeclParameter(Node type, Node name) {
		try {
			XPathExpression expr = QueryBuilder.instance().getExpression(
					"src:unit/src:function/src:parameterlist/src:param");
			NodeList list = (NodeList) expr.evaluate(this.templateDoc,
					XPathConstants.NODESET);

			Node lastNode = list.item(list.getLength() - 1);
			Node dupe = list.item(0).cloneNode(true);

			lastNode.getParentNode().insertBefore(dupe,
					lastNode.getNextSibling());
			if (this.numberOfAttributes > 0) {
				lastNode.getParentNode().insertBefore(
						this.templateDoc.createTextNode(", "),
						lastNode.getNextSibling());
			}

			setType(type, dupe);
			setName(name, dupe);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
	}

	private void setType(Node type, Node dupe) throws XPathExpressionException {
		XPathExpression expr = QueryBuilder.instance().getExpression(
				"./src:decl/src:type");
		Node curr = (Node) expr.evaluate(dupe, XPathConstants.NODE);
		Node typeNode = dupe.getOwnerDocument().importNode(type, true);
		curr.getParentNode().replaceChild(typeNode, curr);
		typeNode.appendChild(this.templateDoc.createTextNode("&"));
	}

	private void setName(Node name, Node dupe) throws XPathExpressionException {
		XPathExpression expr = QueryBuilder.instance().getExpression(
				"./src:decl/src:name");
		Node curr = (Node) expr.evaluate(dupe, XPathConstants.NODE);
		Node typeNode = dupe.getOwnerDocument().importNode(name, true);
		curr.getParentNode().replaceChild(typeNode, curr);
	}

	private void appendCallParameter(Node name) {
		try {
			XPathExpression expr = QueryBuilder
					.instance()
					.getExpression(
							"src:unit/src:expr_stmt/src:expr/src:call/src:argument_list/src:argument");
			NodeList list = (NodeList) expr.evaluate(this.templateDoc,
					XPathConstants.NODESET);

			Node lastNode = list.item(list.getLength() - 1);
			Node dupe = list.item(0).cloneNode(true);

			setParamName(name, dupe);

			lastNode.getParentNode().insertBefore(dupe,
					lastNode.getNextSibling());
			if (this.numberOfAttributes > 0)
				lastNode.getParentNode().insertBefore(
						this.templateDoc.createTextNode(", "),
						lastNode.getNextSibling());
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
	}

	private void setParamName(Node name, Node dupe)
			throws XPathExpressionException {
		XPathExpression expr = QueryBuilder.instance().getExpression(
				"./src:expr/src:name");
		Node curr = (Node) expr.evaluate(dupe, XPathConstants.NODE);
		Node typeNode = dupe.getOwnerDocument().importNode(name, true);
		curr.getParentNode().replaceChild(typeNode, curr);
	}

	public Node buildDeclarationNode() {
		try {
			XPathExpression expr = QueryBuilder.instance().getExpression(
					"src:unit/src:function/src:parameterlist/src:param");
			NodeList killList = (NodeList) expr.evaluate(this.templateDoc,
					XPathConstants.NODESET);
			Node toKill = killList.item(0);
			toKill.getParentNode().removeChild(toKill);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		return this.functionDeclNode;
	}

	public Node buildCallNode() {
		if (!this.callInited) {
			try {
				XPathExpression expr = QueryBuilder
						.instance()
						.getExpression(
								"src:unit/src:expr_stmt/src:expr/src:call/src:argument_list/src:argument");
				Node toKill = ((NodeList) expr.evaluate(this.templateDoc,
						XPathConstants.NODESET)).item(0);
				if (toKill != null)
					toKill.getParentNode().removeChild(toKill);
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}
			this.callInited = true;
		}
		return this.functionCallNode;
	}

	public static void main(String[] args) {
		FunctionBuilder fb = new FunctionBuilder();
		fb.setFunctionName("testFunction1");
		fb.setStatic();
		System.out.println(fb.buildDeclarationNode().getTextContent());
		System.out.println(fb.buildCallNode().getTextContent());
	}
}
