package scanner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import scanner.patterns.ClassIntroductionPattern;
import scanner.patterns.CompleteFunctionPattern;
import scanner.patterns.CompletePrivatePublicBlock;
import scanner.patterns.DirectivesPattern;
import scanner.patterns.InFunctionOccurrencesPattern;
import scanner.patterns.SwitchCasePattern;

import net.sf.saxon.AugmentedSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.sxpath.IndependentContext;
import net.sf.saxon.sxpath.XPathEvaluator;
import net.sf.saxon.sxpath.XPathExpression;
import net.sf.saxon.trans.XPathException;

import analyzer.QueryBuilder;
import backend.PreprocessorNode;
import backend.storage.IdentifiedFeatureList;
import backend.storage.PreprocessorOccurrence;

public class FeatureSearcher {

	private static final String ALL_RELEVANT_DIRECTIVES_XPATH = "//(cpp:if union cpp:ifdef union cpp:ifndef union cpp:else union cpp:elif union cpp:endif)";
	private File outputDir = null;
	private IdentifiedFeatureList backend = null;
	private LinkedList<String> annotationfilter;

	/**
	 * adds a new xml file to the backend and creates a copy in folder base
	 * 
	 * @param xmlDoc
	 * @throws ReplaceFileException thrown if created file already exists in base
	 */
	public void importExternalFile(File xmlDoc) throws ReplaceFileException {

		FileOutputStream out = null;
		try {
			Document doc = parseDocument(xmlDoc);
			if (outputDir != null) {
				File baseFile = new File(outputDir.getAbsolutePath()
						+ File.separatorChar + "Base" + File.separatorChar
						+ xmlDoc.getName());
				if (baseFile.exists())
					throw new ReplaceFileException();
				Transformer transformer = TransformerFactory.newInstance()
						.newTransformer();
				DOMSource src = new DOMSource(doc);
				out = new FileOutputStream(baseFile);
				StreamResult res = new StreamResult(out);
				transformer.transform(src, res);
				xmlDoc = baseFile;
			}
			checkDoc(xmlDoc, doc);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} finally {
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	public void checkDoc(File xmlDoc) {
		if (!backend.isKnown(xmlDoc.getAbsolutePath()))
			checkDoc(xmlDoc, null);
	}

	private void checkDoc(File xmlDoc, Document doc) {

		PreprocessorTree tree = buildPrepTreeWithLineNumbers(xmlDoc);

		try {
			if (doc == null) {
				doc = parseDocument(xmlDoc);
			}
			populateTree(doc, tree);
			tree.calculateDepths();

			// analyzed patterns
			FeaturePattern pattern1 = new CompletePrivatePublicBlock();
			FeaturePattern pattern2 = new CompleteFunctionPattern();
			FeaturePattern pattern3 = new InFunctionOccurrencesPattern();
			FeaturePattern pattern4 = new ClassIntroductionPattern();
			FeaturePattern pattern5 = new DirectivesPattern();
			FeaturePattern pattern6 = new SwitchCasePattern();

			LinkedList<PreprocessorOccurrence> occs = new LinkedList<PreprocessorOccurrence>();

			// ordering does matter
			if (annotationfilter == null
					|| annotationfilter.contains("SwitchCasePattern"))
				occs.addAll(pattern6.checkDoc(doc, tree));
			if (annotationfilter == null
					|| annotationfilter.contains("CompletePrivatePublicBlockPattern"))
				occs.addAll(pattern1.checkDoc(doc, tree));
			if (annotationfilter == null
					|| annotationfilter.contains("ClassIntroductionPattern"))
				occs.addAll(pattern4.checkDoc(doc, tree));
			if (annotationfilter == null
					|| annotationfilter.contains("DirectivesPattern"))
				occs.addAll(pattern5.checkDoc(doc, tree));
			if (annotationfilter == null
					|| annotationfilter.contains("CompleteFunctionPattern"))
				occs.addAll(pattern2.checkDoc(doc, tree));
			if (annotationfilter == null
					|| annotationfilter
							.contains("InFunctionOccurrencesPattern"))
				occs.addAll(pattern3.checkDoc(doc, tree));

			// pick up the remaining patterns
			FeaturePattern remains = new RemainingOccurrencesPattern();
			occs.addAll(remains.checkDoc(doc, tree));

			for (PreprocessorOccurrence occ : occs) {
				occ.setDocFileName(xmlDoc);
				addToBackend(occ);
			}

		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		}
	}

	/**
	 * parses an xml file and returns a document object
	 * 
	 * @param xmlDoc
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public Document parseDocument(File xmlDoc) throws SAXException,
			IOException, ParserConfigurationException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		Document doc = factory.newDocumentBuilder().parse(xmlDoc);
		return doc;
	}

	/**
	 * adds dom-nodes to an exisiting preprocessortree
	 * 
	 * @param doc
	 * @param tree
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws XPathExpressionException
	 */
	public void populateTree(Document doc, PreprocessorTree tree)
			throws SAXException, IOException, ParserConfigurationException,
			XPathExpressionException {

		javax.xml.xpath.XPathExpression expr = QueryBuilder.instance()
				.getExpression(ALL_RELEVANT_DIRECTIVES_XPATH);
		NodeList result = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
		tree.populate(result);
	}

	/**
	 * creates a {@link PreprocessorTree} of the srcml document with line
	 * numbers
	 * 
	 * @param xmlDoc
	 *            -- srcml document
	 * @return created PreprocessorTree
	 */
	public PreprocessorTree buildPrepTreeWithLineNumbers(File xmlDoc) {
		AugmentedSource src = AugmentedSource
				.makeAugmentedSource(new StreamSource(xmlDoc));
		src.setLineNumbering(true);
		Configuration conf = new Configuration();
		conf.setLineNumbering(true);
		XPathEvaluator evaluator = new XPathEvaluator(conf);
		List<?> resultNodes = null;

		try {
			DocumentInfo doc = conf.buildDocument(src);

			IndependentContext ctx = new IndependentContext(
					doc.getConfiguration());
			ctx.declareNamespace(NameSpaceConfigs.CPP_PREFIX,
					NameSpaceConfigs.CPP_NS_URI);
			evaluator.setStaticContext(ctx);

			XPathExpression expr = evaluator
					.createExpression(ALL_RELEVANT_DIRECTIVES_XPATH);
			resultNodes = expr.evaluate(src);

		} catch (XPathException e) {
			e.printStackTrace();
		}

		PreprocessorTree tree = new PreprocessorTree();

		for (Object cont : resultNodes) {
			if (cont instanceof NodeInfo) {
				PreprocessorNode node = new PreprocessorNode();
				NodeInfo nI = (NodeInfo) cont;
				node.setLineNumber(nI.getLineNumber());
				node.setType(nI.getLocalPart());
				tree.add(node);
			}
		}
		src.close();
		return tree;
	}

	/**
	 * adds a {@link PreprocessorOccurrence} to the backend
	 * 
	 * @param occurrence
	 */
	private void addToBackend(PreprocessorOccurrence occurrence) {
		String featureName = new CheckLogics().getFeatureName(occurrence);
		backend.add(featureName, occurrence);
	}

	/**
	 * set the output directory
	 * 
	 * @param outputDir
	 */
	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

	/**
	 * set the backend for the FeatureSearcher class
	 * 
	 * @param backend
	 */
	public void setBackend(IdentifiedFeatureList backend) {
		this.backend = backend;
	}

	public void setAnnotationFilter(LinkedList<String> annotationfilter) {
		this.annotationfilter = annotationfilter;
	}

	// test document should be a unit test or something
	// public static void main (String[] args) {
	// FeatureSearcher fs = new FeatureSearcher();
	//
	// fs.checkDoc(new File(".\\HashOpen.xml"));
	// }

}
