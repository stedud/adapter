package fr.sap.otf.parser.object;

import fr.sap.otf.parser.object.definition.ArgumentDefinition;

public class Argument {

	private ArgumentDefinition definition;
	private String value;

	public ArgumentDefinition getDefinition() {
		return definition;
	}

	public void setDefinition(ArgumentDefinition definition) {
		this.definition = definition;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
