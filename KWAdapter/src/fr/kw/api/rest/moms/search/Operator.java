package fr.kw.api.rest.moms.search;

public enum Operator {
	LIKE("like"), GT("greater"), LW("less"), EQ("equal"), NOT_EQ("notEqual"), BETWEEN("between");

	private String value;

	private Operator(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
