package analyzer;

import backend.storage.PreprocessorOccurrence;
import common.xmlTemplates.FunctionBuilder;
import java.io.File;
import java.util.LinkedList;
import org.w3c.dom.Node;
import refactoring.RefactoringStrategy;

public class CriticalOccurrence extends PreprocessorOccurrence {
	private RefactoringStrategy critNodeType;
	private Node containingFunctionNode;
	private LinkedList<String> localVariableDependencies;
	private LinkedList<String> parameterDependencies;
	private LinkedList<String> ownLocalVariables;
	private String hookFunctionName;
	private CriticalOccurrence dupe;
	private FunctionBuilder hookBuilder;

	public FunctionBuilder getHookBuilder() {
		return this.hookBuilder;
	}

	public void setHookBuilder(FunctionBuilder hookBuilder) {
		this.hookBuilder = hookBuilder;
	}

	public CriticalOccurrence getDupe() {
		return this.dupe;
	}

	public void setDupe(CriticalOccurrence dupe) {
		this.dupe = dupe;
	}

	public CriticalOccurrence(PreprocessorOccurrence current) {
		setDocFileName(new File(current.getDocFileName()));
		setPrepNodes(current.getPrepNodes());
		setType(current.getType());
	}

	public LinkedList<String> getLocalVariableDependencies() {
		return this.localVariableDependencies;
	}

	public void setLocalVariableDependencies(
			LinkedList<String> localVariableDependencies) {
		this.localVariableDependencies = localVariableDependencies;
	}

	public LinkedList<String> getParameterDependencies() {
		return this.parameterDependencies;
	}

	public void setParameterDependencies(
			LinkedList<String> parameterDependencies) {
		this.parameterDependencies = parameterDependencies;
	}

	public LinkedList<String> getOwnLocalVariables() {
		return this.ownLocalVariables;
	}

	public void setOwnLocalVariables(LinkedList<String> ownLocalVariables) {
		this.ownLocalVariables = ownLocalVariables;
	}

	public String getHookFunctionName() {
		return this.hookFunctionName;
	}

	public void setHookFunctionName(String hookFunctionName) {
		this.hookFunctionName = hookFunctionName;
	}

	public void setCritNodeType(RefactoringStrategy critNodeType) {
		this.critNodeType = critNodeType;
	}

	public RefactoringStrategy getCritNodeType() {
		return this.critNodeType;
	}

	public Node getContainingFunctionNode() {
		return this.containingFunctionNode;
	}

	public void setContainingFunctionNode(Node containingFunctionNode) {
		this.containingFunctionNode = containingFunctionNode;
	}
}
