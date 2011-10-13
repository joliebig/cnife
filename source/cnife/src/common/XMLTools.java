package common;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class XMLTools {
	public static Document parseDocument(File xmlDoc) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			Document doc = factory.newDocumentBuilder().parse(xmlDoc);
			return doc;
		} catch (IOException e) {
			System.out.println(e.toString());
			e.printStackTrace();
		} catch (SAXException e) {
			System.out.println(e.toString());
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * This method returns a linked list of all siblings for a given Node n.
	 * @param n
	 * @return
	 */
	public static LinkedList<Node> getSiblings(Node n) {
		LinkedList<Node> res = new LinkedList<Node>();

		while (n != null) {
			res.add(n);
			n = n.getNextSibling();
		}

		return res;
	}
}
