package backend.storage;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import analyzer.FeatureSearcher;
import backend.PreprocessorNode;
import common.XMLTools;
import scanner.PreprocessorTree;

public class PreprocessorOccurrence {
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
		return this.docFileName.getAbsolutePath();
	}

	public PreprocessorNode[] getPrepNodes() {
		if (this.prepNodes[0].getNode() == null) {
			FeatureSearcher searcher = new FeatureSearcher();
			try {
				Document doc = XMLTools.parseDocument(this.docFileName);
				searcher.populateTree(doc, this.tree);
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

		return this.prepNodes;
	}

	public String getType() {
		return this.type;
	}

	public LinkedList<PreprocessorOccurrence> getRootPrepOccurrences() {
		return this.rootPrepOccurrence;
	}

	public LinkedList<PreprocessorOccurrence> getContainedOccurrences() {
		return this.containedOccurrences;
	}

	public void setContainedOccurrences(
			LinkedList<PreprocessorOccurrence> containedOccurrences) {
		this.containedOccurrences = containedOccurrences;
	}

	public Document getDocument() {
		if (this.prepNodes != null) {
			return this.prepNodes[0].getNode().getOwnerDocument();
		}
		return null;
	}

	public long getLinesOfCode() {
		return this.prepNodes[(this.prepNodes.length - 1)].getLineNumber()
				- this.prepNodes[0].getLineNumber();
	}
}
