package refactoring.actions;

import analyzer.CriticalOccurrence;
import backend.PreprocessorNode;
import backend.storage.PreprocessorOccurrence;
import common.NodeTools;
import common.QueryBuilder;
import common.xmlTemplates.FunctionBuilder;
import java.util.LinkedList;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import refactoring.RefactoringAction;
import refactoring.RefactoringDocument;

public class HookRefactoring extends RefactoringAction {
	private static String FIND_FUNCTION_CONTAINER = "./(ancestor::src:function union ancestor::src:constructor)";

	protected void doRefactoring(Document from, RefactoringDocument pos,
			RefactoringDocument neg, PreprocessorOccurrence occ) {
		CriticalOccurrence critOcc = null;
		if ((occ instanceof CriticalOccurrence)) {
			critOcc = (CriticalOccurrence) occ;
			switch (critOcc.getCritNodeType()) {
			case SIMPLE_HOOK:
				if (!NodeTools.haveSameParents(
						critOcc.getPrepNodes()[0].getNode(),
						critOcc.getPrepNodes()[1].getNode()))
					break;
				if ((critOcc.getPrepNodes().length >= 2)
						&& (!NodeTools.haveSameParents(
								critOcc.getPrepNodes()[0].getNode(),
								critOcc.getPrepNodes()[1].getNode())))
					break;
				doRefactoring(from, pos, neg, critOcc);

				break;
			}
		}
	}

	protected void doRefactoring(Document from, RefactoringDocument pos,
			RefactoringDocument neg, CriticalOccurrence occ) {
		PreprocessorNode[] nodes = occ.getPrepNodes();
		FunctionBuilder fb = null;
		if ((occ.getDupe() != null) && (occ.getDupe().getHookBuilder() != null)) {
			fb = occ.getDupe().getHookBuilder();
			insertDupeHookCall(nodes, from, fb);
		} else {
			boolean positive = false;
			if (!nodes[0].getNode().getNodeName().startsWith("cpp:ifn")) {
				fb = extractHook(nodes[0], nodes[1], from, pos,
						this.featureName, occ.getHookFunctionName(),
						occ.getLocalVariableDependencies(),
						occ.getParameterDependencies());
				positive = true;
				pos.setModified(true);
			} else {
				fb = extractHook(nodes[0], nodes[1], from, neg,
						this.negFeatureName, occ.getHookFunctionName(),
						occ.getLocalVariableDependencies(),
						occ.getParameterDependencies());
				neg.setModified(true);
			}
			if ((nodes.length > 2) && (positive)) {
				fb = extractHook(nodes[1], nodes[2], from, neg,
						this.negFeatureName, occ.getHookFunctionName(),
						occ.getLocalVariableDependencies(),
						occ.getParameterDependencies());
				neg.setModified(true);
			} else if (nodes.length > 2) {
				fb = extractHook(nodes[1], nodes[2], from, pos,
						this.featureName, occ.getHookFunctionName(),
						occ.getLocalVariableDependencies(),
						occ.getParameterDependencies());
				pos.setModified(true);
			}
		}
		for (int i = 0; i < nodes.length; i++) {
			nodes[i].getNode().getParentNode().removeChild(nodes[i].getNode());
		}
		if (occ.getDupe() == null)
			occ.setHookBuilder(fb);
		else if (occ.getDupe().getHookBuilder() == null)
			occ.getDupe().setHookBuilder(fb);
	}

	private void insertDupeHookCall(PreprocessorNode[] nodes, Document from,
			FunctionBuilder fb) {
		System.out.println("dup call");
		Node functionCallNode = fb.buildCallNode();
		PreprocessorNode start = nodes[0];
		Node importedCall = start.getNode().getOwnerDocument()
				.importNode(functionCallNode, true);
		start.getNode().getParentNode()
				.insertBefore(importedCall, start.getNode());

		Node currNode = start.getNode().getNextSibling();

		while (currNode != nodes[1].getNode()) {
			Node next = currNode.getNextSibling();
			currNode.getParentNode().removeChild(currNode);
			currNode = next;
		}
		if (nodes.length > 2) {
			currNode = currNode.getNextSibling();
			while (currNode != nodes[2].getNode()) {
				Node next = currNode.getNextSibling();
				currNode.getParentNode().removeChild(currNode);
				currNode = next;
			}
		}
	}

