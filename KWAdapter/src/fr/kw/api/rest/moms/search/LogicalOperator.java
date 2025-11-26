package fr.kw.api.rest.moms.search;

public enum LogicalOperator {
	AND("AND"), OR("OR"), AND_NOT("AND NOT"), OR_NOT("OR NOT");

	private final String value;

	private LogicalOperator(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
