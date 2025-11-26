package fr.sap.otf.parser.object.definition;

public class ArgumentDefinition {

	private String description;
	private ArgumentType type;
	private int length = -1;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ArgumentType getType() {
		return type;
	}

	public void setType(ArgumentType type) {
		this.type = type;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

}
