package analyzer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import common.NodeTools;
import common.xmlTemplates.NewFileTemplate;

import refactoring.RefactoringAction;
import refactoring.RefactoringDocument;
import refactoring.RefactoringFactory;
import refactoring.RefactoringStrategy;

import backend.storage.IdentifiedFeature;
import backend.storage.PreprocessorOccurrence;

public class AnalyzeFeature {
	private IdentifiedFeature feature;
	private AnalyzedFeature analyzedFeature = null;
	private LinkedList<CriticalOccurrence> crits; //TODO kann lokal werden
	
	
//	 * @deprecated
//	private static String EXPR_NAMES_QUERY = 
//		"((./following::*/src:expr/src:name[1]) " +
//		"intersect " +
//		"(./following-sibling::cpp:endif/preceding::*/src:expr/src:name[1]))";
	
	private static String EXPR_NAMES_BELOW_IF_QUERY = 
		"./(following::src:expr[not(contains(.,\"->\"))]/src:name union following::src:expr/src:name[1])";
	
	private static String EXPR_NAMES_BEFORE_END_QUERY = 
		"./(preceding::src:expr[not(contains(.,\"->\"))]/src:name union preceding::src:expr/src:name[1])";
	
	
//	 * @deprecated
//	private static String EXPR_DECLS_QUERY =
//		"((./following::*/src:decl_stmt/src:decl/src:name) " +
//		"intersect " +
//		"(./following-sibling::cpp:endif/preceding::*/src:decl_stmt/src:decl/src:name))";

	private static String EXPR_DECLS_BELOW_IF_QUERY =
		"./following::src:decl_stmt/src:decl/src:name";

	private static String EXPR_DECLS_BEFORE_END_QUERY =
		"./preceding::src:decl_stmt/src:decl/src:name";

	
	private static String FIND_FUNCTION_CONTAINER =
		"./ancestor::src:function";
	
	private static String FIND_NEXT_GOTOS = 
		"./following::* intersect //src:goto";
	
	private static String FIND_NEXT_DEFINE = 
		"./following::* intersect //cpp:define";
	
	public AnalyzeFeature (IdentifiedFeature feature) {
		this.feature = feature;
	}
	
	public LinkedList<CriticalOccurrence> getCriticalNodes() {
		return crits;
	}
	
