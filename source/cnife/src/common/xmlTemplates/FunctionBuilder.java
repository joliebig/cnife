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

import analyzer.QueryBuilder;

public class FunctionBuilder {

	private static String TEMPLATE_FILE = "./function_vorlage.xml";
	private static final String DECL_NODE_QUERY = "/src:unit/src:function";
	private static final String CALL_NODE_QUERY = "/src:unit/src:expr_stmt";
	private static final String NAME_NODES = 
		"/src:unit/" +
		"(src:function union src:expr_stmt/src:expr/src:call)" +
		"/src:name";
	private static final String TYPE_NODE = 
		"/src:unit/src:function/src:type/src:name";
	
	private static final String DECL_PARAMETERS = 
		"src:unit/src:function/src:parameterlist/src:param";
	
	private static final String CALL_PARAMETERS = 
		"src:unit/src:expr_stmt/src:expr/src:call/src:argument_list/src:argument";
	
	private Node functionDeclNode = null;
	private Node functionCallNode = null;
	private Document templateDoc = null;
	private int numberOfAttributes = 0;
	
	public FunctionBuilder() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		
		try {
			templateDoc = factory.newDocumentBuilder().parse(TEMPLATE_FILE);
			
			XPathExpression expr = QueryBuilder.instance().getExpression(DECL_NODE_QUERY);
			functionDeclNode = (Node) expr.evaluate(templateDoc, XPathConstants.NODE);
			
			expr = QueryBuilder.instance().getExpression(CALL_NODE_QUERY);
			functionCallNode = (Node) expr.evaluate(templateDoc, XPathConstants.NODE);
			
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setFunctionName(String name) {
		try {
			XPathExpression expr = QueryBuilder.instance().getExpression(NAME_NODES);
			NodeList list = (NodeList) expr.evaluate(templateDoc, XPathConstants.NODESET);
			
			for (int i = 0; i < list.getLength(); i++) {
				list.item(i).setTextContent(name.trim());
			}
			
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void setStatic() {
		try {
			XPathExpression expr = QueryBuilder.instance().getExpression(TYPE_NODE);
			Node typeName = (Node) expr.evaluate(templateDoc, XPathConstants.NODE);
			
			Node tmp = typeName.cloneNode(true);
			tmp.setTextContent("static");
			Text separator = templateDoc.createTextNode(" ");
			typeName.getParentNode().insertBefore(separator, typeName);
			typeName.getParentNode().insertBefore(tmp, separator);
			
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void appendParameter(Node type, Node name) {
		appendDeclParameter(type, name);
		appendCallParameter(name);
		numberOfAttributes++;
	}
	
	private void appendDeclParameter(Node type, Node name) {
		try {
			XPathExpression expr = 
				QueryBuilder.instance().getExpression(DECL_PARAMETERS);
			NodeList list = (NodeList) expr.evaluate(
					templateDoc, XPathConstants.NODESET);
			
			Node lastNode = list.item(list.getLength()-1);
			Node dupe = list.item(0).cloneNode(true);
			
			lastNode.getParentNode().insertBefore(dupe, lastNode.getNextSibling());
			if (numberOfAttributes > 0) {
				lastNode.getParentNode().insertBefore(
						templateDoc.createTextNode(", "), 
						lastNode.getNextSibling());
			}
			
			setType(type, dupe);
			setName(name, dupe);
//			System.out.println("");
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	private void setType(Node type, Node dupe) throws XPathExpressionException {
		XPathExpression expr = 
			QueryBuilder.instance().getExpression("./src:decl/src:type");
		Node curr = (Node)expr.evaluate(dupe, XPathConstants.NODE);
		Node typeNode = dupe.getOwnerDocument().importNode(type, true);
		curr.getParentNode().replaceChild(typeNode, curr);
		typeNode.appendChild(templateDoc.createTextNode("&")); //TODO: Check, ob & stimmt
	}
	
	private void setName(Node name, Node dupe) throws XPathExpressionException {
		XPathExpression expr = 
			QueryBuilder.instance().getExpression("./src:decl/src:name");
		Node curr = (Node)expr.evaluate(dupe, XPathConstants.NODE);
		Node typeNode = dupe.getOwnerDocument().importNode(name, true);
		curr.getParentNode().replaceChild(typeNode, curr);
	}

	private void appendCallParameter(Node name) {
		try {
			XPathExpression expr = 
				QueryBuilder.instance().getExpression(CALL_PARAMETERS);
			NodeList list = (NodeList) expr.evaluate(
					templateDoc, XPathConstants.NODESET);
			
			Node lastNode = list.item(list.getLength()-1);
			Node dupe = list.item(0).cloneNode(true);
			
			setParamName(name, dupe);
			
			lastNode.getParentNode().insertBefore(dupe, lastNode.getNextSibling());
			if (numberOfAttributes > 0) {
				lastNode.getParentNode().insertBefore(
						templateDoc.createTextNode(", "), 
						lastNode.getNextSibling());
			}
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void setParamName(Node name, Node dupe) throws XPathExpressionException {
		XPathExpression expr = 
			QueryBuilder.instance().getExpression("./src:expr/src:name");
		Node curr = (Node)expr.evaluate(dupe, XPathConstants.NODE);
		Node typeNode = dupe.getOwnerDocument().importNode(name, true);
		curr.getParentNode().replaceChild(typeNode, curr);
	}
	
	public Node buildDeclarationNode() {
		try {
			XPathExpression expr = 
				QueryBuilder.instance().getExpression(DECL_PARAMETERS);
			NodeList killList = (NodeList) expr.evaluate(
						templateDoc, XPathConstants.NODESET);
			Node toKill = killList.item(0);
			toKill.getParentNode().removeChild(toKill);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return functionDeclNode;
		
	}

	private boolean callInited = false;
	public Node buildCallNode() {
		if (!callInited) {
			try {
				XPathExpression expr = 
					QueryBuilder.instance().getExpression(CALL_PARAMETERS);
				Node toKill = ((NodeList) expr.evaluate(
						templateDoc, XPathConstants.NODESET)).item(0);
				if (toKill != null)
					toKill.getParentNode().removeChild(toKill);
			} catch (XPathExpressionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			callInited = true;
		}
		return functionCallNode;
		
	}
	
	
	//TODO DEBUG
	public static void main (String[] args) {
		FunctionBuilder fb = new FunctionBuilder();
		fb.setFunctionName("testFunction1");
		fb.setStatic();
		System.out.println(fb.buildDeclarationNode().getTextContent());
		System.out.println(fb.buildCallNode().getTextContent());
	}
}
