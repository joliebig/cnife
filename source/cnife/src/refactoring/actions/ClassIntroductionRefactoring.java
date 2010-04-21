package refactoring.actions;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import backend.PreprocessorNode;
import backend.storage.PreprocessorOccurrence;
import refactoring.RefactoringAction;
import refactoring.RefactoringDocument;

public class ClassIntroductionRefactoring extends RefactoringAction {

	protected void doRefactoring(Document from, RefactoringDocument pos,
			RefactoringDocument neg, PreprocessorOccurrence occ) {
		PreprocessorNode[] nodes = occ.getPrepNodes();
		boolean positive = false;
				
		if (!nodes[0].getNode().getNodeName().startsWith("cpp:ifn")) {
			moveNodes(nodes[0], nodes[1], from, pos, featureName);
			positive = true;
			pos.setModified(true);
		} else {
			neg.setModified(true);
			moveNodes(nodes[0], nodes[1], from, neg, negFeatureName);
		}
		if (nodes.length > 2 && positive) {
			moveNodes(nodes[1], nodes[2], from, neg, negFeatureName);
			neg.setModified(true);
		} else if (nodes.length > 2) {
			moveNodes(nodes[1], nodes[2], from, pos, featureName);
			pos.setModified(true);
		}
		
		for (int i = 0; i < nodes.length; i++) {
			nodes[i].getNode().getParentNode().removeChild(nodes[i].getNode());
		}

	}

	private void moveNodes(PreprocessorNode preprocessorNode,
			PreprocessorNode preprocessorNode2, Document from,
			RefactoringDocument target, String featureName) {
		Node firstChild = from.getFirstChild();
		target.adoptNode(firstChild);
		target.replaceChild(firstChild, target.getFirstChild());
		
		
	}

}
