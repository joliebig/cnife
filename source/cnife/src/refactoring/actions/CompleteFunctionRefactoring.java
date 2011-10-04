package refactoring.actions;

import backend.PreprocessorNode;
import backend.storage.PreprocessorOccurrence;
import common.NodeTools;
import common.QueryBuilder;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import refactoring.RefactoringAction;
import refactoring.RefactoringDocument;

public class CompleteFunctionRefactoring extends RefactoringAction {

	protected void doRefactoring(Document from, RefactoringDocument pos,
			RefactoringDocument neg, PreprocessorOccurrence occ) {
		PreprocessorNode[] nodes = occ.getPrepNodes();
		boolean positive = false;
		if (!nodes[0].getNode().getNodeName().startsWith("cpp:ifn")) {
			moveNodes(nodes[0], nodes[1], from, pos, this.featureName);
			positive = true;
			pos.setModified(true);
		} else {
			moveNodes(nodes[0], nodes[1], from, neg, this.negFeatureName);
			neg.setModified(true);
		}
		if ((nodes.length > 2) && (positive)) {
			moveNodes(nodes[1], nodes[2], from, neg, this.negFeatureName);
			neg.setModified(true);
		} else if (nodes.length > 2) {
			moveNodes(nodes[1], nodes[2], from, pos, this.featureName);
			pos.setModified(true);
		}

		for (int i = 0; i < nodes.length; i++)
			nodes[i].getNode().getParentNode().removeChild(nodes[i].getNode());
	}

	private void moveNodes(PreprocessorNode start, PreprocessorNode end,
			Document from, Document target, String featureName) {
		if (NodeTools.haveSameParents(start.getNode(), end.getNode())) {
			DocumentFragment frag = from.createDocumentFragment();
			extractNodes(start, end, frag);
			try {
				XPathExpression expr = QueryBuilder
						.instance()
						.getExpression(
								"/src:unit/src:class/src:block/src:comment[text()=\"//---refactored functions\"]");
				Node entryPoint = (Node) expr.evaluate(target,
						XPathConstants.NODE);
				NodeList childs = frag.getChildNodes();
				for (int i = 0; i < childs.getLength(); i++) {
					System.out.println("clone Node 1");
					Node child = childs.item(i).cloneNode(true);
					target.adoptNode(child);
					entryPoint.getParentNode().insertBefore(child,
							entryPoint.getNextSibling());
				}
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}
		} else if (NodeTools.getDepth(start.getNode()) == NodeTools
				.getDepth(end.getNode())) {
			DocumentFragment frag = from.createDocumentFragment();

			extractNodes(start, end, frag);
			try {
				XPathExpression expr = QueryBuilder
						.instance()
						.getExpression(
								"/src:unit/src:class/src:block/src:comment[text()=\"//---refactored functions\"]");
				Node entryPoint = (Node) expr.evaluate(target,
						XPathConstants.NODE);

				NodeList childs = frag.getChildNodes();
				for (int i = 0; i < childs.getLength(); i++) {
					System.out.println("clone node 2");
					Node child = childs.item(i).cloneNode(true);
					target.adoptNode(child);
					entryPoint.getParentNode().insertBefore(child,
							entryPoint.getNextSibling());
				}
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}
		} else {
			System.out.print("Knotenproblem: " + featureName + "\n"
					+ start.getLineNumber());
		}
	}

	private void extractNodes(PreprocessorNode start, PreprocessorNode end,
			DocumentFragment frag) {
		Node startBlock = start.getNode().getParentNode();
		Node currentBlock = startBlock;
		Node current = start.getNode().getNextSibling();
		while (current != end.getNode()) {
			frag.appendChild(currentBlock.cloneNode(false));
			frag.getLastChild().setTextContent(
					"\n" + frag.getLastChild().getNodeName() + ":\n");
			while ((current != end.getNode()) && (current != null)) {
				Node next = current.getNextSibling();
				frag.getLastChild().appendChild(current);
				current = next;
			}
			currentBlock = currentBlock.getNextSibling();
			if ((current != end.getNode()) && (currentBlock.hasChildNodes())) {
				current = currentBlock.getFirstChild();
			}
		}

		current = current.getNextSibling();
		while (current != null) {
			startBlock.appendChild(current);
			current = current.getNextSibling();
		}
	}
}
