package fr.kw.adapter.parser.type.rdi;

public enum RdiLineType {
	HEADER("H"), SORT("S"), CONTROL("C"), DATA("D"), ARCHIVE("I"), PARAMETER("P"), NONE(""),
	INCLUDE_BEGIN("CINC-BEGIN"), INCLUDE_END("CINC-END");

	private String name;

	RdiLineType(String name) {
		this.name = name;
	}

	public String toString() {
		return name;
	}
}