	public void analyze() {
		analyzedFeature = new AnalyzedFeature();
		analyzedFeature.setName(feature.getName());
		
		Iterator<PreprocessorOccurrence> it = feature.iterateOccurrences();
		crits = new LinkedList<CriticalOccurrence>();
		
		HashMap<String, HashMap<Node, LinkedList<CriticalOccurrence>>> affectedFiles =
			new HashMap<String, HashMap<Node,LinkedList<CriticalOccurrence>>>();
		analyzedFeature.setAffectedFiles(affectedFiles);
		
		while(it.hasNext()) {
			PreprocessorOccurrence current = it.next();
			if (current.getType().equals("unknown") 
					|| current.getType().equals("HookRefactoring")) {
				CriticalOccurrence occ = new CriticalOccurrence(current);
				boolean isImpossible = false;
				try {
					isImpossible = hasDefines(occ);
					if (!isImpossible) {
						isImpossible = hasGoto(occ);
						if (isImpossible) occ.setType("impossible (local goto)");
					} else {
						occ.setType("impossible (local #define)");
					}
					setContainer(occ);
				} catch (XPathExpressionException e) {
					e.printStackTrace();
				}
				
				if (!isImpossible || occ.getContainingFunctionNode() != null) {
					
					
					HashMap<Node, LinkedList<CriticalOccurrence>> affectedFunctions;
					if (affectedFiles.containsKey(occ.getDocFileName())) {
						affectedFunctions = affectedFiles.get(occ.getDocFileName());
					} else {
						affectedFunctions = new HashMap<Node, LinkedList<CriticalOccurrence>>();
						affectedFiles.put(occ.getDocFileName(), affectedFunctions);
					}
					
					
					LinkedList<CriticalOccurrence> occsInFunction;
					if (affectedFunctions.containsKey(occ.getContainingFunctionNode())) {
						occsInFunction = affectedFunctions.get(occ.getContainingFunctionNode());
					} else {
						occsInFunction = new LinkedList<CriticalOccurrence>();
						affectedFunctions.put(occ.getContainingFunctionNode(), occsInFunction);
					}
					occsInFunction.add(occ);
				} else {
					occ.setType("impossible");
					//TODO: DEBUG
//					System.out.println("_________");
//					System.out.println(occ.getDocFileName());
//					System.out.println("impossible Occ: " + occ.getPrepNodes()[0].getLineNumber());
				}
				if (!occ.getType().startsWith("impossible") && !occ.getType().startsWith("unknown")) {
					
					try {
						occ.setCritNodeType(RefactoringStrategy.SIMPLE_HOOK);
						checkDependencies(occ);
//						//TODO DEBUG
//						if (occ.getLocalVariableDependencies().size() == 0 && occ.getParameterDependencies().size() == 0) {
//							System.out.println("no dependencies!");
//						} else {
//							System.out.println(occ.getLocalVariableDependencies().size() + " local/" + occ.getParameterDependencies().size() + " parameter dependencies");
//						}
//						if (occ.getOwnLocalVariables().size() > 0) {
//							System.out.println("own declarations: " + occ.getOwnLocalVariables().size());
//						}
					} catch (XPathExpressionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				crits.add(occ);
				analyzedFeature.addOccurrence(occ);
			} else {
				analyzedFeature.addOccurrence(current);
			}
		}
	}
	
	private boolean hasDefines(CriticalOccurrence occ) throws XPathExpressionException {
		XPathExpression expr = 
			QueryBuilder.instance().getExpression(FIND_NEXT_DEFINE);
		
		NodeList upper = (NodeList) expr.evaluate(
				occ.getPrepNodes()[0].getNode(), XPathConstants.NODESET);
		NodeList lower = (NodeList) expr.evaluate(
				occ.getPrepNodes()[occ.getPrepNodes().length-1].getNode(), XPathConstants.NODESET);
	
		return lower.item(0) != upper.item(0);
	}
	
	private boolean hasGoto(CriticalOccurrence occ) throws XPathExpressionException {
		XPathExpression expr = 
			QueryBuilder.instance().getExpression(FIND_NEXT_GOTOS);
		
		NodeList upper = (NodeList) expr.evaluate(
				occ.getPrepNodes()[0].getNode(), XPathConstants.NODESET);
		NodeList lower = (NodeList) expr.evaluate(
				occ.getPrepNodes()[occ.getPrepNodes().length-1].getNode(), XPathConstants.NODESET);
	
		return lower.getLength() != 0 && upper.getLength() != 0 && lower.item(0) != upper.item(0);
	}

	private void setContainer(CriticalOccurrence occ) throws XPathExpressionException {
		String[] functionCheck = new String[occ.getPrepNodes().length];
		Node functionNode = null;
		for (int i = 0; i < occ.getPrepNodes().length; i++) {
			XPathExpression expr = 
				QueryBuilder.instance().getExpression(FIND_FUNCTION_CONTAINER);
			functionNode = (Node) expr.evaluate(
					occ.getPrepNodes()[i].getNode(), XPathConstants.NODE);
			if (functionNode == null) {
				//TODO DEBUG (Pattern noch unbekannt?!)
				System.out.println("Null-Function? " + occ.getDocFileName() + " at line " + occ.getPrepNodes()[i].getLineNumber());
				continue;
			} else {
				Node functionChild = functionNode.getFirstChild();
				StringBuilder functionString = new StringBuilder("");
				while (!functionChild.getNodeName().equalsIgnoreCase("block") 
						&& functionChild != null) {
					functionString.append(functionChild.getTextContent());
					functionChild = functionChild.getNextSibling();
				}
				functionCheck[i] = functionString.toString();
			}
		}
		
		boolean allNodesInSameFunction = functionCheck.length > 1;
		for (int i = 1; i < functionCheck.length; i++) {
			allNodesInSameFunction &= functionCheck[i] != null && functionCheck[0].equals(functionCheck[i]);
		}
		
		if (allNodesInSameFunction) {
			occ.setContainingFunctionNode(functionNode);
//			//TODO DEBUG
//			System.out.println("_________");
//			System.out.println(occ.getDocFileName());
//			System.out.println(occ.getPrepNodes()[0].getLineNumber());
//			System.out.println(occ.getType());
//			System.out.println(functionCheck[0]);
//		} else {
//			System.out.println("_________");
//			System.out.println(occ.getDocFileName());
//			System.out.println(occ.getType());
//			System.out.println("dismatch lines: " + occ.getPrepNodes()[0].getLineNumber() + " " + occ.getPrepNodes()[occ.getPrepNodes().length-1].getLineNumber());
//			//END DEBUG
		}
		
	}

	private void checkDependencies(CriticalOccurrence occ) 
	throws XPathExpressionException {
		
		//Hack für Schnittmengenbildung zwischen allen Vars, 
		//die nach ifdef und vor endif auftreten
		XPathExpression varUsageBelowIfExpr = 
			QueryBuilder.instance().getExpression(EXPR_NAMES_BELOW_IF_QUERY);
		XPathExpression varUsageBeforeEndExpr = 
			QueryBuilder.instance().getExpression(EXPR_NAMES_BEFORE_END_QUERY);
		NodeList belowIfList = 
			(NodeList) varUsageBelowIfExpr.evaluate(
					occ.getPrepNodes()[0].getNode(), XPathConstants.NODESET);
		NodeList beforeEndList = 
			(NodeList) varUsageBeforeEndExpr.evaluate(
					occ.getPrepNodes()[occ.getPrepNodes().length-1].getNode(), 
					XPathConstants.NODESET);
		
		NodeList exprList = NodeTools.intersectUpperLower(belowIfList, beforeEndList);
		//-------------------------------------------------
		
		//gleiches nochmal für die lokalen Definitionen von Vars
		XPathExpression varDeclBelowIfExpr = 
			QueryBuilder.instance().getExpression(EXPR_DECLS_BELOW_IF_QUERY);
		XPathExpression varDeclBeforeEndExpr = 
			QueryBuilder.instance().getExpression(EXPR_DECLS_BEFORE_END_QUERY);
		NodeList declBelowIfList = 
			(NodeList) varDeclBelowIfExpr.evaluate(
					occ.getPrepNodes()[0].getNode(), XPathConstants.NODESET);
		NodeList declBeforeEndList = 
			(NodeList) varDeclBeforeEndExpr.evaluate(
					occ.getPrepNodes()[occ.getPrepNodes().length-1].getNode(), 
					XPathConstants.NODESET);
		NodeList declList = NodeTools.intersectUpperLower(declBelowIfList, declBeforeEndList);
		if (declList.getLength() > 0) {
			occ.setType("impossible (local declaration)");
		}
		//-------------------------------------------------
		
		LinkedList<String> ownLocalVariables = new LinkedList<String>();
		HashSet<String> declNames = new HashSet<String>();
		for (int i = 0; i < declList.getLength(); i++) {
			declNames.add(declList.item(i).getTextContent());
			ownLocalVariables.add(declList.item(i).getTextContent());
		}
		
		LinkedList<String> localVariableDependencies = new LinkedList<String>();
		LinkedList<String> parameterDependencies = new LinkedList<String>();
		HashSet<String> duplicateEliminator = new HashSet<String>();
		for (int i = 0; i < exprList.getLength(); i++) {
			String varName = exprList.item(i).getTextContent();
			//TODO: check, ob Variable von einer andren Occurrence definiert wurde!
			if (declNames.contains(varName)) {
				//selbstdefinierte Variable
				occ.setType("impossible (local declaration)");
			} else if (!duplicateEliminator.contains(varName) &&
					isLocalVar(occ.getPrepNodes()[0].getNode(), varName)) {
				localVariableDependencies.add(varName);
				duplicateEliminator.add(varName);
			} else if (!duplicateEliminator.contains(varName) &&
					isParameter(occ.getPrepNodes()[0].getNode(), varName)) {
				parameterDependencies.add(varName);
				duplicateEliminator.add(varName);
			}
		}
		occ.setLocalVariableDependencies(localVariableDependencies);
		occ.setParameterDependencies(parameterDependencies);
		occ.setOwnLocalVariables(ownLocalVariables);
		
	}
	
	private boolean isLocalVar(Node ctxNode, String varName) {
		boolean result = false;
		try {
			XPathExpression expr = QueryBuilder.instance().getExpression(
					"./ancestor::src:function/src:block/" +
					"descendant::src:decl_stmt/src:decl[src:name = \"" + 
					varName + "\"]");
			result = ((NodeList)expr.evaluate(ctxNode, XPathConstants.NODESET)).getLength() > 0;
		} catch (XPathExpressionException e) {
			// kann nicht auftreten, Query ist getestet
			e.printStackTrace();
		}
		return result;
	}
	
	private boolean isParameter(Node ctxNode, String varName) {
		boolean result = false;
		try {
			XPathExpression expr = QueryBuilder.instance().getExpression(
					"./ancestor::src:function/src:parameter_list/src:param/" +
					"src:decl[src:name=\"" + varName + "\"]");
			result = 
				((NodeList)expr.evaluate(
						ctxNode, XPathConstants.NODESET)).getLength() > 0;
		} catch (XPathExpressionException e) {
			// kann nicht auftreten, Query ist getestet
			e.printStackTrace();
		}
		return result;
		
	}
	
	
	public LinkedList<RefactoringDocument> refactor() 
	throws SAXException, IOException, ParserConfigurationException {
		if (analyzedFeature == null) {
			analyze();
		}
		HashMap<String, RefactoringDocument> posfiles = 
			new HashMap<String, RefactoringDocument>();
		HashMap<String, RefactoringDocument> negfiles = 
			new HashMap<String, RefactoringDocument>();
		Iterator<PreprocessorOccurrence> it = analyzedFeature.iterateOccurrences();
		
		LinkedList<RefactoringDocument> modifiedFiles = new LinkedList<RefactoringDocument>();
		setHookNames();
		while (it.hasNext()) {
			
			
			PreprocessorOccurrence occ = it.next();
			RefactoringAction action = RefactoringFactory.instance().getAction(occ.getType());
			if (action != null) {
				RefactoringDocument pos;
				RefactoringDocument neg;
				if (posfiles.containsKey(occ.getDocFileName())) {
					pos = posfiles.get(occ.getDocFileName());
				} else {
					pos = NewFileTemplate.instantiate();
					posfiles.put(occ.getDocFileName(), pos);
					try {
						((NewFileTemplate)pos).setClassName(occ.getDocument());
					} catch (XPathExpressionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (negfiles.containsKey(occ.getDocFileName())) {
					neg = negfiles.get(occ.getDocFileName());
				} else {
					neg = NewFileTemplate.instantiate();
					negfiles.put(occ.getDocFileName(), neg);
					try {
						((NewFileTemplate)neg).setClassName(occ.getDocument());
					} catch (XPathExpressionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				action.doRefactoring(occ.getDocument(), 
						pos, neg, 
						occ, analyzedFeature.getName());
//				System.out.println(occ.getDocFileName());
				String docName = (new File(occ.getDocFileName())).getName();
				if (pos.getModified() || neg.getModified()) {
					RefactoringDocument doc = new RefactoringDocument(occ.getDocument());
					modifiedFiles.add(doc);
					doc.setFileName(docName);
				}
				if (pos.getModified()) {
					modifiedFiles.add(pos);
					pos.setFileName(docName);
				}
				if (neg.getModified()) {
					modifiedFiles.add(neg);
					neg.setFileName(docName);
				}
			}
		}
		
		
		return modifiedFiles;
	}
	
	private void setHookNames() {
		HashMap<String, HashMap<Node, LinkedList<CriticalOccurrence>>> files = 
			analyzedFeature.getAffectedFiles();
		
		for (String key : files.keySet()) {
			int counter = 0;
			for (Node node : files.get(key).keySet()) {
				for (CriticalOccurrence occ : files.get(key).get(node)) {
					//TODO String-Konstante herausziehen
					if (occ.getType().equals("HookRefactoring") 
							&& (occ.getHookFunctionName() == null
							|| occ.getHookFunctionName().length() == 0)
							&& (occ.getCritNodeType() == RefactoringStrategy.SIMPLE_HOOK
							|| occ.getCritNodeType() == RefactoringStrategy.BLOCK_REPLICATION_WITH_HOOK
							|| occ.getCritNodeType() == RefactoringStrategy.STATEMENT_REPLICATION_WITH_HOOK)) {
						counter++;
						String name = key.substring(key.lastIndexOf('\\')+1);
						name = name.substring(0, name.indexOf('.'));
						
						occ.setHookFunctionName(name + "HookFunction" + counter);
					}
				}
			}
		}
	}
	
	public AnalyzedFeature getAnalyzedFeature() {
		return this.analyzedFeature;
	}

}
