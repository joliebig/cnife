package scanner.patterns;

import scanner.FeaturePattern;

public class CompleteFunctionPattern extends FeaturePattern {
	private static final String COMPLETE_FUNCTION_REFACTORING = "CompleteFunctionRefactoring";
	private static final String COMPLETE_FUNCTIONS = "/src:unit/src:class/src:block/((src:public union src:private)/(cpp:if union cpp:ifndef union cpp:ifdef) union ((. union src:public union src:private)/(cpp:else union cpp:endif)))";

	public String getRefactoringType() {
		return CompleteFunctionPattern.COMPLETE_FUNCTION_REFACTORING;
	}

	public String getXpathQuery() {
		return CompleteFunctionPattern.COMPLETE_FUNCTIONS;
	}
}
