package common;

import java.io.File;

public class Src2Srcml {
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

}
