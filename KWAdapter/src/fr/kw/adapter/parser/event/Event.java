package fr.kw.adapter.parser.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.kw.adapter.parser.event.structure.EventStructure;
import fr.kw.adapter.parser.event.structure.RecordStructure;


public class Event extends Record {

	public Event(RecordStructure structure) {
		super(structure);
		// TODO Auto-generated constructor stub
	}

	public Event(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	public Event(EventStructure structure) {
		super(structure);
		// TODO Auto-generated constructor stub
	}

	private void insertFiedInDocRecord(Field field, RecordStructure block) {
		// TODO : chercher le record pouvant contenir le champ, le créer s'il n'existe
		// pas
		if (block == null || block.getParent() == null || block.getName().compareTo("root") == 0) {
			this.fields.add(field);
			return;
		}

		// find root parent block
		List<RecordStructure> blockPath = new ArrayList<RecordStructure>();

		blockPath.add(block);

		while (blockPath.get(blockPath.size() - 1).getParent() != null) {
			blockPath.add(blockPath.get(blockPath.size() - 1).getParent());
		}
		Collections.reverse(blockPath);

		int level = 0; // level 0 = root = doc level
		Record currRecord = this;

		for (final RecordStructure recStruct : blockPath) {
			boolean lastLevel = recStruct.getName().compareTo(block.getName()) == 0;

			ArrayList<Record> children = new ArrayList<Record>();
			children.add(currRecord);
			children.addAll(currRecord.getRecords());
			Collections.reverse(children);// On part du dernier inséré (le plus récent)
			boolean recordFound = false;
			for (Record r : children) {
				if (r.getDefinition() != null) {
					if (r.getDefinition().getName().compareTo(recStruct.getName()) == 0) {
						recordFound = true;
						currRecord = r;
						break;
					}
				}
			}

			if (!recordFound) {
				Record newRecord = new Record(recStruct);
				newRecord.setName(recStruct.getName());
				currRecord.getRecords().add(newRecord);
				newRecord.setParent(currRecord);

				currRecord = newRecord;
			}

			if (lastLevel) {
				if (currRecord.containsField(field.getName())) {
					Record newRecord = new Record(block);
					newRecord.setName(block.getName());
					newRecord.setParent(currRecord.getParent());
					newRecord.getFields().add(field);
					currRecord.getParent().getRecords().add(newRecord);

				} else {
					currRecord.getFields().add(field);
				}
			}

		}

		/**
		 * { RecordStructure recStruct = null; if (recStruct.getParent() != null &&
		 * recStruct.getName().compareTo("root") != 0) { boolean finalLevel =
		 * recStruct.getName().compareTo(block.getName()) == 0; level++; boolean found =
		 * false;
		 * 
		 * List<Record> children = new ArrayList(currRecord.getRecords());
		 * Collections.reverse(children); for (Record r : children) { if
		 * (r.getDefinition() == null) { System.out.println("Record has no definition ("
		 * + r.toString() + ")"); } if (r.getName().compareTo(recStruct.getName()) == 0
		 * && !found) { found = true; parent = currRecord; currRecord = r; break; } }
		 * 
		 * if (!found) { Record newRecord = new Record(block);
		 * currRecord.getRecords().add(newRecord); parent = currRecord; currRecord =
		 * newRecord; } if (finalLevel) { if
		 * (currRecord.containsField(field.getDefinition().getName())) { Record
		 * newRecord = new Record(block); newRecord.getFields().add(field);
		 * parent.getRecords().add(newRecord); } else { currRecord.fields.add(field); }
		 * } } }
		 **/

	}

	public void insertRecord(Record record) {

		if (structure == null) {
			this.records.add(record);
			record.setParent(this);
			return;
		}
		boolean recordExistsInStructure = structure.recordExistsInStructure(record.getName());
		boolean originalRecordCreated = false;
		Record recordCopy = null;
		for (Field field : record.fields) {
			if (this.fieldPresent(field, false))
				continue;

			// else
			
			if (recordExistsInStructure)
			{
				this.insertField(field);
			}
			else
			{
				RecordStructure block = structure.findRecord(field.getName());
				if (block == null) {
					if (!originalRecordCreated) {
						recordCopy = new Record(record.getName());
						recordCopy.setParent(this);
						recordCopy.setStructure(record.getStructure());
						this.records.add(recordCopy);
						originalRecordCreated = true;
					}
					recordCopy.getFields().add(field);
				} else {
					insertFiedInDocRecord(field, block);
				}
			}
			
//			RecordStructure block = structure.findRecord(field.getName());
//			if (block == null) {
//				if (! structure.recordExistsInStructure(record.getName()))
//				{
//					this.insertField(field);
//				}else
//					{
//						if (!originalRecordCreated) {
//						recordCopy = new Record(record.getName());
//						recordCopy.setParent(this);
//						recordCopy.setStructure(record.getStructure());
//						this.records.add(recordCopy);
//						originalRecordCreated = true;
//						}
//						recordCopy.getFields().add(field);
//					}
//			} else {
//				insertFiedInDocRecord(field, block);
//			}
		}
	}

	public void insertField(Field field) {

		if (this.fieldPresent(field, false))
			return;// Field object instance already present

		if (structure == null)
			this.fields.add(field);
		else {
			RecordStructure block = structure.findRecord(field.getName());
			if (block == null) {
				this.fields.add(field);
			} else {
				insertFiedInDocRecord(field, block);
			}
		}
	}

	@Override
	public String toXML() {
		StringBuffer sb = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append(super.toXML());
		return sb.toString();
	}


}
