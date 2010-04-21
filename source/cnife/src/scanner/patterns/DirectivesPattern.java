package scanner.patterns;

import scanner.FeaturePattern;

public class DirectivesPattern extends FeaturePattern {

	private static final String REFACTORING = "DirectivesRefactoring"; 
	private static final String XPATH_QUERY = "/src:unit/(cpp:ifdef union cpp:ifndef)/following-sibling::src:class/preceding-sibling::cpp:endif/((. union preceding-sibling::cpp:ifdef union preceding-sibling::cpp:ifndef union preceding-sibling::else))";
	
	@Override
	public String getRefactoringType() {
		return REFACTORING;
	}

	@Override
	public String getXpathQuery() {
		
		return XPATH_QUERY;
	}

}
