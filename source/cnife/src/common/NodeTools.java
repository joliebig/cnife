package common;

import java.util.ArrayList;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NodeTools {
	public static boolean haveSameParents(Node node1, Node node2) {
		return node1.getParentNode() == node2.getParentNode();
	}

	public static Node getCommonAncestors(Node node1, Node node2) {
		int depthNode1 = getDepth(node1);
		int depthNode2 = getDepth(node2);
		Node ancestor1 = node1;
		Node ancestor2 = node2;
		if (depthNode1 > depthNode2) {
			for (int i = 0; i < depthNode1 - depthNode2; i++)
				ancestor1 = ancestor1.getParentNode();
		} else if (depthNode1 < depthNode2) {
			for (int i = 0; i < depthNode2 - depthNode1; i++) {
				ancestor2 = ancestor2.getParentNode();
			}
		}

		while ((ancestor1 != ancestor2) && (ancestor1 != null)
				&& (ancestor2 != null)) {
			ancestor1 = ancestor1.getParentNode();
			ancestor2 = ancestor2.getParentNode();
		}
		if ((ancestor1 == null) || (ancestor2 == null)) {
			return null;
		}
		return ancestor1;
	}

	public static int getDepth(Node node) {
		int depth = 0;
		Node ancestor = node.getParentNode();
		while (ancestor != null) {
			depth++;
			ancestor = ancestor.getParentNode();
		}
		return depth;
	}

	public static boolean containsNode(Node container, Node child) {
		return getCommonAncestors(container, child) == container;
	}

	public static NodeList intersectUpperLower(NodeList lowerList,
			NodeList upperList) {
		final ArrayList<Node> content = new ArrayList<Node>();
		NodeList result = new NodeList() {
			
			private ArrayList<Node> list = content;
			
			@Override
			public Node item(int index) {
				return list.get(index);
			}
			
			@Override
			public int getLength() {
				return list.size();
			}
		};
		int upperIndex = upperList.getLength() - 1;
		int lowerIndex = 0;
		boolean done = false;
		while (!done) {
			done = true;
			for (int i = upperIndex; (i >= 0) && (done); i--) {
				if (lowerList.item(lowerIndex) == upperList.item(i)) {
					done = false;
					content.add(lowerList.item(lowerIndex));
				}
			}
			lowerIndex++;
		}

		return result;
	}
}
