package fr.kw.api.rest.moms.search;

import java.util.ArrayList;
import java.util.List;

public class SearchParameter {

	protected String name = "KW_MEDIUM";
	protected List<String> values = new ArrayList<String>(1);

	protected LogicalOperator logicalOperator = LogicalOperator.AND;
	protected Operator operator = Operator.EQ;
	protected List<SearchParameter> children = new ArrayList<SearchParameter>(1);

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue(int i) {
		return values.get(i);
	}

	public void addValue(String value) {
		this.values.add(value);
	}

	public LogicalOperator getLogicalOperator() {
		return logicalOperator;
	}

	public void setLogicalOperator(LogicalOperator logicalOperator) {
		this.logicalOperator = logicalOperator;
	}

	public Operator getOperator() {
		return operator;
	}

	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	public List<SearchParameter> getChildren() {
		return children;
	}

	@Override
	public String toString() {
		return toJSonString();
	}

	public String toJSonString() {
		StringBuffer sb = new StringBuffer("{\"");
		if (this.operator == Operator.BETWEEN) {
			sb.append(this.operator.getValue());
			sb.append("\":{");
			sb.append("\"logicalOperator\":\"");
			sb.append(this.logicalOperator.getValue());
			sb.append("\", \"parameterName\":\"");
			sb.append(this.getName());
			sb.append("\", \"lowValue\":\"");
			sb.append(this.getValue(0));
			sb.append("\", \"highValue\":\"");
			sb.append(this.getValue(1));
		} else {
			sb.append(this.operator.getValue());
			sb.append("\":{");
			sb.append("\"logicalOperator\":\"");
			sb.append(this.logicalOperator.getValue());
			sb.append("\", \"parameterName\":\"");
			sb.append(this.getName());
			sb.append("\", \"value\":\"");
			sb.append(this.getValue(0));
		}

		sb.append("\"}}");

		// TODO Auto-generated method stub
		return sb.toString();
	}

}
