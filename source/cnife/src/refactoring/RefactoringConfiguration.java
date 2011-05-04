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
		} else {
			RefactoringRule current = null;
			if (!levels.containsKey(from)) {
				current = new RefactoringRule();
				levels.put(from, current);
			} else {
				current = levels.get(from);
			}
			current.addRule(minlines, to);

		}
	}

	private boolean checkRule(RefactoringStrategy from, RefactoringStrategy to) {
		if (from == to) {
			return true;
		} else if (from == null) {
			return false;
		} else {
			return checkRule(from.getWiderScopeStrategy(), to);
		}

	}

	public RefactoringStrategy getMappedRule(RefactoringStrategy strat) {
		return getMappedRule(strat, 0);
	}

	public RefactoringStrategy getMappedRule(RefactoringStrategy strat,
			int lines) {
		RefactoringStrategy mappedStrat = null;
		if (!levels.containsKey(strat)) {
			mappedStrat = strat;
		} else {
			RefactoringRule rule = levels.get(strat);
			mappedStrat = rule.getBestStrategy(lines);
			if (mappedStrat == null) {
				mappedStrat = strat;
			}
		}
		return mappedStrat;
	}
}
