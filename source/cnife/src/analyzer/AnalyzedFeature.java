package analyzer;

import backend.storage.IdentifiedFeature;
import java.util.HashMap;
import java.util.LinkedList;
import org.w3c.dom.Node;

public class AnalyzedFeature extends IdentifiedFeature {
	private HashMap<String, HashMap<Node, LinkedList<CriticalOccurrence>>> affectedFiles;

	public void setAffectedFiles(
			HashMap<String, HashMap<Node, LinkedList<CriticalOccurrence>>> affectedFiles) {
		this.affectedFiles = affectedFiles;
	}

	public HashMap<String, HashMap<Node, LinkedList<CriticalOccurrence>>> getAffectedFiles() {
		return this.affectedFiles;
	}
}
