package fr.kw.adapter.parser.event.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.kw.adapter.parser.event.Record;
import fr.utils.Utils;

public class RecordStructure {

	protected String name = "";
	protected RecordStructure parent = null;
	protected Map<String, FieldDefinition> fields = new HashMap<String, FieldDefinition>(5);
	protected Map<String, RecordStructure> children = new HashMap<String, RecordStructure>(5);

	/**
	 * Will contain definitions for JDBC or Script.
	 */
	protected List<String> externalDataDefinitions;

	public RecordStructure(String name) {
		super();
		this.parent = null;
		this.name = name;
	}

	public RecordStructure(String name, RecordStructure parent) {
		super();
		this.name = name;
		this.parent = parent;
	}

	public String getName() {
		return name;
	}

	public RecordStructure getParent() {
		return parent;
	}

	public void addExternalDataDefinition(String jdbcDefinition) {
		if (externalDataDefinitions == null)
			externalDataDefinitions = new ArrayList<String>(1);

		externalDataDefinitions.add(jdbcDefinition);
	}

	public Map<String, FieldDefinition> getFields() {
		return fields;
	}

	public Map<String, RecordStructure> getChildren() {
		return children;
	}

	public RecordStructure findRecord(String fieldName) {

		RecordStructure result = findRecord(fieldName, this);

		return result;
	}

	private RecordStructure findRecord(String fieldName, RecordStructure record) {
		RecordStructure result = record.getFields().containsKey(fieldName) ? record : null;

		if (result == null) {
			for (RecordStructure r : record.getChildren().values()) {
				result = findRecord(fieldName, r);
				if (result != null)
					break;
			}
		}

		return result;
	}

	public String toXML() {
		StringBuffer sb = new StringBuffer("<");
		String recordName = Utils.normalizeXmlName(getName());
		sb.append(recordName);
		sb.append(">\n");
		for (FieldDefinition f : fields.values()) {
			sb.append(f.toXML());
			sb.append("\n");
		}

		for (RecordStructure r : children.values()) {
			sb.append(r.toXML());
			sb.append("\n");
		}

		sb.append("</");
		sb.append(recordName);
		sb.append(">");
		return sb.toString();
	}

	@Override
	public String toString() {
		return "RecordStructure [name=" + name + ", parent=" + parent + ", fields=" + fields + ", children=" + children
				+ "]";
	}

	public List<String> getExternalDataDefinitions() {
		if (externalDataDefinitions == null)
			return Collections.EMPTY_LIST;
		else
			return externalDataDefinitions;
	}

	public boolean recordExistsInStructure(String recordName)
	{
		for (RecordStructure rec : this.children.values())
		{
			if (rec.getName().compareTo(recordName) == 0) return true;
			else
			{
				boolean existsInChildren = rec.recordExistsInStructure(recordName);
				if (existsInChildren) return true;
			}
		}
		return false;
	}
}
