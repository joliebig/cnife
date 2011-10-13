package refactoring;

import java.util.TreeMap;

public class RefactoringRule {
	TreeMap<Integer, RefactoringStrategy> mapping;

	public RefactoringRule() {
		mapping = new TreeMap<Integer, RefactoringStrategy>();
	}

	public void addRule(int minlines, RefactoringStrategy strategy) {
		this.mapping.put(Integer.valueOf(minlines), strategy);
	}

	public RefactoringStrategy getBestStrategy(int lines) {
		return (RefactoringStrategy) this.mapping.floorEntry(
				Integer.valueOf(lines)).getValue();
	}
}
