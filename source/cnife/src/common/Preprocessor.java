package common;

import java.util.LinkedList;
import java.io.*;

import org.w3c.dom.Node;

public class Preprocessor {
	private static LinkedList<LinkedList<Boolean>> combinations(int n) {
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

	private static void run(LinkedList<Boolean> conf, File infile, File outfile) {
		try {
			Runtime rt = Runtime.getRuntime();
			String cppcmd = "cpp";
			cppcmd += " --CC"; // preserver comments
			cppcmd += " --P";  // do not create line-markers
			cppcmd += " --fdirectives-only"; // do not expand macros
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
	}

	private static LinkedList<String> getConfigurationParameter(Node n) {
		return new LinkedList<String>();
	}
}
