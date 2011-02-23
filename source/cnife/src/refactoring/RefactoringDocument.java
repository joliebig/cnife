package refactoring;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

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
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public RefactoringDocument(Document xmlDoc) {
		super();
//		Document  doc = null;
//TODO Dokumentenkopien
		this.xmlDoc = xmlDoc;
//		if (xmlDoc != null) {
//			doc = xmlDoc.getImplementation().createDocument(
//					xmlDoc.getNamespaceURI(), 
//					null, 
//					xmlDoc.getDoctype());
//			Node imported = doc.importNode(xmlDoc.getFirstChild(), true);
//			doc.appendChild(imported);
////TODO DEBUG			
////			System.out.println(doc.getFirstChild().getTextContent());
//		}
//		this.xmlDoc = doc;
	}

	public void saveDocument(String pathName)
			throws TransformerFactoryConfigurationError, FileNotFoundException,
			TransformerException {
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				DOMSource src = new DOMSource(xmlDoc);
				FileOutputStream out = new FileOutputStream(pathName);
				StreamResult res = new StreamResult(out);
				transformer.transform(src, res);
			}

	public void setFeatureName(String featureName) {
		this.featureName = featureName;
	}
	
	public String getFeatureName() {
		return featureName;
	}

	public void setModified(boolean modified) {
		this.modified = modified;
		
	}

	public boolean getModified() {
		return modified;
	}
	//Delegators
	
	public Node adoptNode(Node source) throws DOMException {
		return xmlDoc.adoptNode(source);
	}

	public Node appendChild(Node newChild) throws DOMException {
		return xmlDoc.appendChild(newChild);
	}

	public Node cloneNode(boolean deep) {
		return xmlDoc.cloneNode(deep);
	}

	public short compareDocumentPosition(Node other) throws DOMException {
		return xmlDoc.compareDocumentPosition(other);
	}

	public Attr createAttribute(String name) throws DOMException {
		return xmlDoc.createAttribute(name);
	}

	public Attr createAttributeNS(String namespaceURI, String qualifiedName)
			throws DOMException {
				return xmlDoc.createAttributeNS(namespaceURI, qualifiedName);
			}

	public CDATASection createCDATASection(String data) throws DOMException {
		return xmlDoc.createCDATASection(data);
	}

	public Comment createComment(String data) {
		return xmlDoc.createComment(data);
	}

	public DocumentFragment createDocumentFragment() {
		return xmlDoc.createDocumentFragment();
	}

	public Element createElement(String tagName) throws DOMException {
		return xmlDoc.createElement(tagName);
	}

	public Element createElementNS(String namespaceURI, String qualifiedName)
			throws DOMException {
				return xmlDoc.createElementNS(namespaceURI, qualifiedName);
			}

	public EntityReference createEntityReference(String name) throws DOMException {
		return xmlDoc.createEntityReference(name);
	}

	public ProcessingInstruction createProcessingInstruction(String target, String data)
			throws DOMException {
				return xmlDoc.createProcessingInstruction(target, data);
			}

	public Text createTextNode(String data) {
		return xmlDoc.createTextNode(data);
	}

	public NamedNodeMap getAttributes() {
		return xmlDoc.getAttributes();
	}

	public String getBaseURI() {
		return xmlDoc.getBaseURI();
	}

	public NodeList getChildNodes() {
		return xmlDoc.getChildNodes();
	}

	public DocumentType getDoctype() {
		return xmlDoc.getDoctype();
	}

	public Element getDocumentElement() {
		return xmlDoc.getDocumentElement();
	}

	public String getDocumentURI() {
		return xmlDoc.getDocumentURI();
	}

	public DOMConfiguration getDomConfig() {
		return xmlDoc.getDomConfig();
	}

	public Element getElementById(String elementId) {
		return xmlDoc.getElementById(elementId);
	}

	public NodeList getElementsByTagName(String tagname) {
		return xmlDoc.getElementsByTagName(tagname);
	}

	public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
		return xmlDoc.getElementsByTagNameNS(namespaceURI, localName);
	}

	public Object getFeature(String feature, String version) {
		return xmlDoc.getFeature(feature, version);
	}

	public Node getFirstChild() {
		return xmlDoc.getFirstChild();
	}

	public DOMImplementation getImplementation() {
		return xmlDoc.getImplementation();
	}

	public String getInputEncoding() {
		return xmlDoc.getInputEncoding();
	}

	public Node getLastChild() {
		return xmlDoc.getLastChild();
	}

	public String getLocalName() {
		return xmlDoc.getLocalName();
	}

	public String getNamespaceURI() {
		return xmlDoc.getNamespaceURI();
	}

	public Node getNextSibling() {
		return xmlDoc.getNextSibling();
	}

	public String getNodeName() {
		return xmlDoc.getNodeName();
	}

	public short getNodeType() {
		return xmlDoc.getNodeType();
	}

	public String getNodeValue() throws DOMException {
		return xmlDoc.getNodeValue();
	}

	public Document getOwnerDocument() {
		return xmlDoc.getOwnerDocument();
	}

	public Node getParentNode() {
		return xmlDoc.getParentNode();
	}

	public String getPrefix() {
		return xmlDoc.getPrefix();
	}

	public Node getPreviousSibling() {
		return xmlDoc.getPreviousSibling();
	}

	public boolean getStrictErrorChecking() {
		return xmlDoc.getStrictErrorChecking();
	}

	public String getTextContent() throws DOMException {
		return xmlDoc.getTextContent();
	}

	public Object getUserData(String key) {
		return xmlDoc.getUserData(key);
	}

	public String getXmlEncoding() {
		return xmlDoc.getXmlEncoding();
	}

	public boolean getXmlStandalone() {
		return xmlDoc.getXmlStandalone();
	}

	public String getXmlVersion() {
		return xmlDoc.getXmlVersion();
	}

	public boolean hasAttributes() {
		return xmlDoc.hasAttributes();
	}

	public boolean hasChildNodes() {
		return xmlDoc.hasChildNodes();
	}

	public Node importNode(Node importedNode, boolean deep) throws DOMException {
		return xmlDoc.importNode(importedNode, deep);
	}

	public Node insertBefore(Node newChild, Node refChild) throws DOMException {
		return xmlDoc.insertBefore(newChild, refChild);
	}

	public boolean isDefaultNamespace(String namespaceURI) {
		return xmlDoc.isDefaultNamespace(namespaceURI);
	}

	public boolean isEqualNode(Node arg) {
		return xmlDoc.isEqualNode(arg);
	}

	public boolean isSameNode(Node other) {
		return xmlDoc.isSameNode(other);
	}

	public boolean isSupported(String feature, String version) {
		return xmlDoc.isSupported(feature, version);
	}

	public String lookupNamespaceURI(String prefix) {
		return xmlDoc.lookupNamespaceURI(prefix);
	}

	public String lookupPrefix(String namespaceURI) {
		return xmlDoc.lookupPrefix(namespaceURI);
	}

	public void normalize() {
		xmlDoc.normalize();
	}

	public void normalizeDocument() {
		xmlDoc.normalizeDocument();
	}

	public Node removeChild(Node oldChild) throws DOMException {
		return xmlDoc.removeChild(oldChild);
	}

	public Node renameNode(Node n, String namespaceURI, String qualifiedName)
			throws DOMException {
				return xmlDoc.renameNode(n, namespaceURI, qualifiedName);
			}

	public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
		return xmlDoc.replaceChild(newChild, oldChild);
	}

	public void setDocumentURI(String documentURI) {
		xmlDoc.setDocumentURI(documentURI);
	}

	public void setNodeValue(String nodeValue) throws DOMException {
		xmlDoc.setNodeValue(nodeValue);
	}

	public void setPrefix(String prefix) throws DOMException {
		xmlDoc.setPrefix(prefix);
	}

	public void setStrictErrorChecking(boolean strictErrorChecking) {
		xmlDoc.setStrictErrorChecking(strictErrorChecking);
	}

	public void setTextContent(String textContent) throws DOMException {
		xmlDoc.setTextContent(textContent);
	}

	public Object setUserData(String key, Object data, UserDataHandler handler) {
		return xmlDoc.setUserData(key, data, handler);
	}

	public void setXmlStandalone(boolean xmlStandalone) throws DOMException {
		xmlDoc.setXmlStandalone(xmlStandalone);
	}

	public void setXmlVersion(String xmlVersion) throws DOMException {
		xmlDoc.setXmlVersion(xmlVersion);
	}


}