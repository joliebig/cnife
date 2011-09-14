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
	public static File runSrc2srcml(File infile) {
		File res = null;
		try {
			res = File.createTempFile("test", ".c", tmpdir);
			Runtime rt = Runtime.getRuntime();
			String src2srcmlcmd = "src2srcml";
			src2srcmlcmd += " --language C++"; // set language
			src2srcmlcmd += " " + infile.getAbsolutePath(); // set input file
			src2srcmlcmd += " -o " + res; // set output file

			Process pr = rt.exec(src2srcmlcmd);
			int exitval = pr.waitFor();
			if (exitval != 0)
				System.out.println("src2srcml returned with value: " + exitval);

		} catch (Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}
		return res;
	}

	/**
	 * Method that runs the srcml2src tool on an input file generating an
	 * output file.
	 * @param infile
	 * @return
	 */
	public static File runSrcml2src(File infile) {
		File res = null;
		try {
			res = File.createTempFile("test", ".c", tmpdir);
			Runtime rt = Runtime.getRuntime();
			String srcml2srccmd = "srcml2src";
			srcml2srccmd += " " + infile;
			srcml2srccmd += " --language C++";
			srcml2srccmd += " -o " + res;

			Process pr = rt.exec(srcml2srccmd);
			int exitval = pr.waitFor();
			if (exitval != 0)
				System.out.println("srcml2src returned with value: " + exitval);
		} catch (Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}
		return res;
	}

	/**
	 * This method returns files that contain the src2srcml representation of
	 * all infiles.
	 * @param infiles
	 * @return
	 */
	public static LinkedList<File> runAll(LinkedList<File> infiles) {
		LinkedList<File> res = new LinkedList<File>();

		for (File infile: infiles) {
			res.add(Src2Srcml.runSrc2srcml(infile));
		}

		return res;
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
	 * This method returns a list of file handles with all variants of an undisiciplined
	 * annotated statement, wrapped in a simple test function.
	 * @param infiles
	 * @return
	 */
	public static LinkedList<File> prepareAllFiles(LinkedList<File> infiles) {
		LinkedList<File> res = new LinkedList<File>();
		File cr = null;

		for (File infile: infiles) {
			cr = Src2Srcml.prepareFile(infile);
			res.add(cr);
		}
		return res;
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

	/**
	 * Method returns a list of NodeLists that contain the expanded statements of an undisciplined
	 * annotated statement.
	 * @param infiles
	 * @return
	 */
	public static LinkedList<NodeList> extractNodesFromAll(LinkedList<File> infiles) {
		LinkedList<NodeList> res = new LinkedList<NodeList>();

		for (File infile: infiles) {
			res.add(Src2Srcml.extractNodes(infile));
		}

		return res;
	}

}
