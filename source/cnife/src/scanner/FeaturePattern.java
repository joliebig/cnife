package scanner;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import analyzer.QueryBuilder;
import backend.PreprocessorNode;
import backend.storage.PreprocessorOccurrence;


public abstract class FeaturePattern {
	
	
	public abstract String getXpathQuery();

	public abstract String getRefactoringType();

	public boolean verify(
			PreprocessorTree patternTree, 
			PreprocessorTree completeTree) {
		
		completeTree.calculateMatches(patternTree);
		
		return true;
	}
	
	public LinkedList<PreprocessorOccurrence> checkDoc (
			Document srcDoc,  PreprocessorTree completeTree) {
		LinkedList<PreprocessorOccurrence> occs = null;
		
		try {
			occs = doXPath (
					srcDoc, 
					completeTree, 
					getXpathQuery(), 
					getRefactoringType(), 
					true);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (occs == null || occs.isEmpty()) {
			return new LinkedList<PreprocessorOccurrence>();
		} else {
			return occs;
		}
	}

	protected PreprocessorTree constructTree(NodeList result, PreprocessorTree completeTree) {
		PreprocessorTree tree = new PreprocessorTree();
		
		for (int i = 0; i < result.getLength(); i++) {
			PreprocessorNode node = new PreprocessorNode();
			
			node.setNode(result.item(i));
			node.setType(
					result.item(i).getNodeName().substring(
							NameSpaceConfigs.CPP_PREFIX.length() + 1));
			
			tree.addAgainst(node, completeTree);
		}
		
		/*/TODO DEBUG
		List<PreprocessorNode> list = tree.flattenTree();
		for (PreprocessorNode node : list) {
			System.out.println(node.getType());
		}
		//TODO DEBUG END */
		return tree;
	}

	protected LinkedList<PreprocessorOccurrence> buildOccurrences(
			List<PreprocessorNode> occurrenceSequence, String type) {
		LinkedList<PreprocessorOccurrence> result = 
			new LinkedList<PreprocessorOccurrence>();
		Iterator<PreprocessorNode> nodeIterator = occurrenceSequence.iterator();
		while (nodeIterator.hasNext()) {
			PreprocessorOccurrence occ = new PreprocessorOccurrence();
			
			PreprocessorNode ifNode = nodeIterator.next();
			PreprocessorNode elseNode = null;
			PreprocessorNode endNode = null;
			
			PreprocessorNode tmp = nodeIterator.next();
			if (tmp.getType().equals("else")) {
				elseNode = tmp;
				endNode = nodeIterator.next();
			} else {
				endNode = tmp;
			}
			if (elseNode == null) {
				occ.setPrepNodes(new PreprocessorNode[]{ifNode, endNode});
			} else {
				occ.setPrepNodes(new PreprocessorNode[]{ifNode, elseNode, endNode});
			}
			occ.setType(type);
			result.add(occ);
		}
		
		return result;
	}

	protected LinkedList<PreprocessorOccurrence> doXPath(
			Document srcDoc, PreprocessorTree completeTree, String xpath,
			String patternType, boolean doVerification) 
	throws XPathExpressionException {
				LinkedList<PreprocessorOccurrence> occs;
				XPathExpression expr = QueryBuilder.instance().getExpression(xpath);
				NodeList result = (NodeList) expr.evaluate(
						srcDoc, XPathConstants.NODESET);
				//System.out.println(result.getLength());
				PreprocessorTree tree = constructTree(result, completeTree);
				
				verify(tree, completeTree);
				
				occs = buildOccurrences(tree.getOccurrenceSequence(), patternType);
				return occs;
			}
}
