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
		if ((actionId.equals("CompleteFunctionRefactoring"))
				|| (actionId.equals("CompletePubPrivRefactoring"))
				|| (actionId.equals("CompleteFunctionRefactoringInline")))
			return new CompleteFunctionRefactoring();
		if (actionId.equals("ClassIntroductionRefactoring"))
			return new ClassIntroductionRefactoring();
		if (actionId.equals("DirectivesRefactoring"))
			return new DirectivesRefactoring();
		if (actionId.equals("HookRefactoring")) {
			return new HookRefactoring();
		}
		return null;
	}
}
