package scanner;

import backend.storage.PreprocessorOccurrence;
import java.util.LinkedList;
import org.w3c.dom.Document;

public class RemainingOccurrencesPattern extends FeaturePattern {
	private static final String TYPE_UNKNOWN = "unknown";

	public LinkedList<PreprocessorOccurrence> checkDoc(Document srcDoc,
			PreprocessorTree completeTree) {
		completeTree.removeMarkedOccurrences();
		return buildOccurrences(completeTree.getOccurrenceSequence(),
				getRefactoringType());
	}

	public String getRefactoringType() {
		return RemainingOccurrencesPattern.TYPE_UNKNOWN;
	}

	public String getXpathQuery() {
		return "";
	}
}
