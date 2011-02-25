package refactoring;

public enum RefactoringStrategy {
	NO_REFACTORING, 
	SIMPLE_REFACTORING,
	BEFORE_REFACTORING,
	AFTER_REFACTORING,
	AROUND_REFACTORING,
	SIMPLE_HOOK,
	STATEMENT_REPLICATION_WITH_HOOK,
	BLOCK_REPLICATION_WITH_HOOK,
	REPLICATE_FUNCTION;

	public RefactoringStrategy getWiderScopeStrategy() {
		RefactoringStrategy wider = null;
		switch (this) {
		case NO_REFACTORING:
			wider = null;
			break;
		case SIMPLE_REFACTORING:
		case REPLICATE_FUNCTION:
			wider = NO_REFACTORING;
			break;
		case SIMPLE_HOOK:
		case STATEMENT_REPLICATION_WITH_HOOK:
			wider = BLOCK_REPLICATION_WITH_HOOK;
			break;
		case BEFORE_REFACTORING:
		case AFTER_REFACTORING:
		case AROUND_REFACTORING:
		case BLOCK_REPLICATION_WITH_HOOK:
			wider = REPLICATE_FUNCTION;
			break;
		}
		return wider;
	}
}
