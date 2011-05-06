package analyzer;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import jargs.gnu.CmdLineParser;

import org.xml.sax.SAXException;

import refactoring.RefactoringDocument;
import scanner.FeatureSearcher;
import analyzer.visitors.FeatureVisitor;
import backend.storage.IdentifiedFeature;
import backend.storage.IdentifiedFeatureList;
import backend.storage.PreprocessorOccurrence;

public class FeatureRefactoringAnalyzer {

	private File outputDir;
	LinkedList<FeatureVisitor> visitors;
	IdentifiedFeatureList backend;
	LinkedList<String> annotationfilter;

	public FeatureRefactoringAnalyzer(File outputDir, LinkedList<String> annotationfilterlist) {
		assert outputDir.exists() && outputDir.isDirectory();

		this.outputDir = outputDir;
		this.visitors = new LinkedList<FeatureVisitor>();
		this.annotationfilter = annotationfilterlist;

		initBackEnd();
		initProjectDir();
	}

	private void initBackEnd() {
		backend = new IdentifiedFeatureList();
	}

	private void initProjectDir() {
		File baseDir = new File(outputDir.getAbsolutePath()
				+ File.separatorChar + "Base");
		if (baseDir.exists()) {
			File[] outputDirContent = outputDir.listFiles(new FileFilter() {

				@Override
				public boolean accept(File file) {
					if (file.isDirectory() && !file.getName().startsWith("."))
						return true;
					return false;
				}

			});

			for (File fileDir : outputDirContent) {
				File[] fileDirContent = fileDir.listFiles(new FileFilter() {

					@Override
					public boolean accept(File file) {
						if (file.getName().toLowerCase().matches(".*\\.xml")
								&& !file.isDirectory()) {
							return true;
						}
						return false;
					}
				});

				for (File featureFile : fileDirContent) {
					FeatureSearcher searcher = new FeatureSearcher();
					searcher.setOutputDir(outputDir);
					searcher.setBackend(backend);
					searcher.setAnnotationFilter(annotationfilter);
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
		} else {

			FeatureSearcher scanner = new FeatureSearcher();
			scanner.setOutputDir(outputDir);
			scanner.setBackend(backend);
			scanner.checkDoc(xmlFile);

			return true;
		}
	}

	public LinkedList<String> getFeatureNames() {
		Iterator<IdentifiedFeature> it = this.backend.iterate();
		LinkedList<String> featureNames = new LinkedList<String>();
		while (it.hasNext()) {
			featureNames.add(it.next().getName());
		}
		return featureNames;
	}

	/**
	 * debugging and testing
	 * 
	 * @return textrepresentation for refactorings
	 */
	public String getRefactoringSuggestion() {
		return null;
	}

	/**
	 * Berechnet die besten Features
	 * 
	 * @param topK
	 *            wieviele Features geliefert werden sollen
	 * @return sortiert nach Score (beste zuerst in der Liste) alle Features,
	 *         maximal topK-viele
	 */
	public LinkedList<IdentifiedFeature> getBestFeatures(int topK) {
		return null;
	}

	public void saveRefactorings(Set<String> affectedFiles,
			LinkedList<RefactoringDocument> modFiles)
			throws FileNotFoundException, TransformerFactoryConfigurationError,
			TransformerException {
		for (String file : affectedFiles) {
			backend.removeFileFromList(file);
		}

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
		CmdLineParser.Option findclones = cmdparser.addBooleanOption('f',
				"findclones");

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

		String inputfolderval = (String) cmdparser.getOptionValue(inputfolder);
		Boolean findclonesval = (Boolean) cmdparser.getOptionValue(findclones,
				Boolean.TRUE);
		inputfolderval.trim();
		Boolean refactorval = (Boolean) cmdparser.getOptionValue(refactor,
				Boolean.FALSE);
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
			System.out
					.print("Project directory not valid: " + projectDirectory);
			System.exit(1);
		}

		int simple = 0;
		int hook = 0;
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
			AnalyzeFeature afeat = new AnalyzeFeature(feat, findclonesval,
					providehooknamesval);
			afeat.analyze();
			afeats.add(afeat.getAnalyzedFeature());
			Iterator<PreprocessorOccurrence> it = afeat.getAnalyzedFeature()
					.iterateOccurrences();
			System.out.println("processing Feature " + name + " ...");
			while (it.hasNext()) {
				PreprocessorOccurrence occ = it.next();
				System.out.println("\nFound type: " + occ.getType()
						+ " on linenumber: "
						+ occ.getPrepNodes()[0].getLineNumber() + " ("
						+ occ.getLinesOfCode() + " loc)\nin File: "
						+ occ.getDocFileName());
				if (stmttrafoval > 0 && occ.getLinesOfCode() <= stmttrafoval)
					occ.setType("toomit");
				if (occ.getType().startsWith("impossible")
						|| occ.getType().startsWith("unknown")) {
					impc++;
				} else if (occ.getType().startsWith("Hook")) {
					hook++;
				} else if (occ.getType().startsWith("toomit")) {
					omit++;
				} else {
					simple++;
				}
			}
			System.out
					.println("-------------------------------------------------------------\n");

			if (refactorval) {
				LinkedList<RefactoringDocument> modFiles = afeat.refactor();
				a.saveRefactorings(afeat.getAnalyzedFeature()
						.getAffectedFiles().keySet(), modFiles);
			}
		}
		System.out.println("Finished. Statistics:");
		System.out.println("the good: " + simple);
		System.out.println("the bad " + hook);
		System.out.println("impossibles: " + impc);
		System.out.println("omitted (user intervention): " + omit);
	}

