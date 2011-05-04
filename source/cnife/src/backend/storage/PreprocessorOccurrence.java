package backend.storage;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import scanner.FeatureSearcher;
import scanner.PreprocessorTree;
import backend.PreprocessorNode;


public class PreprocessorOccurrence {
	
	//maximal 3
	private PreprocessorNode[] prepNodes;
	private File docFileName;
	private String type;
	private LinkedList<PreprocessorOccurrence> rootPrepOccurrence;
	private LinkedList<PreprocessorOccurrence> containedOccurrences;
	private PreprocessorTree tree;
	
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
		if (prepNodes[0].getNode() == null) {
			FeatureSearcher searcher = new FeatureSearcher();
			try {
				Document doc = searcher.parseDocument(docFileName);
				searcher.populateTree(doc, tree);
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}
			
		}
		return prepNodes;
	}
	
	public String getType() {
		return type;
	}
	
	public LinkedList<PreprocessorOccurrence> getRootPrepOccurrences() {
		return rootPrepOccurrence;
	}
	
	public LinkedList<PreprocessorOccurrence> getContainedOccurrences() {
		return containedOccurrences;
	}
	
	public void setContainedOccurrences(LinkedList<PreprocessorOccurrence> containedOccurrences) {
		this.containedOccurrences = containedOccurrences;
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
