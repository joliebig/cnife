package common;

import java.io.File;
import java.util.LinkedList;

import org.javatuples.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class TestExpansion {
	public static void main(String args[]) {
		Document doc = XMLTools.parseDocument(new File("/home/joliebig/test1.c.xml"));
		Node switchstmt = doc.getElementsByTagName("switch").item(0);
		LinkedList<String> fnames = Src2Srcml.getConfigurationParameter(switchstmt);
		LinkedList<LinkedList<Boolean>> confs = Preprocessor.combinations(fnames.size());
		LinkedList<Pair<Boolean, String>> c = Preprocessor.createConfiguration(confs.getFirst(), fnames);
		File ofd = Preprocessor.prepareCodeForCPP(switchstmt.getTextContent());
		File nfd = Preprocessor.run(c, ofd);
	}
}
