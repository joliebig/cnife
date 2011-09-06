package common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Src2Srcml {

	private static final File tmpdir = new File("/tmp");

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
	 * Method adds function declaration to infile and returns outfile with the
	 * result.
	 * @param infile
	 * @return
	 */
	public static File prepareFile(File infile) {
		File tmpfile = null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(infile));
			tmpfile = File.createTempFile("test", ".c", tmpdir);
			BufferedWriter bw = new BufferedWriter(new FileWriter(tmpfile));

			bw.write("void test() {");
			String line;

			while ((line = br.readLine()) != null) {
				// skip #define and #undef directives added by cpp
				if (line.startsWith("#")) continue;
				bw.write(line);
			}

			bw.write("}");
			bw.close();
		} catch (IOException e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}
		return tmpfile;
	}

	/**
	 * Method returns a list of configuration parameters extracted from code.
	 * @param n AST Node, we use for the extraction.
	 * @return
	 */
	public static LinkedList<String> getConfigurationParameter(Node n) {
		String FEATURE_NAMES = "//cpp:if//src:expr/src:name";
		Set<String> res = new HashSet<String>();
		try {
			XPathExpression names = QueryBuilder.instance().getExpression(FEATURE_NAMES);
			NodeList nl = (NodeList) names.evaluate(n, XPathConstants.NODESET);

			for (int i = 0; i < nl.getLength(); i++)
				res.add(nl.item(i).getTextContent());
		} catch (XPathExpressionException e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}

		LinkedList<String> rl = new LinkedList<String>();
		for (String s: res)
			rl.add(s);

		return rl;
	}

	/**
	 * Method extracts all statements of a function in form of a nodelist.
	 * @param infile
	 * @return
	 */
	public static NodeList extractNodes(File infile) {
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
