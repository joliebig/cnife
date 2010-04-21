package analyzer;

import java.util.HashMap;
import java.util.LinkedList;

import org.w3c.dom.Node;

import backend.storage.IdentifiedFeature;

public class AnalyzedFeature extends IdentifiedFeature {
	private HashMap<String, HashMap<Node, LinkedList<CriticalOccurrence>>> affectedFiles;

	public void setAffectedFiles(
			HashMap<String, HashMap<Node, LinkedList<CriticalOccurrence>>> affectedFiles) {
		this.affectedFiles = affectedFiles;
	}

	public HashMap<String, HashMap<Node, LinkedList<CriticalOccurrence>>> getAffectedFiles() {
		return affectedFiles;
	}

}
