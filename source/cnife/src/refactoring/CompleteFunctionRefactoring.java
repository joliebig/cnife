package refactoring;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

import analyzer.QueryBuilder;
import backend.PreprocessorNode;
import backend.storage.PreprocessorOccurrence;

public class CompleteFunctionRefactoring {

	private static final String ENTRY_POINT = "/src:unit/src:class/src:block/src:comment";
	
	public void moveNodes(Document from, Document pos, Document neg, PreprocessorOccurrence occ, String featureName) {
		
		PreprocessorNode[] nodes = occ.getPrepNodes();
		boolean positive = false;
		if (nodes[0].getNode().getNodeName().startsWith("cpp:ifdef")) {
			refactor(nodes[0], nodes[1], from, pos, featureName);
			positive = true;
		} else {
			refactor(nodes[0], nodes[1], from, neg, "NOT_" + featureName);
		}
		if (nodes.length > 2 && positive) {
			refactor(nodes[1], nodes[2], from, neg, "NOT_" + featureName);
		} else if (nodes.length > 2) {
			refactor(nodes[1], nodes[2], from, pos, featureName);
		}
		
		for (int i = 0; i < nodes.length; i++) {
			nodes[i].getNode().getParentNode().removeChild(nodes[i].getNode());
		}
		
	}

	private void refactor(PreprocessorNode start,
			PreprocessorNode end, Document from, Document target, 
			String featureName) {
		
		
		DocumentFragment frag = from.createDocumentFragment();
		frag.appendChild(start.getNode().getParentNode().cloneNode(false));
		frag.getFirstChild().setTextContent("\n" + frag.getFirstChild().getNodeName() + ":\n");
		Node current = start.getNode().getNextSibling();
		while (current != end.getNode()) {
			Node next = current.getNextSibling();
			frag.getFirstChild().appendChild(current);
			current = next;
		}
		
		try {
			XPathExpression expr = QueryBuilder.instance().getExpression(ENTRY_POINT);
			Node entryPoint = (Node) expr.evaluate(target, XPathConstants.NODE);
			target.adoptNode(frag);
			entryPoint.getParentNode().insertBefore(frag, entryPoint.getNextSibling());
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
