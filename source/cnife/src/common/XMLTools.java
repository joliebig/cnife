package common;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
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
}
