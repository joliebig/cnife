package refactoring;

import java.util.HashMap;

public class RefactoringConfiguration {
	HashMap<RefactoringStrategy, RefactoringRule> levels;

	public RefactoringConfiguration() {
		levels = new HashMap<RefactoringStrategy, RefactoringRule>();
	}

	public void addRule(RefactoringStrategy from, RefactoringStrategy to)
			throws RefactoringConfigurationException {
		addRule(from, to, 0);
	}

	public void addRule(RefactoringStrategy from, RefactoringStrategy to,
			int minlines) throws RefactoringConfigurationException {
		if (!checkRule(from, to)) {
			throw new RefactoringConfigurationException(
					"RefactoringStrategy not in scope!");
		}
		RefactoringRule current = null;
		if (!this.levels.containsKey(from)) {
			current = new RefactoringRule();
			this.levels.put(from, current);
		} else {
			current = (RefactoringRule) this.levels.get(from);
		}
		current.addRule(minlines, to);
	}

	private boolean checkRule(RefactoringStrategy from, RefactoringStrategy to) {
		if (from == to)
			return true;
		if (from == null) {
			return false;
		}
		return checkRule(from.getWiderScopeStrategy(), to);
	}

	public RefactoringStrategy getMappedRule(RefactoringStrategy strat) {
		return getMappedRule(strat, 0);
	}

	public RefactoringStrategy getMappedRule(RefactoringStrategy strat,
			int lines) {
		RefactoringStrategy mappedStrat = null;
		if (!this.levels.containsKey(strat)) {
			mappedStrat = strat;
		} else {
			RefactoringRule rule = (RefactoringRule) this.levels.get(strat);
			mappedStrat = rule.getBestStrategy(lines);
			if (mappedStrat == null) {
				mappedStrat = strat;
			}
		}
		return mappedStrat;
	}
}
