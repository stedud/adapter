package fr.kw.adapter.parser.type.xml;

public class Pattern {

	protected String patternOnName = ".*";
	protected String patternOnValue = ".*";

	public Pattern(String patternOnName, String patternOnValue) {
		super();
		this.patternOnName = patternOnName;
		this.patternOnValue = patternOnValue;
	}

	public Pattern() {
		super();
	}

	public Pattern(String patternOnName) {
		super();
		this.patternOnName = patternOnName;
	}

	public String getPatternOnName() {
		return patternOnName;
	}

	public String getPatternOnValue() {
		return patternOnValue;
	}

}
