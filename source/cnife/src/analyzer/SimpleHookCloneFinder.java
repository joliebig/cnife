package analyzer;

import java.util.HashMap;
import org.w3c.dom.Node;

public class SimpleHookCloneFinder {
	private HashMap<String, CriticalOccurrence> hookMap;

	public SimpleHookCloneFinder() {
		this.hookMap = new HashMap<String, CriticalOccurrence>();
	}

	public void doDuplicateCheck(CriticalOccurrence occ) {
		StringBuilder xmlContent = new StringBuilder("");
		Node node = occ.getPrepNodes()[0].getNode();
		while (node != occ.getPrepNodes()[1].getNode()) {
			xmlContent.append(node.getTextContent());
			node = node.getNextSibling();
		}
		if (occ.getPrepNodes().length > 2) {
			node = node.getNextSibling();
			while (node != occ.getPrepNodes()[2].getNode()) {
				xmlContent.append(node.getTextContent());
				node = node.getNextSibling();
			}

		}

		String key = xmlContent.toString().replaceAll("(\n+|\t+|\\s+)", " ");
		key = key.replaceAll("\\s\\s+", " ");
		if (this.hookMap.containsKey(key)) {
			occ.setDupe((CriticalOccurrence) this.hookMap.get(key));
			occ.setHookFunctionName(occ.getDupe().getHookFunctionName());
		} else {
			this.hookMap.put(key, occ);
		}
	}
}
