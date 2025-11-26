package fr.kw.adapter.parser.type.recordin;

import fr.kw.adapter.parser.event.structure.FieldDefinition;

public class FieldRecordDefinition extends FieldDefinition {

	protected int posField = 0;
	protected int fixLenField = Integer.MAX_VALUE;
	protected String labelPrefix = "";
	protected int posLabelPrefix = 0;
	protected int posPrefix = 0;
	protected String chrSepPrefix = "";
	protected String labelFieldCont = "";
	protected int posLabelFieldCont = 0;
	protected String fieldContString = " ";
	protected int posStartValue = 0;
	protected int posEndValue = 0;
	protected boolean keepSpace = false;
	protected boolean keepTrailingSpace = false;
	protected boolean keepLeadingSpace = false;
	protected String chrSepField = ";";

	public FieldRecordDefinition() {
		super();
	}

	public FieldRecordDefinition(String name) {
		super(name);
	}

}
