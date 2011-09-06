package common;

import java.io.File;
import java.util.LinkedList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Src2Srcml {

	/**
	 * Method that runs the src2srcml tool on an input file generating
	 * the output file.
	 * @param infile
	 * @param outfile
	 */
	public static void run(File infile, File outfile) {
		try {
			Runtime rt = Runtime.getRuntime();
			String src2srcmlcmd = "src2srcml";
			src2srcmlcmd = " --language C++"; // set language
			src2srcmlcmd = " " + infile; // set input file
			src2srcmlcmd = " -o " + outfile; // set output file

			Process pr = rt.exec(src2srcmlcmd);
			int exitval = pr.waitFor();
			if (exitval != 0)
				System.out.println("src2srcml returned with value: " + exitval);

		} catch (Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Method adds
	 * @param infile
	 */
	public static void prepareFile(File infile) {

	}

	/**
	 * Method returns a list of configuration parameters extracted from code.
	 * @param n AST Node, we use for the extraction.
	 * @return
	 */
	@SuppressWarnings("unused")
	private static LinkedList<String> getConfigurationParameter(Node n) {
		String FEATURE_NAMES = "//cpp:if//expr/name";
		LinkedList<String> res = new LinkedList<String>();
		try {
			XPathExpression names = QueryBuilder.instance().getExpression(FEATURE_NAMES);
			NodeList nl = (NodeList) names.evaluate(n, XPathConstants.NODESET);

			for (int i = 0; i < nl.getLength(); i++)
				res.add(nl.item(i).toString());
		} catch (XPathExpressionException e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}

		return new LinkedList<String>();
	}

	/**
	 * Method extracts all statements of a function in form of a nodelist.
	 * @param infile
	 * @return
	 */
	public static NodeList extract(File infile) {
		Document doc = XMLTools.parseDocument(infile);
		String FUNCTION_STMTS = "//function/block/*";
		NodeList nl = null;

		try {
			XPathExpression stmts = QueryBuilder.instance().getExpression(FUNCTION_STMTS);
			nl = (NodeList) stmts.evaluate(doc.getFirstChild(), XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}
		return nl;
	}
}
