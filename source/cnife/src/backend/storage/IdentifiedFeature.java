package backend.storage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class IdentifiedFeature {
	private LinkedList<PreprocessorOccurrence> list;
	private int score;
	private String name;
	private long LOCs = 0L;
	private HashMap<String, LinkedList<PreprocessorOccurrence>> fileOccurrences;

	public IdentifiedFeature() {
		this.list = new LinkedList<PreprocessorOccurrence>();
		this.fileOccurrences =
			new HashMap<String, LinkedList<PreprocessorOccurrence>>();
	}

	public int getScore() {
		return this.score;
	}

	public int getClutteredPartsCount() {
		return this.fileOccurrences.size();
	}

	public long getLOCs() {
		return this.LOCs;
	}

	public Iterator<PreprocessorOccurrence> iterateOccurrences() {
		return this.list.iterator();
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addOccurrence(PreprocessorOccurrence occ) {
		if (occ.getPrepNodes().length == 2)
			this.LOCs += occ.getPrepNodes()[1].getLineNumber()
					- occ.getPrepNodes()[0].getLineNumber();
		else if (occ.getPrepNodes().length == 3) {
			this.LOCs += occ.getPrepNodes()[2].getLineNumber()
					- occ.getPrepNodes()[0].getLineNumber();
		}

		this.list.add(occ);

		LinkedList<PreprocessorOccurrence> occInFile = null;
		if (this.fileOccurrences.containsKey(occ.getDocFileName())) {
			occInFile = this.fileOccurrences.get(occ.getDocFileName());
		} else {
			occInFile = new LinkedList<PreprocessorOccurrence>();
			this.fileOccurrences.put(occ.getDocFileName(), occInFile);
		}
		occInFile.add(occ);
	}

	/**
	 * removes a preprocessor occurrence from the backend
	 * @param occ
	 */
	public void removeOccurrence(PreprocessorOccurrence occ) {
		// remove occurrence from file-occurrences
		this.fileOccurrences.remove(occ.getDocFileName());

		// remove occurrence from the list itself
		this.list.remove(occ);
	}

	public void removeWholeFileFromFeature(String fileName) {
		if (this.fileOccurrences.containsKey(fileName)) {
			LinkedList<PreprocessorOccurrence> file =
				fileOccurrences.get(fileName);
			for (PreprocessorOccurrence occ : file) {
				this.LOCs -= occ.getLinesOfCode();
			}
			this.list.removeAll(file);
			this.fileOccurrences.remove(fileName);
		}
	}

	public boolean isEmpty() {
		return this.list.isEmpty();
	}
}
