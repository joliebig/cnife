package scanner.patterns;

import scanner.FeaturePattern;

public class SwitchCasePattern extends FeaturePattern {
	private static String XPATH_QUERY = "//src:switch/src:block/(src:case union .)/(cpp:ifdef union cpp:endif)";

	private static String REFACTORING_TYPE = "impossible switch/case";

	public String getRefactoringType() {
		return REFACTORING_TYPE;
	}

	public String getXpathQuery() {
		return XPATH_QUERY;
	}
}
