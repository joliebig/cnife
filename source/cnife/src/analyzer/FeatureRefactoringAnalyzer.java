package analyzer;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import refactoring.RefactoringDocument;
import scanner.FeatureSearcher;


import analyzer.visitors.FeatureVisitor;
import backend.storage.IdentifiedFeature;
import backend.storage.IdentifiedFeatureList;

public class FeatureRefactoringAnalyzer {

	private File outputDir;
	LinkedList <FeatureVisitor> visitors;
	IdentifiedFeatureList backend;
	
	public FeatureRefactoringAnalyzer(File outputDir) {
		assert outputDir.exists() && outputDir.isDirectory();
		
		this.outputDir = outputDir;
		this.visitors = new LinkedList<FeatureVisitor>();
		
		initBackEnd();
		initProjectDir();
	}
	
	private void initBackEnd() {
		backend = new IdentifiedFeatureList(); 
	}

	private void initProjectDir() {
		File baseDir = new File(outputDir.getAbsolutePath() + File.separatorChar + "Base");
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
	 * F�rs Debugging und Testing ;)
	 * @return eine Textrepr�sentation der Refactoringvorschl�ge
	 */
	public String getRefactoringSuggestion() {
		//TODO Textausgabe des Refactorings generieren
		return null;
	}
	
	/**
	 * Berechnet die besten Features
	 * @param topK wieviele Features geliefert werden sollen
	 * @return
	 * 		sortiert nach Score (beste zuerst in der Liste) alle Features,
	 * 		maximal topK-viele
	 */
	public LinkedList<IdentifiedFeature> getBestFeatures(int topK) {
		//TODO Geordnet nach Scores die besten Features liefern
		return null;
	}
	
	public void saveRefactorings(
			Set<String> affectedFiles,
			LinkedList<RefactoringDocument> modFiles) 
	throws FileNotFoundException, TransformerFactoryConfigurationError, TransformerException {
		for (String file : affectedFiles) {
			backend.removeFileFromList(file);
		}
		
		//TODO Derivative
		for (RefactoringDocument doc : modFiles) {
			String featureName = doc.getFeatureName();
			if (featureName == null) {
				doc.saveDocument(this.outputDir.getAbsolutePath() + File.separatorChar 
						+ "Base" + File.separatorChar + doc.getFileName());
			} else {
				File featureDir = new File(this.outputDir.getAbsolutePath() + File.separatorChar + featureName);
				if (!featureDir.exists()) featureDir.mkdir();
				doc.saveDocument(featureDir.getAbsolutePath() + File.separatorChar + doc.getFileName());
			}
			
		}
		
		initProjectDir();
	}
	
	public void registerVisitors (FeatureVisitor visitor) {
		this.visitors.add(visitor);
	}
	
	public static void main (String[] args) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException, TransformerFactoryConfigurationError, TransformerException {
		FeatureRefactoringAnalyzer a = new FeatureRefactoringAnalyzer(new File("test"));
//		File[] dirlist = (new File("source_mls")).listFiles();
//		
//		for (File file : dirlist) {
//			if (file.getName().matches(".*Stat.*"))	{
//				System.out.println("bla");
//			}
//			boolean test = a.addFile(file);
//			if (test == false) {
//				System.out.println("File " + file.getName() + " not found!");
//				System.exit(0);
//			} else {
//				System.out.println("File " + file.getName() + " added!");
//			}
//		}
		
		Iterator<IdentifiedFeature> it = a.backend.iterate();
		LinkedList<String> featureNames = new LinkedList<String>();
		while (it.hasNext()) {
			featureNames.add(it.next().getName());
		}
		for (String name : featureNames) {
//			String name = "HAVE_BTREE";
			IdentifiedFeature feature = a.backend.getIdentifiedFeatureByName(name);
			if (feature == null) continue;
			System.out.println(name);
			AnalyzeFeature feat = new AnalyzeFeature(feature);
			feat.analyze();
			
			LinkedList<RefactoringDocument> modFiles = feat.refactor();
			a.saveRefactorings(feat.getAnalyzedFeature().getAffectedFiles().keySet(), modFiles);
			
		}
//			IdentifiedFeature feature = it.next();
//			System.out.println("Found Feature: " + feature.getName());
//			System.out.println("LOCS:          " + feature.getLOCs());
//			Iterator<PreprocessorOccurrence> occIt = feature.iterateOccurrences();
//			while (occIt.hasNext()) {
//				PreprocessorOccurrence occ = occIt.next();
//				System.out.println("Occurrence in: " + occ.getDocFileName());
//				System.out.println("recognized Pattern: " + occ.getType());
//				PreprocessorNode[] nodes = occ.getPrepNodes();
//				for (int i = 0; i < nodes.length; i++) {
//					System.out.println("Directive  at: line " 
//							+ nodes[i].getLineNumber()
//							+ " Type: " 
//							+ nodes[i].getType());
//				}
//			}
//		}
//		IdentifiedFeature feat = a.backend.getIdentifiedFeatureByName("DIAGNOSTIC");
//		Iterator<PreprocessorOccurrence> featIt = feat.iterateOccurrences();
//		NewFileTemplate pos = NewFileTemplate.instantiate();
//		NewFileTemplate neg = NewFileTemplate.instantiate();
//		Document doc = null;
//		while(featIt.hasNext()) {
//			PreprocessorOccurrence occ = featIt.next();
//			RefactoringAction action = RefactoringFactory.instance().getAction(occ.getType());
//			doc = occ.getPrepNodes()[0].getNode().getOwnerDocument();
//			action.moveNodes(doc, 
//					pos, neg, occ, feat.getName());
//		}
//		pos.setClassName(doc);
//		neg.setClassName(doc);
//		
//		
//		System.out.println("-----ORIGINAL-----");
//		System.out.println(doc.getFirstChild().getTextContent());
//		System.out.println("---END:ORiGINAL---\n");
//		System.out.println("-------POS--------");
//		System.out.println(pos.getFirstChild().getTextContent());
//		System.out.println("-----END:POS------\n");
//		System.out.println("-------NEG--------");
//		System.out.println(neg.getFirstChild().getTextContent());
//		System.out.println("-----END:NEG------");
	}
}
