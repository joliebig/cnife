package scanner.patterns;

import scanner.FeaturePattern;

public class CompletePrivatePublicBlock extends FeaturePattern {
	private static final String COMPLETE_PUB_PRIV_REFACTORING = "CompletePubPrivRefactoring";
	private static final String COMPLETE_PUB_PRIV_BLOCK = "/src:unit/src:class/src:block/(((. union src:private[@type=\"default\"])/(cpp:ifdef union cpp:ifndef union cpp:if)) union ((. union src:public union src:private)/(cpp:else union cpp:endif)))";

	public String getRefactoringType() {
		return CompletePrivatePublicBlock.COMPLETE_PUB_PRIV_REFACTORING;
	}

	public String getXpathQuery() {
		return CompletePrivatePublicBlock.COMPLETE_PUB_PRIV_BLOCK;
	}
}
