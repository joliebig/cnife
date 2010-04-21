package backend.storage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class IdentifiedFeature {
	private LinkedList<PreprocessorOccurrence> list;
	private int score;
	private String name;
	private long LOCs = 0;
	private HashMap<String, LinkedList<PreprocessorOccurrence>> fileOccurrences;
	
	public IdentifiedFeature() {
		this.list = new LinkedList<PreprocessorOccurrence>();
		this.fileOccurrences = 
			new HashMap<String, LinkedList<PreprocessorOccurrence>>();
	}
	
	public int getScore() {
		return score;
	}
	
	public int getClutteredPartsCount() {
		return fileOccurrences.size(); 
	}
	
	public long getLOCs() {
		return LOCs;
	}
	
	public Iterator<PreprocessorOccurrence> iterateOccurrences() {
		return list.iterator();
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Fügt ein neues PreprocessorOccurrence-Objekt diesem Feature hinzu
	 * @param occ
	 */
	public void addOccurrence(PreprocessorOccurrence occ) {
		if (occ.getPrepNodes().length == 2) {
			this.LOCs += 
				occ.getPrepNodes()[1].getLineNumber() 
				- occ.getPrepNodes()[0].getLineNumber();
		} else if (occ.getPrepNodes().length == 3) {
			this.LOCs += 
				occ.getPrepNodes()[2].getLineNumber() 
				- occ.getPrepNodes()[0].getLineNumber();
		}
		
		list.add(occ);
		
		LinkedList<PreprocessorOccurrence> occInFile = null;
		if (fileOccurrences.containsKey(occ.getDocFileName())) {
			occInFile = fileOccurrences.get(occ.getDocFileName());
		} else {
			occInFile = new LinkedList<PreprocessorOccurrence>();
			fileOccurrences.put(occ.getDocFileName(), occInFile);
		}
		occInFile.add(occ);
	}
	
	/**
	 * Löscht alle Auftreten einer Datei aus diesem Feature
	 * @param fileName
	 */
	public void removeWholeFileFromFeature (String fileName) {
		if (fileOccurrences.containsKey(fileName)) {
			LinkedList<PreprocessorOccurrence> file = 
				fileOccurrences.get(fileName);
			for (PreprocessorOccurrence occ : file) {
				this.LOCs -= occ.getLinesOfCode();
			}
			list.removeAll(file);
			fileOccurrences.remove(fileName);
		}
	}

	public boolean isEmpty () {
		return list.isEmpty();
	}
}
