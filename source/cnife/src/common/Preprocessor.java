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
			outfile.deleteOnExit();
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
		return processCPPOutput(conf, outfile);
	}

	/**
	 * This method generates all variants of an annotated input file given a list of configurations confs
	 * and a list of parameters p
	 * @param confs
	 * @param p
	 * @param infile
	 * @return
	 */
	public static LinkedList<File> runAll(LinkedList<LinkedList<Boolean>> confs, LinkedList<String> p, File infile) {
		LinkedList<File> res = new LinkedList<File>();
		LinkedList<Pair<Boolean, String>> cc = null;
		File out = null;

		for (LinkedList<Boolean> conf: confs) {
			cc = Preprocessor.createConfiguration(conf, p);
			out = Preprocessor.run(cc, infile);
			res.add(out);
		}

		return res;
	}

	/**
	 * This method processes the output of a cpp run (i.e., remove #define macros at start and
	 * add the configuration #if)
	 * @param c configuration of the program variant
	 * @param outfile file that contains unpreprocessed cpp output
	 * @return
	 */
	private static File processCPPOutput(LinkedList<Pair<Boolean, String>> conf, File outfile) {
		if (outfile == null || conf == null) return null;
		File res = null;
		String sconf = "#if ";

		for (Pair<Boolean, String> e: conf) {
			if (!e.getValue0()) sconf += "!";
			sconf += "defined(" + e.getValue1() + ") && ";
		}

		sconf = sconf.substring(0, sconf.length()-3); // split last &&

		try {
			res = File.createTempFile("test", ".c", tmpdir);
			BufferedReader br = new BufferedReader(new FileReader(outfile));
			BufferedWriter bw = new BufferedWriter(new FileWriter(res));
			String line = null;
			bw.write(sconf+"\n");

			while ((line = br.readLine()) != null) {
				// beware this might cause a problem
				// cpp run generates a list of #defines and attaches them prior to
				// the preprocessed code, if a user uses a #define on his own this
				// #define is delete just like the generated ones.
				if (line.startsWith("#"))
					continue;
				bw.write(line+"\n");
			}

			bw.write("#endif\n");
			bw.close();

		} catch (IOException e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}

		return res;
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
		File res = null;
		try {
			res = File.createTempFile("test", ".c", tmpdir);
			res.deleteOnExit();

			BufferedWriter ofd = new BufferedWriter(new FileWriter(res));
			ofd.write(n);
			ofd.close();
		} catch (IOException e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}

		return res;
	}
}
