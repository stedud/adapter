package fr.kw.adapter.parser.event;

import org.apache.commons.text.StringEscapeUtils;

import fr.utils.Utils;
import fr.utils.Values;

public class Field {

	private String name = "";
	protected String value = "";
	protected boolean append = false;
	protected boolean include = false;

	public Field() {
		// TODO Auto-generated constructor stub
	}

	public Field(String name) {
		super();
		this.name = Values.getValue(name);
	}

	public Field(String name, String value) {
		super();
		this.name = Values.getValue(name);
		this.value = Values.getValue(value);
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = Values.getValue(value);
	}

	public String toXML() {
		StringBuffer sb = new StringBuffer("<");
		String fieldName = Values.getValue(Utils.normalizeXmlName(getName()));
		sb.append(fieldName);
		sb.append(">");
		sb.append(Values.getValue(StringEscapeUtils.escapeXml11(getValue())));
		sb.append("</");
		sb.append(fieldName);
		sb.append(">");
		return sb.toString();
	}

	public boolean isAppend() {
		return append;
	}

	public void setAppend(boolean append) {
		this.append = append;
	}

	@Override
	public String toString() {
		return "Field [name=" + name + ", value=" + value + ", append=" + append + "]";
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = Values.getValue(name);
	}

	public boolean isInclude() {
		return include;
	}

	public void setInclude(boolean include) {
		this.include = include;
	}

	@Override
	public int hashCode() {
		
		return this.toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		
		return obj != null && obj instanceof Field && this.toString().compareTo(obj.toString()) == 0;
	}

}
