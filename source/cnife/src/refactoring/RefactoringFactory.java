package refactoring;

import refactoring.actions.ClassIntroductionRefactoring;
import refactoring.actions.CompleteFunctionRefactoring;
import refactoring.actions.DirectivesRefactoring;
import refactoring.actions.HookRefactoring;

public class RefactoringFactory {

	public static RefactoringFactory instance() {
		return new RefactoringFactory();
	}

	public RefactoringAction getAction(String actionId) {
		if (actionId.equals("CompleteFunctionRefactoring")
				|| actionId.equals("CompletePubPrivRefactoring")) {
			return new CompleteFunctionRefactoring();
		} else if (actionId.equals("ClassIntroductionRefactoring")) {
			return new ClassIntroductionRefactoring();
		} else if (actionId.equals("DirectivesRefactoring")) {
			return new DirectivesRefactoring();
		} else if (actionId.equals("HookRefactoring")) {
			return new HookRefactoring();
		} else {
			return null;
		}
	}
}
