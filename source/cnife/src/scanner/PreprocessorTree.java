package scanner;

import backend.PreprocessorNode;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.w3c.dom.NodeList;

public class PreprocessorTree {
	LinkedList<NodeHolder> nodeHolders;

	public PreprocessorTree() {
		nodeHolders = new LinkedList<NodeHolder>();
	}

	public boolean calculateMatches(PreprocessorTree patternTree) {
		List<NodeHolder> patternNodeHolders = patternTree.getDFSList();
		List<NodeHolder> completeNodeHolders = this.getDFSList();

		for (NodeHolder pHolder : patternNodeHolders) {
			if ((pHolder.ifDirective != null) && (pHolder.endDirective != null)) {
				for (NodeHolder cHolder : completeNodeHolders) {
					if ((cHolder.patternMark)
							|| (!pHolder.compareDirectives(cHolder))) {
						continue;
					}
					cHolder.patternMark = true;
					pHolder.patternMark = true;
					pHolder.ifDirective.setLineNumber(cHolder.ifDirective
							.getLineNumber());
					pHolder.depth = cHolder.depth;
					if (pHolder.elseDirective != null) {
						pHolder.elseDirective
								.setLineNumber(cHolder.elseDirective
										.getLineNumber());
					}
					pHolder.endDirective.setLineNumber(cHolder.endDirective
							.getLineNumber());
				}
			}

		}

		patternTree.removeUnmarkedOccurrences();

		return true;
	}

	public void removeUnmarkedOccurrences() {
		PreprocessorTree newTree = new PreprocessorTree();
		buildUpCleanTree(newTree, true);
		this.nodeHolders = newTree.nodeHolders;
	}

	public void removeMarkedOccurrences() {
		PreprocessorTree newTree = new PreprocessorTree();
		buildUpCleanTree(newTree, false);
		this.nodeHolders = newTree.nodeHolders;
	}

