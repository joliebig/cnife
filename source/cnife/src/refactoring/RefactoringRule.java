package refactoring;


import java.util.TreeMap;

public class RefactoringRule {
	TreeMap<Integer, RefactoringStrategy> mapping;
	
	public RefactoringRule() {
		mapping = new TreeMap<Integer, RefactoringStrategy>();
	}
	public void addRule(int minlines, RefactoringStrategy strategy) {
		mapping.put(minlines, strategy);
	}
	
	public RefactoringStrategy getBestStrategy (int lines) {
		return mapping.floorEntry(lines).getValue();
	}
}
