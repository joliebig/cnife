package analyzer;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import jargs.gnu.CmdLineParser;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import org.xml.sax.SAXException;

import analyzer.visitors.FeatureVisitor;
import backend.storage.IdentifiedFeature;
import backend.storage.IdentifiedFeatureList;
import backend.storage.PreprocessorOccurrence;

import refactoring.RefactoringDocument;

public class FeatureRefactoringAnalyzer {
	private File outputDir;
	LinkedList<FeatureVisitor> visitors;
	IdentifiedFeatureList backend;
	LinkedList<String> annotationfilter;

	public FeatureRefactoringAnalyzer(File outputDir, LinkedList<String> annotationfilter) {
		assert ((outputDir.exists()) && (outputDir.isDirectory()));

		this.outputDir = outputDir;
		this.visitors = new LinkedList<FeatureVisitor>();
		this.annotationfilter = annotationfilter;

		initBackEnd();
		initProjectDir();
	}

	private void initBackEnd() {
		this.backend = new IdentifiedFeatureList();
	}

	private void initProjectDir() {
		File baseDir = new File(this.outputDir.getAbsolutePath()
				+ File.separatorChar + "Base");
		if (baseDir.exists()) {
			File[] outputDirContent = this.outputDir
					.listFiles(new FileFilter() {
						public boolean accept(File file) {
							return (file.isDirectory())
									&& (!file.getName().startsWith("."));
						}
					});
			for (File fileDir : outputDirContent) {
				File[] fileDirContent = fileDir.listFiles(new FileFilter() {
					public boolean accept(File file) {
						return (file.getName().toLowerCase()
								.matches(".*\\.xml")) && (!file.isDirectory());
					}
				});
				for (File featureFile : fileDirContent) {
					FeatureSearcher searcher = new FeatureSearcher();
					searcher.setOutputDir(this.outputDir);
					searcher.setBackend(this.backend);
					searcher.checkDoc(featureFile);
				}
			}
		} else {
			baseDir.mkdir();
		}
	}

	public boolean addFile(File xmlFile) {
		if (!xmlFile.exists()) {
			return false;
		}

		FeatureSearcher scanner = new FeatureSearcher();
		scanner.setOutputDir(this.outputDir);
		scanner.setBackend(this.backend);
		scanner.checkDoc(xmlFile);

		return true;
	}

	public LinkedList<String> getFeatureNames() {
		Iterator<IdentifiedFeature> it = this.backend.iterate();
		LinkedList<String> featureNames = new LinkedList<String>();
		while (it.hasNext()) {
			featureNames.add(((IdentifiedFeature) it.next()).getName());
		}
		return featureNames;
	}

	public String getRefactoringSuggestion() {
		return null;
	}

	public LinkedList<IdentifiedFeature> getBestFeatures(int topK) {
		return null;
	}

	public void saveDisciplining(LinkedList<RefactoringDocument> modfiles)
		throws FileNotFoundException,
				TransformerFactoryConfigurationError,
				TransformerException {
		for (RefactoringDocument doc: modfiles) {
			doc.saveDocument(doc.getDocumentURI().substring(5));
		}

		initProjectDir();
	}

	public void saveRefactorings(Set<String> affectedFiles,
			LinkedList<RefactoringDocument> modFiles)
			throws FileNotFoundException, TransformerFactoryConfigurationError,
			TransformerException {
		for (String file : affectedFiles)
				this.backend.removeFileFromList(file);

		for (RefactoringDocument doc : modFiles) {
			String featureName = doc.getFeatureName();
			if (featureName == null) {
				doc.saveDocument(this.outputDir.getAbsolutePath()
						+ File.separatorChar + "Base" + File.separatorChar
						+ doc.getFileName());
			} else {
				File featureDir = new File(this.outputDir.getAbsolutePath()
						+ File.separatorChar + featureName);
				if (!featureDir.exists())
					featureDir.mkdir();
				doc.saveDocument(featureDir.getAbsolutePath()
						+ File.separatorChar + doc.getFileName());
			}

		}

		initProjectDir();
	}

	public void registerVisitors(FeatureVisitor visitor) {
		this.visitors.add(visitor);
	}

