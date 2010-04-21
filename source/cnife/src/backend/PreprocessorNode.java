package backend;
import java.io.Serializable;

import org.w3c.dom.Node;


public class PreprocessorNode implements Serializable {
	private static final long serialVersionUID = -6679500861908491082L;
	
	private Node node;
	private long lineNumber;
	private String type;
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Node getNode() {
		return node;
	}
	public void setNode(Node node) {
		this.node = node;
	}
	public long getLineNumber() {
		return lineNumber;
	}
	public void setLineNumber(long lineNumber) {
		this.lineNumber = lineNumber;
	}
	
	public boolean equals(PreprocessorNode other)  {
		return this.node == other.getNode();
	}
}
