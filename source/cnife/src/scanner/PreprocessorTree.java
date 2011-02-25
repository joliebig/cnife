package scanner;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.NodeList;

import backend.PreprocessorNode;


public class PreprocessorTree {
	
	LinkedList<NodeHolder> nodeHolders;
	
	public PreprocessorTree() {
		nodeHolders = new LinkedList<NodeHolder>();
	}
	
	public LinkedList<NodeHolder> getDirectChildren() {
		return nodeHolders;
	}
	
	/**
	 * prüft den übergebenen Tree auf in diesem Baum enthaltene Patterns
	 * gefundene Patterns werden in diesem Baum als 'gefunden' markiert,
	 * während nicht gefundene Patterns aus dem übergebenen Baum
	 * gelöscht werden
	 * @param patternTree
	 * @return
	 */
	public boolean calculateMatches(PreprocessorTree patternTree) {
		List<NodeHolder> patternNodeHolders = patternTree.getDFSList();
		List<NodeHolder> completeNodeHolders = this.getDFSList();
		
		for (NodeHolder pHolder : patternNodeHolders) {
			if (pHolder.ifDirective != null && pHolder.endDirective != null) {
				for (NodeHolder cHolder : completeNodeHolders) {
					if (!cHolder.patternMark && pHolder.compareDirectives(cHolder)) {
						/*/TODO DEBUG
						System.out.println("Match at " + cHolder.ifDirective.getLineNumber());
						//TODO DEBUG END */
						cHolder.patternMark = true;
						pHolder.patternMark = true;
						pHolder.ifDirective.setLineNumber(cHolder.ifDirective.getLineNumber());
						pHolder.depth = cHolder.depth;
						if (pHolder.elseDirective != null) {
							pHolder.elseDirective.setLineNumber(cHolder.elseDirective.getLineNumber());
						}
						pHolder.endDirective.setLineNumber(cHolder.endDirective.getLineNumber());
					}
				}
			}
		}
		
		patternTree.removeUnmarkedOccurrences();
		/*/TODO DEBUG
		List<PreprocessorNode> list = patternTree.flattenTree();
		for (PreprocessorNode node : list) {
			System.out.println(node.getLineNumber() + " " + node.getNode().getLocalName());
		}
		//TODO DEBUG END */
		return true;
	}
	
	/**
	 * loescht alle unmarkierten Knoten aus dem Baum
	 */
	public void removeUnmarkedOccurrences() {
		PreprocessorTree newTree = new PreprocessorTree();
		buildUpCleanTree(newTree, true);
		this.nodeHolders = newTree.nodeHolders;
	}

	/**
	 * loescht alle markierten Knoten aus dem Baum
	 */
	public void removeMarkedOccurrences() {
		PreprocessorTree newTree = new PreprocessorTree();
		buildUpCleanTree(newTree, false);
		this.nodeHolders = newTree.nodeHolders;
	}
	
	/**
	 * befuellt einen neuen Baum mit entweder nur den markierten Knoten aus 
	 * diesem Baum oder nur mit den unmarkierten 
	 * @param tree
	 * 			der neue Baum, der dann die spezifizierten Knoten enthalten soll
	 * @param markedNodesOnly
	 * 			wenn true, dann werden alle markierten Knoten nach tree verschoben
	 * 			wenn false, dann werden alle unmarkierten Knoten nach tree verschoben
	 */
	private void buildUpCleanTree(PreprocessorTree tree, boolean markedNodesOnly) {
		for (NodeHolder holder : nodeHolders) {
			if (holder.patternMark == markedNodesOnly) {
				tree.add(holder.ifDirective);
				if (holder.betweenIf != null) {
					holder.betweenIf.buildUpCleanTree(tree, markedNodesOnly);
				}
				if (holder.elseDirective != null) {
					tree.add(holder.elseDirective);
					if (holder.betweenElse != null) {
						holder.betweenElse.buildUpCleanTree(tree, markedNodesOnly);
					}
				}
				if (holder.endDirective != null) {
					tree.add(holder.endDirective);
				}
			} else {
				if (holder.betweenIf != null) {
					holder.betweenIf.buildUpCleanTree(tree, markedNodesOnly);
				}
				if (holder.betweenElse != null) {
					holder.betweenElse.buildUpCleanTree(tree, markedNodesOnly);
				}
			}
		}
	}

