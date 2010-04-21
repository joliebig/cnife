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
		return list.values().iterator();
	}
	
	/**
	 * @deprecated
	 * @param feature
	 */
	public void add(IdentifiedFeature feature) {
		list.put(feature.getName(), feature);
	}
	
	public void add(String featureName, PreprocessorOccurrence occurrence) {
		IdentifiedFeature feature = null;
		if (list.containsKey(featureName)) {
			feature = list.get(featureName);
		} else {
			feature = new IdentifiedFeature();
			feature.setName(featureName);
			list.put(featureName, feature);
		}
		feature.addOccurrence(occurrence);
		
		
		if (!knownDocs.containsKey(occurrence.getDocFileName())) {
			knownDocs.put(
					occurrence.getDocFileName(), occurrence.getDocument());
		}

	}
	
	public IdentifiedFeature getIdentifiedFeatureByName(String name) {
		if (list.containsKey(name)) {
			return list.get(name);
		} else {
			return null;
		}
	}
	
	public void refactorFeature (String name) {
		IdentifiedFeature feature = getIdentifiedFeatureByName(name);
		if (feature != null) {
			refactorFeature(feature);
		}
	}
	
	public void refactorFeature (IdentifiedFeature feature) {
		//TODO
	}
	
	/**
	 * Löscht alle Auftreten einer Datei aus dem Backend
	 * @param fileName
	 */
	public void removeFileFromList (String fileName) {
		if (knownDocs.containsKey(fileName)) {
			LinkedList<String> emptyFeatures = new LinkedList<String>();
			for (String featureName : list.keySet()) {
				IdentifiedFeature feat = list.get(featureName);
				feat.removeWholeFileFromFeature(fileName);
				if (feat.isEmpty()) {
					emptyFeatures.add(featureName);
				}
			}
			knownDocs.remove(fileName);
			for (String featureName : emptyFeatures) {
				list.remove(featureName);
			}
		}
	}
	
	public boolean isKnown(String docFileName) {
		return knownDocs.containsKey(docFileName);
	}
}
