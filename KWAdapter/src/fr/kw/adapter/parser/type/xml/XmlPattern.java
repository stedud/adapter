package fr.kw.adapter.parser.type.xml;

public class XmlPattern {

	protected Pattern patternOnNode = new Pattern();
	protected Pattern patternOnAttribute = null;

	public XmlPattern() {
		// TODO Auto-generated constructor stub
	}

	public Pattern getPatternOnNode() {
		return patternOnNode;
	}

	public void setPatternOnNode(Pattern patternOnNode) {
		this.patternOnNode = patternOnNode;
	}

	public Pattern getPatternOnAttribute() {
		return patternOnAttribute;
	}

	public void setPatternOnAttribute(Pattern patternOnAttribute) {
		this.patternOnAttribute = patternOnAttribute;
	}

}