	public List<PreprocessorNode> getOccurrenceSequence() {
		List<NodeHolder> occs = this.getDFSList();
		LinkedList<PreprocessorNode> result = new LinkedList<PreprocessorNode>();
		
		for (NodeHolder holder : occs) {
			holder.ifDirective.setDepth(holder.depth);
			result.addLast(holder.ifDirective); 
			if (holder.elseDirective != null) {
				holder.elseDirective.setDepth(holder.depth);
				result.addLast(holder.elseDirective); 
			}
			holder.endDirective.setDepth(holder.depth);
			result.addLast(holder.endDirective); 
			
		}
		return result;
	}
	
	private List<NodeHolder> getDFSList() {
		LinkedList<NodeHolder> result = new LinkedList<NodeHolder>();
		for (NodeHolder holder : nodeHolders) {
			result.add(holder);
			if (holder.betweenIf != null) {
				result.addAll(holder.betweenIf.getDFSList());
			}
			if (holder.betweenElse != null) {
				result.addAll(holder.betweenElse.getDFSList());
			}
		}
		return result;
	}
	
	public boolean add (PreprocessorNode node) {
		boolean wasInserted = false;
		NodeHolder insertingRoot = null;
		
		//Wurzel entweder initialisieren oder sie holen
		if (nodeHolders.isEmpty()) {
			insertingRoot = new NodeHolder();
			nodeHolders.addLast(insertingRoot);
		} else {
			insertingRoot = nodeHolders.getLast();
		}
		
		String type = node.getType();
		if (type.startsWith("if")) {       //("if(n)?def")) {
			if (insertingRoot.endDirective != null) {
				insertingRoot = new NodeHolder();
				nodeHolders.addLast(insertingRoot);
			}
			insertIf(insertingRoot, node);
		} else if (type.matches("else")) {
			wasInserted = insertElse(insertingRoot, node);
		} else if (type.matches("elif")) {
			wasInserted = insertElse(insertingRoot, node);
			insertIf(insertingRoot, node);
		} else if (type.matches("endif")) {
			wasInserted = insertEnd(insertingRoot, node);
		}
		return wasInserted;
	}
	

	private void insertIf(NodeHolder curr, PreprocessorNode node) {
		if (curr.ifDirective == null) {
			curr.ifDirective = node;
			
		} else if (curr.elseDirective == null){
			if (curr.betweenIf == null) {
				curr.betweenIf = new PreprocessorTree();
			}
			//insertIf(curr.betweenIf, node);
			curr.betweenIf.add(node);
		} else {
			if (curr.betweenElse == null) {
				curr.betweenElse = new PreprocessorTree();
			}
			//insertIf(curr.betweenElse, node);
			curr.betweenElse.add(node);
		}
	}

	
	private boolean insertElse(NodeHolder curr, PreprocessorNode node) {
		boolean isInserted = false; 
		if (curr.elseDirective == null && curr.endDirective == null) {
			if (curr.betweenIf != null) {
				isInserted = curr.betweenIf.add(node); 
				//insertElse(curr.betweenIf, node);
			}
			if (!isInserted) {
				isInserted = true;
				curr.elseDirective = node;
			}
		} else if (curr.endDirective == null){
			isInserted = curr.betweenElse.add(node); 
			//insertElse(curr.betweenElse, node);
		}
		return isInserted;
	}

	
	private boolean insertEnd(NodeHolder curr, PreprocessorNode node) {
		boolean isInserted = false;
		if (curr.endDirective == null) {
			if (curr.betweenIf != null) {
				isInserted = curr.betweenIf.add(node); 
				//insertEnd(curr.betweenIf, node);
			}
			if (!isInserted && curr.betweenElse != null) {
				isInserted = curr.betweenElse.add(node); 
				//insertEnd(curr.betweenElse, node);
			}
			if (!isInserted) {
				curr.endDirective = node;
				if (curr.ifDirective.getType().equals("elif")) {
					curr.missingEnd = true;
				}
				isInserted = true;
			}
		}
		if (isInserted && 
				curr.elseDirective != null &&
				curr.elseDirective.getType().equals("elif") && 
				curr.betweenElse.nodeHolders.getLast().missingEnd) {
			curr.betweenElse.nodeHolders.getLast().missingEnd = false;
			insertEnd(curr, node);
		}
		return isInserted;
	}

	public List<PreprocessorNode> flattenTree() {
		LinkedList<PreprocessorNode> list = new LinkedList<PreprocessorNode>();
		for (NodeHolder holder : nodeHolders) {
			list.addAll(recurseTree(holder));
		}
		return list;
	}
	