	private void buildUpCleanTree(PreprocessorTree tree, boolean markedNodesOnly) {
		for (NodeHolder holder : this.nodeHolders)
			if (holder.patternMark == markedNodesOnly) {
				tree.add(holder.ifDirective);
				if (holder.betweenIf != null) {
					holder.betweenIf.buildUpCleanTree(tree, markedNodesOnly);
				}
				if (holder.elseDirective != null) {
					tree.add(holder.elseDirective);
					if (holder.betweenElse != null) {
						holder.betweenElse.buildUpCleanTree(tree,
								markedNodesOnly);
					}
				}
				if (holder.endDirective != null)
					tree.add(holder.endDirective);
			} else {
				if (holder.betweenIf != null) {
					holder.betweenIf.buildUpCleanTree(tree, markedNodesOnly);
				}
				if (holder.betweenElse != null)
					holder.betweenElse.buildUpCleanTree(tree, markedNodesOnly);
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
		for (NodeHolder holder : this.nodeHolders) {
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

	public boolean add(PreprocessorNode node) {
		boolean wasInserted = false;
		NodeHolder insertingRoot = null;

		if (this.nodeHolders.isEmpty()) {
			insertingRoot = new NodeHolder();
			this.nodeHolders.addLast(insertingRoot);
		} else {
			insertingRoot = (NodeHolder) this.nodeHolders.getLast();
		}

		String type = node.getType();
		if (type.startsWith("if")) {
			if (insertingRoot.endDirective != null) {
				insertingRoot = new NodeHolder();
				this.nodeHolders.addLast(insertingRoot);
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
		} else if (curr.elseDirective == null) {
			if (curr.betweenIf == null) {
				curr.betweenIf = new PreprocessorTree();
			}

			curr.betweenIf.add(node);
		} else {
			if (curr.betweenElse == null) {
				curr.betweenElse = new PreprocessorTree();
			}

			curr.betweenElse.add(node);
		}
	}

	private boolean insertElse(NodeHolder curr, PreprocessorNode node) {
		boolean isInserted = false;
		if ((curr.elseDirective == null) && (curr.endDirective == null)) {
			if (curr.betweenIf != null) {
				isInserted = curr.betweenIf.add(node);
			}

			if (!isInserted) {
				isInserted = true;
				curr.elseDirective = node;
			}
		} else if (curr.endDirective == null) {
			isInserted = curr.betweenElse.add(node);
		}

		return isInserted;
	}

	private boolean insertEnd(NodeHolder curr, PreprocessorNode node) {
		boolean isInserted = false;
		if (curr.endDirective == null) {
			if (curr.betweenIf != null) {
				isInserted = curr.betweenIf.add(node);
			}

			if ((!isInserted) && (curr.betweenElse != null)) {
				isInserted = curr.betweenElse.add(node);
			}

			if (!isInserted) {
				curr.endDirective = node;
				if (curr.ifDirective.getType().equals("elif")) {
					curr.missingEnd = true;
				}
				isInserted = true;
			}
		}
		if ((isInserted)
				&& (curr.elseDirective != null)
				&& (curr.elseDirective.getType().equals("elif"))
				&& (((NodeHolder) curr.betweenElse.nodeHolders.getLast()).missingEnd)) {
			((NodeHolder) curr.betweenElse.nodeHolders.getLast()).missingEnd = false;
			insertEnd(curr, node);
		}
		return isInserted;
	}

	public List<PreprocessorNode> flattenTree() {
		LinkedList<PreprocessorNode> list = new LinkedList<PreprocessorNode>();
		for (NodeHolder holder : this.nodeHolders) {
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
			}

			if (holder.elseDirective != null) {
				list.addLast(holder.elseDirective);
				if (holder.betweenElse != null) {
					list.addAll(holder.betweenElse.flattenTree());
				}

			}

			if (!holder.ifDirective.getType().equals("elif")) {
				list.add(holder.endDirective);
			}
		}

		return list;
	}

	public void populate(NodeList result) {
		List<PreprocessorNode> flatTree = this.flattenTree();

		if (flatTree.size() != result.getLength()) {
			System.out.println("Exception!");
			System.out.println(result.item(0).getOwnerDocument()
					.getFirstChild().getTextContent());
			System.exit(0);
		}

		for (int i = 0; i < result.getLength(); i++)
			((PreprocessorNode) flatTree.get(i)).setNode(result.item(i));
	}

	public void addAgainst(PreprocessorNode node, PreprocessorTree completeTree) {
		if (node.getType().startsWith("if")) {
			add(node);
		} else {
			NodeHolder holder = completeTree.findMatchingNodeHolder(node);
			PreprocessorNode matchAgainstIf = null;
			if (holder != null) {
				matchAgainstIf = holder.ifDirective;
				Iterator<NodeHolder> it = getDFSList().iterator();
				NodeHolder current = null;
				while (it.hasNext()) {
					current = (NodeHolder) it.next();
					if (current.ifDirective.equals(matchAgainstIf))
						if (node.getType().startsWith("else"))
							current.elseDirective = node;
						else if (node.getType().startsWith("endif"))
							current.endDirective = node;
				}
			}
		}
	}

	private NodeHolder findMatchingNodeHolder(PreprocessorNode notIfNode) {
		Iterator<NodeHolder> it = nodeHolders.iterator();
		NodeHolder found = null;

		while ((found == null) && (it.hasNext())) {
			NodeHolder current = (NodeHolder) it.next();
			if (((current.elseDirective != null) && (current.elseDirective
					.equals(notIfNode)))
					|| ((current.endDirective != null) && (current.endDirective
							.equals(notIfNode)))) {
				found = current;
			}
			if ((current.betweenIf != null) && (found == null)) {
				found = current.betweenIf.findMatchingNodeHolder(notIfNode);
			}
			if ((current.betweenElse != null) && (found == null)) {
				found = current.betweenElse.findMatchingNodeHolder(notIfNode);
			}
		}
		return found;
	}

	public void calculateDepths() {
		calculateDepths(0);
	}

	private void calculateDepths(int depth) {
		for (NodeHolder nh : this.nodeHolders) {
			nh.depth = 0;
			if (nh.betweenIf != null)
				nh.betweenIf.calculateDepths(depth + 1);
			if (nh.betweenElse != null)
				nh.betweenElse.calculateDepths(depth + 1);
		}
	}

	private class NodeHolder {
		boolean patternMark = false;
		boolean missingEnd = false;
		PreprocessorNode ifDirective;
		PreprocessorTree betweenIf;
		PreprocessorNode elseDirective;
		PreprocessorTree betweenElse;
		PreprocessorNode endDirective;
		int depth = 0;

		private NodeHolder() {
		}

		public boolean compareDirectives(NodeHolder other) {
			boolean equalDirectives = false;

			if ((this.ifDirective == null) || (this.endDirective == null)) {
				return false;
			}

			if ((other.ifDirective == null) || (other.endDirective == null)) {
				return false;
			}

			if ((this.ifDirective.getNode() == other.ifDirective.getNode())
					&& (this.endDirective.getNode() == other.endDirective
							.getNode())) {
				equalDirectives = true;
			}

			if (equalDirectives) {
				if ((this.elseDirective != null)
						&& (other.elseDirective != null))
					equalDirectives = this.elseDirective.getNode() == other.elseDirective
							.getNode();
				else if ((this.elseDirective != null)
						|| (other.elseDirective != null)) {
					equalDirectives = false;
				}
			}
			return equalDirectives;
		}
	}
}
