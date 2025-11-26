package fr.kw.adapter.parser.event.structure;

import fr.utils.Utils;

public class FieldDefinition {

	protected String name = "";

	public FieldDefinition() {
	}

	public FieldDefinition(String name) {
		super();
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String toXML() {
		return "<" + Utils.normalizeXmlName(getName()) + "/>";

	}

	@Override
	public String toString() {
		return "FieldDefinition [name=" + name + "]";
	}

}
