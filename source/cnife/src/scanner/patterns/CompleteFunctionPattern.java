package scanner.patterns;

import scanner.FeaturePattern;


public class CompleteFunctionPattern extends FeaturePattern {

	private static final String COMPLETE_FUNCTION_REFACTORING = "CompleteFunctionRefactoring";
	private static final String COMPLETE_FUNCTIONS =
		"/src:unit/src:class/src:block/((src:public union src:private)" +
		"/(cpp:if union cpp:ifndef union cpp:ifdef) " +
		"union " +
		"((. union src:public union src:private)/(cpp:else union cpp:endif)))";
	
	
	@Override
	public String getRefactoringType() {
		return COMPLETE_FUNCTION_REFACTORING;
	}
	@Override
	public String getXpathQuery() {
		// TODO Auto-generated method stub
		return COMPLETE_FUNCTIONS;
	}
	
	
		
}
