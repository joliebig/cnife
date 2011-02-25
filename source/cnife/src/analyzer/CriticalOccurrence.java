package analyzer;

import java.io.File;
import java.util.LinkedList;

import org.w3c.dom.Node;

import common.xmlTemplates.FunctionBuilder;

import refactoring.RefactoringStrategy;

import backend.storage.PreprocessorOccurrence;

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
		return hookBuilder;
	}

	public void setHookBuilder(FunctionBuilder hookBuilder) {
		this.hookBuilder = hookBuilder;
	}

	public CriticalOccurrence getDupe() {
		return dupe;
	}

	public void setDupe(CriticalOccurrence dupe) {
		this.dupe = dupe;
	}

	public CriticalOccurrence(PreprocessorOccurrence current) {
		this.setDocFileName(new File(current.getDocFileName()));
		this.setPrepNodes(current.getPrepNodes());
		this.setType(current.getType());
	}

	public LinkedList<String> getLocalVariableDependencies() {
		return localVariableDependencies;
	}

	public void setLocalVariableDependencies(
			LinkedList<String> localVariableDependencies) {
		this.localVariableDependencies = localVariableDependencies;
	}

	public LinkedList<String> getParameterDependencies() {
		return parameterDependencies;
	}

	public void setParameterDependencies(LinkedList<String> parameterDependencies) {
		this.parameterDependencies = parameterDependencies;
	}

	public LinkedList<String> getOwnLocalVariables() {
		return ownLocalVariables;
	}

	public void setOwnLocalVariables(LinkedList<String> ownLocalVariables) {
		this.ownLocalVariables = ownLocalVariables;
	}

	public String getHookFunctionName() {
		return hookFunctionName;
	}

	public void setHookFunctionName(String hookFunctionName) {
		this.hookFunctionName = hookFunctionName;
	}

	public void setCritNodeType(RefactoringStrategy critNodeType) {
		this.critNodeType = critNodeType;
	}

	public RefactoringStrategy getCritNodeType() {
		return critNodeType;
	}
	
	public Node getContainingFunctionNode() {
		return containingFunctionNode;
	}

	public void setContainingFunctionNode(Node containingFunctionNode) {
		this.containingFunctionNode = containingFunctionNode;
	}
}
