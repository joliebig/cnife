package scanner.patterns;

import scanner.FeaturePattern;

public class CompletePrivatePublicBlock extends FeaturePattern {

	private static final String COMPLETE_PUB_PRIV_REFACTORING = "CompletePubPrivRefactoring";
	private static final String COMPLETE_PUB_PRIV_BLOCK =
		"/src:unit/src:class/src:block/" +
		"(((. union src:private[@type=\"default\"])/" +
		"(cpp:ifdef union cpp:ifndef union cpp:if))" +
		" union " +
		"((. union src:public union src:private)/(cpp:else union cpp:endif)))";
	@Override
	public String getRefactoringType() {
		return COMPLETE_PUB_PRIV_REFACTORING;
	}

	@Override
	public String getXpathQuery() {
		return COMPLETE_PUB_PRIV_BLOCK;
	}

}