	public static void main3(String[] args) throws SAXException, IOException,
			ParserConfigurationException, XPathExpressionException,
			TransformerFactoryConfigurationError, TransformerException {
		FeatureRefactoringAnalyzer a = new FeatureRefactoringAnalyzer(new File(
				"BerkleyDB_HASH"), null);
		int impc = 0;
		int implc = 0;
		int simple = 0;
		int hook = 0;
		HashMap<String, Integer> types = new HashMap<String, Integer>();

		LinkedList<String> list = a.getFeatureNames();
		LinkedList<AnalyzedFeature> afeats = new LinkedList<AnalyzedFeature>();
		System.out.println("Feature-Anzahl: " + list.size());
		for (String name : list) {
			IdentifiedFeature feat = a.backend.getIdentifiedFeatureByName(name);
			System.out.println("Feature: " + name);
			System.out.println("gesamt LOCs: " + feat.getLOCs());
			AnalyzeFeature afeat = new AnalyzeFeature(feat, false, false);
			afeat.analyze();
			afeats.add(afeat.getAnalyzedFeature());
			System.out.println("LOCs" + feat.getLOCs());
			Iterator<PreprocessorOccurrence> it = afeat.getAnalyzedFeature()
					.iterateOccurrences();
			while (it.hasNext()) {
				PreprocessorOccurrence occ = it.next();
				System.out.println("Linenumber: "
						+ occ.getPrepNodes()[0].getLineNumber());
				System.out.println("in File: " + occ.getDocFileName());
				System.out.println("LOCs in File: " + occ.getLinesOfCode());
				System.out.println("Found type: " + occ.getType() + "\n");
				if (occ.getType().startsWith("impossible")
						|| occ.getType().startsWith("unknown")) {
					impc++;
					implc += occ.getLinesOfCode();
				} else if (occ.getType().startsWith("Hook")) {
					hook++;
				} else {
					simple++;
				}
				if (types.containsKey(occ.getType()))
					types.put(occ.getType(), types.get(occ.getType()) + 1);
				else
					types.put(occ.getType(), new Integer(1));
			}
			System.out
					.println("-------------------------------------------------------------\n");
			LinkedList<RefactoringDocument> modFiles = afeat.refactor();
			a.saveRefactorings(afeat.getAnalyzedFeature().getAffectedFiles()
					.keySet(), modFiles);
			afeat.refactor();
		}
		Set<String> keys = types.keySet();
		for (String type : keys) {
			System.out.println(type + ": " + types.get(type));
		}
		System.out.println("einfache: " + simple + " hooks " + hook);
		System.out.println("unmoegliche: " + impc + " LOCs " + implc);

		// File[] dirlist = (new File("source_mls")).listFiles();
		//
		// for (File file : dirlist) {
		// if (file.getName().matches(".*Stat.*")) {
		// System.out.println("bla");
		// }
		// boolean test = a.addFile(file);
		// if (test == false) {
		// System.out.println("File " + file.getName() + " not found!");
		// System.exit(0);
		// } else {
		// System.out.println("File " + file.getName() + " added!");
		// }
		// }

		// Iterator<IdentifiedFeature> it = a.backend.iterate();
		// LinkedList<String> featureNames = new LinkedList<String>();
		// while (it.hasNext()) {
		// featureNames.add(it.next().getName());
		// }
		// for (String name : featureNames) {
		// // String name = "HAVE_BTREE";
		// IdentifiedFeature feature =
		// a.backend.getIdentifiedFeatureByName(name);
		// if (feature == null) continue;
		// System.out.println(name);
		// AnalyzeFeature feat = new AnalyzeFeature(feature);
		// feat.analyze();
		//
		// LinkedList<RefactoringDocument> modFiles = feat.refactor();
		// a.saveRefactorings(feat.getAnalyzedFeature().getAffectedFiles().keySet(),
		// modFiles);

		// }
		// IdentifiedFeature feature = it.next();
		// System.out.println("Found Feature: " + feature.getName());
		// System.out.println("LOCS:          " + feature.getLOCs());
		// Iterator<PreprocessorOccurrence> occIt =
		// feature.iterateOccurrences();
		// while (occIt.hasNext()) {
		// PreprocessorOccurrence occ = occIt.next();
		// System.out.println("Occurrence in: " + occ.getDocFileName());
		// System.out.println("recognized Pattern: " + occ.getType());
		// PreprocessorNode[] nodes = occ.getPrepNodes();
		// for (int i = 0; i < nodes.length; i++) {
		// System.out.println("Directive  at: line "
		// + nodes[i].getLineNumber()
		// + " Type: "
		// + nodes[i].getType());
		// }
		// }
		// }
		// IdentifiedFeature feat =
		// a.backend.getIdentifiedFeatureByName("DIAGNOSTIC");
		// Iterator<PreprocessorOccurrence> featIt = feat.iterateOccurrences();
		// NewFileTemplate pos = NewFileTemplate.instantiate();
		// NewFileTemplate neg = NewFileTemplate.instantiate();
		// Document doc = null;
		// while(featIt.hasNext()) {
		// PreprocessorOccurrence occ = featIt.next();
		// RefactoringAction action =
		// RefactoringFactory.instance().getAction(occ.getType());
		// doc = occ.getPrepNodes()[0].getNode().getOwnerDocument();
		// action.moveNodes(doc,
		// pos, neg, occ, feat.getName());
		// }
		// pos.setClassName(doc);
		// neg.setClassName(doc);
		//
		//
		// System.out.println("-----ORIGINAL-----");
		// System.out.println(doc.getFirstChild().getTextContent());
		// System.out.println("---END:ORiGINAL---\n");
		// System.out.println("-------POS--------");
		// System.out.println(pos.getFirstChild().getTextContent());
		// System.out.println("-----END:POS------\n");
		// System.out.println("-------NEG--------");
		// System.out.println(neg.getFirstChild().getTextContent());
		// System.out.println("-----END:NEG------");
	}
}
