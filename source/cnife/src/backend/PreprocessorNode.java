package backend;

import java.io.Serializable;
import org.w3c.dom.Node;

public class PreprocessorNode implements Serializable {
	private static final long serialVersionUID = -6679500861908491082L;
	private Node node;
	private long lineNumber;
	private String type;
	private int depth;

	public int getDepth() {
		return this.depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Node getNode() {
		return this.node;
	}

	public void setNode(Node node) {
		this.node = node;
	}

	public long getLineNumber() {
		return this.lineNumber;
	}

	public void setLineNumber(long lineNumber) {
		this.lineNumber = lineNumber;
	}

	public boolean equals(PreprocessorNode other) {
		return this.node == other.getNode();
	}
}
