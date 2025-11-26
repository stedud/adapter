package fr.kw.adapter.parser.type.page.document;

public class Field {

	protected String name;
	protected String value;

	public Field() {
		this(null, null);
	}

	public Field(String name) {
		this(name, null);
	}

	public Field(String name, String value) {
		super();
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "Field [name=" + name + ", value=" + value + "]";
	}

}