	public static void main(String[] args) throws SAXException, IOException,
			ParserConfigurationException, TransformerFactoryConfigurationError,
			TransformerException {
		// setup command line arguments parser
		CmdLineParser cmdparser = new CmdLineParser();

		// inputfolder that cnife analyzes or refactors
		CmdLineParser.Option inputfolder = cmdparser.addStringOption('i',
				"inputfolder");

		// use a simple clonefinder to reduce the number of refinements
		// (function/statement level)
		CmdLineParser.Option detectclones = cmdparser.addBooleanOption('d',
				"detectclones");

		// refactor or analyze only to get statistics
		CmdLineParser.Option refactor = cmdparser.addBooleanOption('r',
				"refactor");

		// provide hook names
		CmdLineParser.Option providehooknames = cmdparser.addBooleanOption('h',
				"providehooknames");

		// limit statement transformations
		CmdLineParser.Option stmttrafo = cmdparser.addIntegerOption('s',
				"stmttrafo");

		// filter annotations, e.g. omit case-blocks
		CmdLineParser.Option annotationfilter = cmdparser.addStringOption('a',
				"annotationfilter");

		try {
			cmdparser.parse(args);
		} catch (CmdLineParser.OptionException e) {
			System.err.println(e.getMessage());
			System.exit(2);
		}

		String inputfolderval = (String) cmdparser.getOptionValue(inputfolder, null);
		Boolean detectclonesval = (Boolean) cmdparser.getOptionValue(detectclones,
				Boolean.TRUE);

		if (inputfolderval != null) {
			inputfolderval.trim();
		} else {
			System.err.println("No inputfolder given. Terminating ...!");
			System.exit(2);
		}
		Boolean refactorval = (Boolean) cmdparser.getOptionValue(refactor, Boolean.FALSE);
		Boolean providehooknamesval = (Boolean) cmdparser.getOptionValue(
				providehooknames, Boolean.FALSE);
		Integer stmttrafoval = (Integer) cmdparser.getOptionValue(stmttrafo, 0);
		String s = (String) cmdparser.getOptionValue(annotationfilter, null);
		String[] slist = null;

		if (s != null)
			slist = s.split(",");
		LinkedList<String> annotationfilterlist = null;
		if (slist != null) {
			annotationfilterlist = new LinkedList<String>();
			for (String str: slist)
				annotationfilterlist.add(str);
		}

		File projectDirectory = new File(inputfolderval);
		if (!projectDirectory.exists() || !projectDirectory.isDirectory()) {
			System.err.println("Project directory not valid: " + projectDirectory);
			System.exit(2);
		}

		int simple = 0;
		int hook = 0;
		int ugly = 0;
		int impc = 0;
		int omit = 0;
		FeatureRefactoringAnalyzer a = new FeatureRefactoringAnalyzer(
				projectDirectory, annotationfilterlist);
		LinkedList<String> list = a.getFeatureNames();
		LinkedList<AnalyzedFeature> afeats = new LinkedList<AnalyzedFeature>();
		System.out.println("Found " + list.size()
				+ " different feature candidates");
		for (String name : list) {
			IdentifiedFeature feat = a.backend.getIdentifiedFeatureByName(name);
			AnalyzeFeature afeat = new AnalyzeFeature(feat, detectclonesval, providehooknamesval);
			LinkedList<RefactoringDocument> modfiles = afeat.analyze();
			a.saveDisciplining(modfiles);
			afeats.add(afeat.getAnalyzedFeature());
			Iterator<PreprocessorOccurrence> it = afeat.getAnalyzedFeature()
			.iterateOccurrences();
			System.out.println("processing Feature " + name + " ...");
			while (it.hasNext()) {
				PreprocessorOccurrence occ = (PreprocessorOccurrence) it.next();
				System.out.println("\nFound type: "
						+ occ.getType()
						+ " on linenumber: "
						+ occ.getPrepNodes()[0].getLineNumber()
						+ "\nin File: "
						+ occ.getDocFileName());
				if (stmttrafoval > 0 && occ.getLinesOfCode() <= stmttrafoval)
					occ.setType("toomit");
				if ((occ.getType().startsWith("impossible"))
						|| (occ.getType().startsWith("unknown")))
					impc++;
				else if (occ.getType().startsWith("Hook"))
					hook++;
				else if (occ.getType().startsWith("toomit"))
					omit++;
				else {
					simple++;
				}
			}
			System.out.println("-------------------------------------------------------------\n");
			if (refactorval) {
				LinkedList<RefactoringDocument> modFiles = afeat.refactor();
				a.saveRefactorings(afeat.getAnalyzedFeature().getAffectedFiles()
					.keySet(), modFiles);
			}
		}
		System.out.println("Finished. Statistics:");
		System.out.println("the good: " + simple);
		System.out.println("the bad " + hook);
		System.out.println("the ugly " + ugly);
		System.out.println("impossibles: " + impc);
		System.out.println("omitted (user intervention): " + omit);
	}
}
