package scanner.patterns;

import scanner.FeaturePattern;

public class CompleteFunctionPatternInline extends FeaturePattern {
	private static final String COMPLETE_FUNCTION_REFACTORING_INLINE = "CompleteFunctionRefactoringInline";
	private static final String COMPLETE_FUNCTIONS_INLINE = "/src:unit/(cpp:ifdef union cpp:ifndef union cpp:if)/following-sibling::src:function/following-sibling::cpp:endif/(. union preceding-sibling::cpp:ifdef union preceding-sibling::cpp:ifndef union preceding-sibling::cpp:if)";

	public String getRefactoringType() {
		return CompleteFunctionPatternInline.COMPLETE_FUNCTION_REFACTORING_INLINE;
	}

	public String getXpathQuery() {
		return CompleteFunctionPatternInline.COMPLETE_FUNCTIONS_INLINE;
	}
}