	private Collection<PreprocessorNode> recurseTree(NodeHolder holder) {
		LinkedList<PreprocessorNode> list = new LinkedList<PreprocessorNode>();
		if (holder != null) {
			if (!holder.ifDirective.getType().equals("elif")) {
				list.addLast(holder.ifDirective);
			}
			if (holder.betweenIf != null) {
				list.addAll(holder.betweenIf.flattenTree());
				//list.addAll(recurseTree(holder.betweenIf));
			}
			
			if (holder.elseDirective != null) {
				list.addLast(holder.elseDirective);
				if (holder.betweenElse != null) {
					list.addAll(holder.betweenElse.flattenTree());
					//list.addAll(recurseTree(holder.betweenElse));
				}
			}
			
			if (!holder.ifDirective.getType().equals("elif")) {
				list.add(holder.endDirective);
			}
		}
		
		return list;
	}

	public class NodeHolder {
		
		boolean patternMark = false;
		boolean missingEnd = false;
		
		public PreprocessorNode ifDirective;
		public PreprocessorTree betweenIf;		
		public PreprocessorNode elseDirective;
		public PreprocessorTree betweenElse;
		public PreprocessorNode endDirective;
		
		//Tiefe beginnt bei 0, gibt den Verschachtelungsgrad an
		int depth = 0;
		
		public boolean compareDirectives(NodeHolder other) {
			boolean equalDirectives = false;
			
			//is this Node valid?
			if (ifDirective == null || endDirective == null) {
				return false;
			}
			//is other Node valid?
			if (other.ifDirective == null || other.endDirective == null) {
				return false;
			}
			
			
			if (ifDirective.getNode() == other.ifDirective.getNode()
					&& endDirective.getNode() == other.endDirective.getNode()) {
				equalDirectives = true;
			}
			
			if (equalDirectives) {
				if (elseDirective != null && other.elseDirective != null) {
					equalDirectives = 
						(elseDirective.getNode() == other.elseDirective.getNode());
				} else if (!(elseDirective == null && other.elseDirective == null)) {
					equalDirectives = false;
				}
			}
			return equalDirectives;
		}
	}

	public void populate(NodeList result) {
		List<PreprocessorNode> flatTree = this.flattenTree();
		
		if (flatTree.size() != result.getLength()) {
			//TODO: Exception werfen
			System.out.println("Exception!");
			System.out.println(result.item(0).getOwnerDocument().getFirstChild().getTextContent());
			System.exit(0);
		}
		
		for (int i = 0; i < result.getLength(); i++) {
			flatTree.get(i).setNode(result.item(i));
		}
	}

	public void addAgainst(PreprocessorNode node, PreprocessorTree completeTree) {
		if (node.getType().startsWith("if")) {       //.matches("if(n)?def")) {
			this.add(node);
		} else {
			NodeHolder holder = completeTree.findMatchingNodeHolder(node);
			PreprocessorNode matchAgainstIf = null;
			if (holder != null) {
				matchAgainstIf = holder.ifDirective;
				Iterator<NodeHolder> it = getDFSList().iterator();
				NodeHolder current = null;
				while (it.hasNext()) {
					current = it.next();
					if (current.ifDirective.equals(matchAgainstIf)) {
						if (node.getType().startsWith("else")) {
							current.elseDirective = node;
						} else if (node.getType().startsWith("endif")) {
							current.endDirective = node;
						}
					}
				}
			}
		}
	}

	private NodeHolder findMatchingNodeHolder(PreprocessorNode notIfNode) {
		Iterator<NodeHolder> it = nodeHolders.iterator();
		NodeHolder found = null;
		
		while (found == null && it.hasNext()) {
			NodeHolder current = it.next();
			if ((current.elseDirective != null && current.elseDirective.equals(notIfNode)) 
					|| (current.endDirective != null && current.endDirective.equals(notIfNode))) {
				found = current;
			} 
			if (current.betweenIf != null && found == null) {
				found = current.betweenIf.findMatchingNodeHolder(notIfNode);
			}
			if (current.betweenElse != null && found == null) {
				found = current.betweenElse.findMatchingNodeHolder(notIfNode);
			}
		}
		return found;
	}
	
	public void calculateDepths() {
		calculateDepths(0);
	}
	
	private void calculateDepths(int depth) {
		for(NodeHolder nh : nodeHolders) {
			nh.depth = 0;
			if (nh.betweenIf != null)
				nh.betweenIf.calculateDepths(depth+1);
			if (nh.betweenElse != null)
				nh.betweenElse.calculateDepths(depth+1);
		}
	}
}
