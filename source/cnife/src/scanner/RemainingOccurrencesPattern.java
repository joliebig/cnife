package scanner;

import java.util.LinkedList;

import org.w3c.dom.Document;

import backend.storage.PreprocessorOccurrence;

public class RemainingOccurrencesPattern extends FeaturePattern {

	private static final String TYPE_UNKNOWN = "unknown";
	
	@Override
	public LinkedList<PreprocessorOccurrence> checkDoc(Document srcDoc,
			PreprocessorTree completeTree) {
		completeTree.removeMarkedOccurrences();
		return buildOccurrences(completeTree.getOccurrenceSequence(), getRefactoringType());
	}

	@Override
	public String getRefactoringType() {
		return TYPE_UNKNOWN;
	}

	@Override
	public String getXpathQuery() {
		return "";
	}

}
