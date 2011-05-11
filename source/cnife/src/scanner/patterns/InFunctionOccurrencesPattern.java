package scanner.patterns;

import scanner.FeaturePattern;

public class InFunctionOccurrencesPattern extends FeaturePattern {
	private static final String XPATH_QUERY = "((/src:unit union (/src:unit/src:class/src:block/(src:private union src:public)))/(src:constructor union src:function)/descendant::src:block/(cpp:ifdef union cpp:ifndef union cpp:if)/(following-sibling::cpp:endif)/(. union preceding-sibling::cpp:else union preceding-sibling::cpp:ifdef union preceding-sibling::cpp:ifndef union preceding-sibling::cpp:if))";
	private static final String REFACTORING_TYPE = "HookRefactoring";

	public String getRefactoringType() {
		return InFunctionOccurrencesPattern.REFACTORING_TYPE;
	}

	public String getXpathQuery() {
		return InFunctionOccurrencesPattern.XPATH_QUERY;
	}
}
