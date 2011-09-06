package common;

import java.util.Iterator;
import java.util.LinkedList;
import java.io.*;
import org.javatuples.*;

public class Preprocessor {

	private static final File tmpdir = new File("/tmp");

	/**
	 * Method generates all combinations of true, false for a given number of elements
	 * e.g.:
	 * combinations(2):
	 * [[true, true], [true,false], [false,true], [false,false]]
	 * @param n
	 * @return all combinations as a list of lists of booleans
	 */
	public static LinkedList<LinkedList<Boolean>> combinations(int n) {
		if (n <= 0) {
			LinkedList<LinkedList<Boolean>> r = new LinkedList<LinkedList<Boolean>>();
			r.add(new LinkedList<Boolean>());
			return r;
		} else {
			LinkedList<LinkedList<Boolean>> res = new LinkedList<LinkedList<Boolean>>();

			for (LinkedList<Boolean> e: combinations(n-1)) {
				@SuppressWarnings("unchecked")
				LinkedList<Boolean> e1 = ((LinkedList<Boolean>)e.clone());
				@SuppressWarnings("unchecked")
				LinkedList<Boolean> e2 = ((LinkedList<Boolean>)e.clone());
				e1.add(true);
				e2.add(false);
				res.add(e1);
				res.add(e2);
			}
			return res;
		}
	}

	/**
	 * Method applies the cpp-tool with a given configuration on the input file
	 * generating the output file
	 * @param conf
	 * @param infile
	 * @param outfile
	 */
	public static File run(LinkedList<Pair<Boolean, String>> conf, File infile) {
		File outfile = null;
		try {
			outfile = File.createTempFile("test", ".c", tmpdir);
			Runtime rt = Runtime.getRuntime();
			String cppcmd = "cpp";
			cppcmd += " -CC"; // preserver comments
			cppcmd += " -P";  // do not create line-markers
			cppcmd += " -fdirectives-only"; // do not expand macros

			for (Pair<Boolean, String> cp: conf) {
				if (cp.getValue0()) cppcmd += " -D";
				else cppcmd += " -U";
				cppcmd += cp.getValue1();
			}

			cppcmd += " " + infile; // input file
			cppcmd += " " + outfile; // output file

			Process pr = rt.exec(cppcmd);
			int exitval = pr.waitFor();
			if (exitval != 0)
				System.out.println("Exited with error code " + exitval);
		} catch (Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}
		return outfile;
	}

	/**
	 * Method creates a configuration for the preprocessor for
	 * @param c
	 * @param p
	 * @return
	 */
	public static LinkedList<Pair<Boolean, String>> createConfiguration(LinkedList<Boolean> c, LinkedList<String> p) {
		LinkedList<Pair<Boolean, String>> res = new LinkedList<Pair<Boolean, String>>();

		Iterator<Boolean> i1 = c.iterator();
		Iterator<String> i2 = p.iterator();
		while (i1.hasNext() && i2.hasNext()) {
			res.add(new Pair<Boolean, String>(i1.next(), i2.next()));
		}

		return res;
	}

	/**
	 * Method puts the code of Node n in a file and returns the file-handle.
	 * @param n
	 * @return
	 */
	public static File prepareCodeForCPP(String n) {
		File fd = null;
		try {
			fd = File.createTempFile("test", ".c", tmpdir);

			BufferedWriter ofd = new BufferedWriter(new FileWriter(fd));
			ofd.write(n);
			ofd.close();
		} catch (IOException e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}

		return fd;
	}
}
