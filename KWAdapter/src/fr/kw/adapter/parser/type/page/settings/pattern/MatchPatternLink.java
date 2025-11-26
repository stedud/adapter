package fr.kw.adapter.parser.type.page.settings.pattern;

public class MatchPatternLink extends APatternDefinition {

	protected String name;

	public MatchPatternLink(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "MatchPatternLink [name=" + name + "]";
	}

}
