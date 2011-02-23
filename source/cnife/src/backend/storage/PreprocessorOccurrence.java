package backend.storage;

import java.io.File;
import java.util.LinkedList;

import org.w3c.dom.Document;

import backend.PreprocessorNode;


public class PreprocessorOccurrence {
	
	//maximal 3
	private PreprocessorNode[] prepNodes;
	private File docFileName;
	private String type;
	private LinkedList<PreprocessorOccurrence> rootPrepOccurrence;
	
	public void setPrepNodes(PreprocessorNode[] prepNodes) {
		this.prepNodes = prepNodes;
	}

	public void setDocFileName(File docFileName) {
		this.docFileName = docFileName;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDocFileName() {
		return docFileName.getAbsolutePath();
	}
	
	public PreprocessorNode[] getPrepNodes() {
		return prepNodes;
	}
	
	public String getType() {
		return type;
	}
	
	public LinkedList<PreprocessorOccurrence> getRootPrepOccurrences() {
		return rootPrepOccurrence;
	}
	
	public Document getDocument() {
		if (prepNodes != null) {
			return prepNodes[0].getNode().getOwnerDocument();
		} else {
			return null;
		}
	}
	
	public long getLinesOfCode() {
		return prepNodes[prepNodes.length-1].getLineNumber() 
				- prepNodes[0].getLineNumber();
	}
}
