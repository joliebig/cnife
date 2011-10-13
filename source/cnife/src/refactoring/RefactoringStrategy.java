package refactoring;

public enum RefactoringStrategy {
	NO_REFACTORING, SIMPLE_REFACTORING, BEFORE_REFACTORING, AFTER_REFACTORING, AROUND_REFACTORING, SIMPLE_HOOK, STATEMENT_REPLICATION_WITH_HOOK, BLOCK_REPLICATION_WITH_HOOK, REPLICATE_FUNCTION;

	public RefactoringStrategy getWiderScopeStrategy() {
		RefactoringStrategy wider = null;
		switch (this) {
		case AFTER_REFACTORING:
			wider = null;
			break;
		case AROUND_REFACTORING:
		case STATEMENT_REPLICATION_WITH_HOOK:
			wider = NO_REFACTORING;
			break;
		case REPLICATE_FUNCTION:
		case SIMPLE_HOOK:
			wider = BLOCK_REPLICATION_WITH_HOOK;
			break;
		case BEFORE_REFACTORING:
		case BLOCK_REPLICATION_WITH_HOOK:
		case NO_REFACTORING:
		case SIMPLE_REFACTORING:
			wider = REPLICATE_FUNCTION;
		}

		return wider;
	}
}
