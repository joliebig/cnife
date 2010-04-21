package scanner.patterns;

import scanner.FeaturePattern;

public class ClassIntroductionPattern extends FeaturePattern {

	private static final String XPATH_QUERY = 
		"/src:unit/(cpp:ifdef union cpp:ifndef)/following-sibling::src:class/" +
		"following-sibling::cpp:endif/" +
		"(. union preceding-sibling::cpp:ifdef union preceding-sibling::cpp:ifndef)";
	private static final String REFACTORING = "ClassIntroductionRefactoring";
	
	@Override
	public String getRefactoringType() {
		return REFACTORING;
	}

	@Override
	public String getXpathQuery() {
		return XPATH_QUERY;
	}

}
