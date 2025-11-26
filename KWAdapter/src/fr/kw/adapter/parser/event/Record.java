package fr.kw.adapter.parser.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.kw.adapter.parser.event.structure.RecordStructure;
import fr.utils.Utils;
import fr.utils.Values;

public class Record {

	protected List<Field> fields = new ArrayList<Field>();
	protected String name = "";
	protected Record parent;
	protected List<Record> records = new ArrayList<Record>();
	protected RecordStructure structure;// facultatif

	public Record(String name) {
		super();
		this.name = Values.getValue(name);
		this.structure = null;
	}

	public String getValue(String fieldName) {

		for (Field f : fields) {
			if (f.getName().compareTo(fieldName) == 0)
				return f.getValue();
		}
		return "";
	}

	public List<Record> getRecords(String recordName) {
		List<Record> recordsMAtched = new ArrayList<Record>();
		for (Record r : records) {
			if (r.getName().compareTo(recordName) == 0)
				recordsMAtched.add(r);
		}
		return recordsMAtched;
	}

	public Record(RecordStructure structure) {
		super();
		if (structure != null)
			this.name = Values.getValue(structure.getName());
		this.structure = structure;
	}

	public boolean containsField(String fieldName) {
		for (Field f : this.fields) {
			if (f.getName().compareTo(fieldName) == 0)
				return true;
		}
		return false;
	}

	public RecordStructure getDefinition() {
		return structure;
	}

	public List<Field> getFields() {
		return fields;
	}

	public Record getRootParent() {
		if (parent == null)
			return this;
		else
			return parent.getRootParent();
	}

	public Record getParent() {
		return parent;
	}

	public List<Record> getRecords() {
		return records;
	}

	public void setParent(Record parent) {
		this.parent = parent;
	}

	public void setStructure(RecordStructure definition) {
		this.structure = definition;
	}

	public String toXML() {
		StringBuffer sb = new StringBuffer("<");
		sb.append(Utils.normalizeXmlName(this.name));
		sb.append(">\n");
		for (Field f : fields) {
			sb.append(f.toXML());
			sb.append("\n");
		}

		for (Record r : records) {
			sb.append(r.toXML());
			sb.append("\n");
		}

		sb.append("</");
		sb.append(Utils.normalizeXmlName(name));
		sb.append(">");
		return sb.toString();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public RecordStructure getStructure() {
		return structure;
	}

	protected boolean fieldPresent(Field field, boolean recursive) {
		if (this.fields.contains(field))
			return true;
		if (recursive) for (Record child : records) {
			if (child.fieldPresent(field, recursive))
				return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "Record [name=" + name + ", parent=" + (parent != null ? parent.getName() : null) + ", fields=" + fields
				+ ", records=" + records + ", structure=" + structure + "]";
	}

	
	public Map<String, Object> getContext()
	{
		Map<String, Object> context = new HashMap<String, Object>();
		for (Field f : this.fields)
		{
			context.put(f.getName(), f.getValue());
		}
		
		
		for (Record r : this.records)
		{
			Map<String, Object> subContext = r.getContext();
			List<Map<String,Object>> records = (List<Map<String, Object>>) context.get(r.getName());
			if ( records == null)
			{
				
				 records = new ArrayList<Map<String,Object>>();
				 context.put(r.getName(), records);
			}
			records.add(subContext);

			
		}
		
		return context;
	}


}
