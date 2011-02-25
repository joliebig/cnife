package scanner;

import org.w3c.dom.Node;

import backend.storage.PreprocessorOccurrence;

public class CheckLogics {
	
	
	
	
	public String getFeatureName(PreprocessorOccurrence pOccurrence) {
		Node node = pOccurrence.getPrepNodes()[0].getNode();
		Node nameNode = node.getLastChild();
		String name = computeName(nameNode.getTextContent());
		return name; 
		
	}



	private String computeName(String textContent) {
		String newTextContent = null;
		
		if (textContent.matches(".*[\\||!|\\&|=|(defined)]+.*")) {
			newTextContent = textContent.replace("||", "_OR_");
			newTextContent = newTextContent.replace("&&", "_AND_");
			newTextContent = newTextContent.replace("defined", "");
			newTextContent = newTextContent.replace("!", "NOT_");
			newTextContent = newTextContent.replace("==", "_EQUALS_");
			newTextContent = newTextContent.replace("<", "_lt_");
			newTextContent = newTextContent.replace(">", "_gt_");
			newTextContent = newTextContent.replace(" ", "");
			newTextContent = "(" + newTextContent + ")";
		} else {
			newTextContent = textContent;
		}
		
		return newTextContent;
	}
}
