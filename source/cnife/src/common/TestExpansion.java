package common;

import java.io.File;
import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TestExpansion {
	public static void main(String args[]) {
		Document doc = XMLTools.parseDocument(new File("/home/joliebig/test1.c.xml"));
		Node switchstmt = doc.getElementsByTagName("switch").item(0);
		LinkedList<String> fnames = Src2Srcml.getConfigurationParameter(switchstmt);
		LinkedList<LinkedList<Boolean>> confs = Preprocessor.combinations(fnames.size());
		File ofd = Preprocessor.writeCode2File(switchstmt.getTextContent());
		LinkedList<File> nfd = Preprocessor.runAll(confs, fnames, ofd);
		LinkedList<File> nfd1 = Src2Srcml.prepareAllFiles(nfd);
		LinkedList<File> nfd2 = Src2Srcml.runAll(nfd1);
		LinkedList<NodeList> nfd3 = Src2Srcml.extractNodesFromAll(nfd2);

		for (NodeList nl: nfd3)
			System.out.println("nodelist has " + nl.getLength() + " items");

	}
}
