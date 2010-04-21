package scanner.patterns;

import scanner.FeaturePattern;

public class SwitchCasePattern extends FeaturePattern {

	private static String XPATH_QUERY = 
		"//src:switch/src:block/(src:case union .)/(cpp:ifdef union cpp:endif)";
	
	private static String REFACTORING_TYPE = "impossible switch/case";
	
	@Override
	public String getRefactoringType() {
		return REFACTORING_TYPE;
	}

	@Override
	public String getXpathQuery() {
		// TODO Auto-generated method stub
		return XPATH_QUERY;
	}

}
