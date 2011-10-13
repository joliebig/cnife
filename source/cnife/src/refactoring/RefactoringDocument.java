package refactoring;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;

public class RefactoringDocument implements Document {
	protected static String CLASS_NODE_QUERY = "/src:unit/src:class/src:name";
	protected Document xmlDoc;
	private String featureName;
	private boolean modified;
	private String fileName;

	public String getFileName() {
		return this.fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public RefactoringDocument(Document xmlDoc) {
		this.xmlDoc = xmlDoc;
	}

	public void saveDocument(String pathName)
			throws TransformerFactoryConfigurationError, FileNotFoundException,
			TransformerException {
		Transformer transformer = TransformerFactory.newInstance()
				.newTransformer();
		DOMSource src = new DOMSource(this.xmlDoc);
		FileOutputStream out = new FileOutputStream(pathName);
		StreamResult res = new StreamResult(out);
		transformer.transform(src, res);
		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setFeatureName(String featureName) {
		this.featureName = featureName;
	}

	public String getFeatureName() {
		return this.featureName;
	}

	public void setModified(boolean modified) {
		this.modified = modified;
	}

	public boolean getModified() {
		return this.modified;
	}

	public Node adoptNode(Node source) throws DOMException {
		return this.xmlDoc.adoptNode(source);
	}

	public Node appendChild(Node newChild) throws DOMException {
		return this.xmlDoc.appendChild(newChild);
	}

	public Node cloneNode(boolean deep) {
		return this.xmlDoc.cloneNode(deep);
	}

	public short compareDocumentPosition(Node other) throws DOMException {
		return this.xmlDoc.compareDocumentPosition(other);
	}

	public Attr createAttribute(String name) throws DOMException {
		return this.xmlDoc.createAttribute(name);
	}

	public Attr createAttributeNS(String namespaceURI, String qualifiedName)
			throws DOMException {
		return this.xmlDoc.createAttributeNS(namespaceURI, qualifiedName);
	}

	public CDATASection createCDATASection(String data) throws DOMException {
		return this.xmlDoc.createCDATASection(data);
	}

	public Comment createComment(String data) {
		return this.xmlDoc.createComment(data);
	}

	public DocumentFragment createDocumentFragment() {
		return this.xmlDoc.createDocumentFragment();
	}

	public Element createElement(String tagName) throws DOMException {
		return this.xmlDoc.createElement(tagName);
	}

	public Element createElementNS(String namespaceURI, String qualifiedName)
			throws DOMException {
		return this.xmlDoc.createElementNS(namespaceURI, qualifiedName);
	}

	public EntityReference createEntityReference(String name)
			throws DOMException {
		return this.xmlDoc.createEntityReference(name);
	}

	public ProcessingInstruction createProcessingInstruction(String target,
			String data) throws DOMException {
		return this.xmlDoc.createProcessingInstruction(target, data);
	}

	public Text createTextNode(String data) {
		return this.xmlDoc.createTextNode(data);
	}

	public NamedNodeMap getAttributes() {
		return this.xmlDoc.getAttributes();
	}

	public String getBaseURI() {
		return this.xmlDoc.getBaseURI();
	}

	public NodeList getChildNodes() {
		return this.xmlDoc.getChildNodes();
	}

	public DocumentType getDoctype() {
		return this.xmlDoc.getDoctype();
	}

	public Element getDocumentElement() {
		return this.xmlDoc.getDocumentElement();
	}

	public String getDocumentURI() {
		return this.xmlDoc.getDocumentURI();
	}

	public DOMConfiguration getDomConfig() {
		return this.xmlDoc.getDomConfig();
	}

	public Element getElementById(String elementId) {
		return this.xmlDoc.getElementById(elementId);
	}

	public NodeList getElementsByTagName(String tagname) {
		return this.xmlDoc.getElementsByTagName(tagname);
	}

	public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
		return this.xmlDoc.getElementsByTagNameNS(namespaceURI, localName);
	}

	public Object getFeature(String feature, String version) {
		return this.xmlDoc.getFeature(feature, version);
	}

	public Node getFirstChild() {
		return this.xmlDoc.getFirstChild();
	}

	public DOMImplementation getImplementation() {
		return this.xmlDoc.getImplementation();
	}

	public String getInputEncoding() {
		return this.xmlDoc.getInputEncoding();
	}

	public Node getLastChild() {
		return this.xmlDoc.getLastChild();
	}

	public String getLocalName() {
		return this.xmlDoc.getLocalName();
	}

	public String getNamespaceURI() {
		return this.xmlDoc.getNamespaceURI();
	}

	public Node getNextSibling() {
		return this.xmlDoc.getNextSibling();
	}

	public String getNodeName() {
		return this.xmlDoc.getNodeName();
	}

	public short getNodeType() {
		return this.xmlDoc.getNodeType();
	}

	public String getNodeValue() throws DOMException {
		return this.xmlDoc.getNodeValue();
	}

	public Document getOwnerDocument() {
		return this.xmlDoc.getOwnerDocument();
	}

	public Node getParentNode() {
		return this.xmlDoc.getParentNode();
	}

	public String getPrefix() {
		return this.xmlDoc.getPrefix();
	}

	public Node getPreviousSibling() {
		return this.xmlDoc.getPreviousSibling();
	}

	public boolean getStrictErrorChecking() {
		return this.xmlDoc.getStrictErrorChecking();
	}

	public String getTextContent() throws DOMException {
		return this.xmlDoc.getTextContent();
	}

	public Object getUserData(String key) {
		return this.xmlDoc.getUserData(key);
	}

	public String getXmlEncoding() {
		return this.xmlDoc.getXmlEncoding();
	}

	public boolean getXmlStandalone() {
		return this.xmlDoc.getXmlStandalone();
	}

	public String getXmlVersion() {
		return this.xmlDoc.getXmlVersion();
	}

	public boolean hasAttributes() {
		return this.xmlDoc.hasAttributes();
	}

	public boolean hasChildNodes() {
		return this.xmlDoc.hasChildNodes();
	}

	public Node importNode(Node importedNode, boolean deep) throws DOMException {
		return this.xmlDoc.importNode(importedNode, deep);
	}

	public Node insertBefore(Node newChild, Node refChild) throws DOMException {
		return this.xmlDoc.insertBefore(newChild, refChild);
	}

	public boolean isDefaultNamespace(String namespaceURI) {
		return this.xmlDoc.isDefaultNamespace(namespaceURI);
	}

	public boolean isEqualNode(Node arg) {
		return this.xmlDoc.isEqualNode(arg);
	}

	public boolean isSameNode(Node other) {
		return this.xmlDoc.isSameNode(other);
	}

	public boolean isSupported(String feature, String version) {
		return this.xmlDoc.isSupported(feature, version);
	}

	public String lookupNamespaceURI(String prefix) {
		return this.xmlDoc.lookupNamespaceURI(prefix);
	}

	public String lookupPrefix(String namespaceURI) {
		return this.xmlDoc.lookupPrefix(namespaceURI);
	}

	public void normalize() {
		this.xmlDoc.normalize();
	}

	public void normalizeDocument() {
		this.xmlDoc.normalizeDocument();
	}

	public Node removeChild(Node oldChild) throws DOMException {
		return this.xmlDoc.removeChild(oldChild);
	}

	public Node renameNode(Node n, String namespaceURI, String qualifiedName)
			throws DOMException {
		return this.xmlDoc.renameNode(n, namespaceURI, qualifiedName);
	}

	public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
		return this.xmlDoc.replaceChild(newChild, oldChild);
	}

	public void setDocumentURI(String documentURI) {
		this.xmlDoc.setDocumentURI(documentURI);
	}

	public void setNodeValue(String nodeValue) throws DOMException {
		this.xmlDoc.setNodeValue(nodeValue);
	}

	public void setPrefix(String prefix) throws DOMException {
		this.xmlDoc.setPrefix(prefix);
	}

	public void setStrictErrorChecking(boolean strictErrorChecking) {
		this.xmlDoc.setStrictErrorChecking(strictErrorChecking);
	}

	public void setTextContent(String textContent) throws DOMException {
		this.xmlDoc.setTextContent(textContent);
	}

	public Object setUserData(String key, Object data, UserDataHandler handler) {
		return this.xmlDoc.setUserData(key, data, handler);
	}

	public void setXmlStandalone(boolean xmlStandalone) throws DOMException {
		this.xmlDoc.setXmlStandalone(xmlStandalone);
	}

	public void setXmlVersion(String xmlVersion) throws DOMException {
		this.xmlDoc.setXmlVersion(xmlVersion);
	}
}
