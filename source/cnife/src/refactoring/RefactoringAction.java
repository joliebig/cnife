package refactoring;

import org.w3c.dom.Document;

import backend.storage.PreprocessorOccurrence;

public abstract class RefactoringAction {

	protected String featureName;
	protected String negFeatureName;
	
	public void doRefactoring(Document doc, RefactoringDocument pos, RefactoringDocument neg,
			PreprocessorOccurrence occ, String name) {
		this.featureName = name;
		negFeatureName = negateFeatureName(featureName);
		pos.setFeatureName(featureName);
		neg.setFeatureName(negFeatureName);
		doRefactoring(doc, pos, neg, occ);
	}

	protected abstract void doRefactoring(Document from, RefactoringDocument pos, RefactoringDocument neg, PreprocessorOccurrence occ);

	private String negateFeatureName(String featureName) {
		String negFeatureName = 
			featureName.toLowerCase().startsWith("not_") ?
					featureName.substring("NOT_".length()) :
						"NOT_" + featureName;
		return negFeatureName;
	}

}