	private FunctionBuilder extractHook(PreprocessorNode start,
			PreprocessorNode end, Document from, Document target,
			String featureName, String hookName, LinkedList<String> localDeps,
			LinkedList<String> paramDeps) {
		FunctionBuilder fb = new FunctionBuilder();
		fb.setFunctionName(hookName);
		try {
			if ((localDeps != null) && (localDeps.size() > 0)) {
				setLocalDeps(start.getNode(), localDeps, fb);
			}
			if ((paramDeps != null) && (paramDeps.size() > 0)) {
				setParamDeps(start.getNode(), paramDeps, fb);
			}

			XPathExpression expr = QueryBuilder.instance().getExpression(
					FIND_FUNCTION_CONTAINER);
			Node functionNode = (Node) expr.evaluate(start.getNode(),
					XPathConstants.NODE);
			Node functionDeclNode = fb.buildDeclarationNode();
			Node functionCallNode = fb.buildCallNode();
			Node importedDecl = start.getNode().getOwnerDocument()
					.importNode(functionDeclNode, true);
			Node importedCall = start.getNode().getOwnerDocument()
					.importNode(functionCallNode, true);
			functionNode.getParentNode().insertBefore(importedDecl,
					functionNode);
			start.getNode().getParentNode()
					.insertBefore(importedCall, start.getNode());

			DocumentFragment frag = from.createDocumentFragment();
			extractNodes(start, end, frag);

			expr = QueryBuilder.instance().getExpression(
					"./src:block/src:comment[text()=\"//--functionbody\"]");
			Node entryPoint = (Node) expr.evaluate(functionDeclNode,
					XPathConstants.NODE);

			NodeList childs = frag.getChildNodes();
			for (int i = 0; i < childs.getLength(); i++) {
				Node child = childs.item(i).cloneNode(true);
				Node adoptedNode = functionDeclNode.getOwnerDocument()
						.adoptNode(child);
				entryPoint.getParentNode()
						.insertBefore(adoptedNode, entryPoint);
			}
			expr = QueryBuilder
					.instance()
					.getExpression(
							"/src:unit/src:class/src:block/src:comment[text()=\"//---refactored functions\"]");
			entryPoint = (Node) expr.evaluate(target, XPathConstants.NODE);
			Node clone = target.adoptNode(functionDeclNode.cloneNode(true));
			Text separator = target.createTextNode("\n");
			entryPoint.getParentNode().insertBefore(separator,
					entryPoint.getNextSibling());
			entryPoint.getParentNode().insertBefore(clone,
					entryPoint.getNextSibling());
			entryPoint.getParentNode().insertBefore(separator.cloneNode(true),
					entryPoint.getNextSibling());
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}

		return fb;
	}

	private void setLocalDeps(Node ref, LinkedList<String> localDeps,
			FunctionBuilder fb) throws XPathExpressionException {
		for (String varName : localDeps) {
			XPathExpression nameExpr = QueryBuilder
					.instance()
					.getExpression(
							"./ancestor::src:function/src:block/descendant::src:decl_stmt/src:decl[src:name = \""
									+ varName
									+ "\"]/src:name[text()=\""
									+ varName + "\"]");
			XPathExpression typeExpr = QueryBuilder
					.instance()
					.getExpression(
							"./ancestor::src:function/src:block/descendant::src:decl_stmt/src:decl[src:name = \""
									+ varName + "\"]/src:type");

			Node nameNode = (Node) nameExpr.evaluate(ref, XPathConstants.NODE);
			Node typeNode = (Node) typeExpr.evaluate(ref, XPathConstants.NODE);

			fb.appendParameter(typeNode, nameNode);
		}
	}

	private void setParamDeps(Node ref, LinkedList<String> paramDeps,
			FunctionBuilder fb) throws XPathExpressionException {
		for (String varName : paramDeps) {
			XPathExpression nameExpr = QueryBuilder.instance().getExpression(
					"./ancestor::src:function/src:parameter_list/src:param/src:decl[src:name=\""
							+ varName + "\"]/src:name");
			XPathExpression typeExpr = QueryBuilder.instance().getExpression(
					"./ancestor::src:function/src:parameter_list/src:param/src:decl[src:name=\""
							+ varName + "\"]/src:type");

			Node nameNode = (Node) nameExpr.evaluate(ref, XPathConstants.NODE);
			Node typeNode = (Node) typeExpr.evaluate(ref, XPathConstants.NODE);

			fb.appendParameter(typeNode, nameNode);
		}
	}

	private void extractNodes(PreprocessorNode start, PreprocessorNode end,
			DocumentFragment frag) {
		Node current = start.getNode().getNextSibling();
		while (current != end.getNode())
			while ((current != end.getNode()) && (current != null)) {
				Node next = current.getNextSibling();
				frag.appendChild(current);
				current = next;
			}
	}
}
