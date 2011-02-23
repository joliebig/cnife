package scanner.patterns;

import scanner.FeaturePattern;

public class InFunctionOccurrencesPattern extends FeaturePattern {
	private static final String XPATH_QUERY =  
    "(/src:unit/src:class/src:block/(src:private union src:public)/" +
    "src:function/descendant::src:block/(cpp:ifdef union cpp:ifndef)/" +
    "(following-sibling::cpp:endif)/" +
    "(. union preceding-sibling::cpp:else union preceding-sibling::cpp:ifdef union preceding-sibling::cpp:ifndef))";
	
	private static final String REFACTORING_TYPE =
		"HookRefactoring";
	
	@Override
	public String getRefactoringType() {
		return REFACTORING_TYPE;
	}

	@Override
	public String getXpathQuery() {
		return XPATH_QUERY;
	}
}
