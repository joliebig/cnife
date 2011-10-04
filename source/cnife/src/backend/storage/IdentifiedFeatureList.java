package backend.storage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import org.w3c.dom.Document;

public class IdentifiedFeatureList {
	private HashMap<String, IdentifiedFeature> list;
	private HashMap<String, Document> knownDocs;

	public IdentifiedFeatureList() {
		this.list = new HashMap<String, IdentifiedFeature>();
		this.knownDocs = new HashMap<String, Document>();
	}

	public Iterator<IdentifiedFeature> iterate() {
		return this.list.values().iterator();
	}

	public void add(String featureName, PreprocessorOccurrence occurrence) {
		IdentifiedFeature feature = null;
		if (this.list.containsKey(featureName)) {
			feature = (IdentifiedFeature) this.list.get(featureName);
		} else {
			feature = new IdentifiedFeature();
			feature.setName(featureName);
			this.list.put(featureName, feature);
		}
		feature.addOccurrence(occurrence);

		if (!this.knownDocs.containsKey(occurrence.getDocFileName()))
			this.knownDocs.put(occurrence.getDocFileName(),
					occurrence.getDocument());
	}

	public IdentifiedFeature getIdentifiedFeatureByName(String name) {
		if (this.list.containsKey(name)) {
			return (IdentifiedFeature) this.list.get(name);
		}
		return null;
	}

	public void refactorFeature(String name) {
		IdentifiedFeature feature = getIdentifiedFeatureByName(name);
		if (feature != null)
			refactorFeature(feature);
	}

	public void refactorFeature(IdentifiedFeature feature) {
	}

	public void removeFileFromList(String fileName) {
		if (this.knownDocs.containsKey(fileName)) {
			LinkedList<String> emptyFeatures = new LinkedList<String>();
			for (String featureName : this.list.keySet()) {
				IdentifiedFeature feat = (IdentifiedFeature) this.list
						.get(featureName);
				feat.removeWholeFileFromFeature(fileName);
				if (feat.isEmpty()) {
					emptyFeatures.add(featureName);
				}
			}
			this.knownDocs.remove(fileName);
			for (String featureName : emptyFeatures)
				this.list.remove(featureName);
		}
	}

	public boolean isKnown(String docFileName) {
		return this.knownDocs.containsKey(docFileName);
	}
 }
