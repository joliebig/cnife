package refactoring;

import backend.storage.PreprocessorOccurrence;
import org.w3c.dom.Document;

public abstract class RefactoringAction {
	protected String featureName;
	protected String negFeatureName;

	public void doRefactoring(Document doc, RefactoringDocument pos,
			RefactoringDocument neg, PreprocessorOccurrence occ, String name) {
		this.featureName = name;
		this.negFeatureName = negateFeatureName(this.featureName);
		pos.setFeatureName(this.featureName);
		neg.setFeatureName(this.negFeatureName);
		doRefactoring(doc, pos, neg, occ);
	}

	protected abstract void doRefactoring(Document paramDocument,
			RefactoringDocument paramRefactoringDocument1,
			RefactoringDocument paramRefactoringDocument2,
			PreprocessorOccurrence paramPreprocessorOccurrence);

	private String negateFeatureName(String featureName) {
		String negFeatureName = "NOT_" + featureName;
		return negFeatureName;
	}
}
