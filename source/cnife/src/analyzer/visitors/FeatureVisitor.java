package analyzer.visitors;

import backend.storage.IdentifiedFeature;
import backend.storage.IdentifiedFeatureList;
import backend.storage.PreprocessorOccurrence;

public abstract class FeatureVisitor {
	protected IdentifiedFeatureList list;
	
	public FeatureVisitor(IdentifiedFeatureList list) {
		this.list = list;
	}
	
	public abstract void visitAllNodes();
	
	protected abstract void visitFeature(IdentifiedFeature feature);
	protected abstract void visitOccurrence(
			PreprocessorOccurrence prepOccurrence);
}
