package fr.kw.adapter.parser.type.recordin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;

import org.apache.commons.lang3.StringUtils;

import fr.kw.adapter.parser.DataDescriptionException;
import fr.kw.adapter.parser.IParser;
import fr.kw.adapter.parser.event.Event;
import fr.kw.adapter.parser.event.Field;
import fr.kw.adapter.parser.event.Record;
import fr.kw.adapter.parser.event.structure.EventStructure;
import fr.utils.LogHelper;
import fr.utils.Utils;

public class RecordParser implements IParser<Record, String> {

	protected RecordParserConfiguration config;

	protected String charset = StandardCharsets.UTF_8.name();

	public RecordParser(RecordParserConfiguration config) {
		this.config = config;
	}

	@Override
	public Event parseEvent(Event currentEvent, String eventCandidate, ScriptEngine scriptEngine) throws DataDescriptionException {
		for (RecordDefinition recDef : config.recordsDefinitions) {

			if (recDef.eventType == RecordType.NEW_EVENT
					|| (currentEvent == null && recDef.eventType == RecordType.EVENT)) {
				Map<String, Object> context = new HashMap<String, Object>();
				context.put("line", eventCandidate);
				context.put("currentEvent", currentEvent);

				if (recDef.match(eventCandidate, context, scriptEngine)) {
					String eventName = recDef.events.get(0);
					String recordName = recDef.id;
					recordName = Utils.normalizeXmlName(recordName);
					Event event = new Event(eventName);
					File eventStructureFile = this.config.getRootConfiguration().getEventStructure(eventName);
					LogHelper.info("Event " + eventName + ", structure file:" + eventStructureFile.getAbsolutePath());
					if (eventStructureFile.canRead()) {
						LogHelper.info("Loading structure file:" + eventStructureFile.getPath());
						EventStructure eventStructure = EventStructure.load(eventStructureFile);
						event.setStructure(eventStructure);
					}
					List<Field> fields = parseFields(recDef, eventCandidate);

					Record rec = new Record(recordName);
					rec.getFields().addAll(fields);

					event.insertRecord(rec);

					return event;
				}
			}
		}

		return null;
	}

	@Override
	public Record parseFields(Event currentEvent, Record currentRecord, String line, ScriptEngine scriptEngine) throws DataDescriptionException {

		Event event = currentEvent;
		Record rootRecord = currentRecord.getRootParent();
		if (event == null)
			return null;

		for (RecordDefinition recDef : config.recordsDefinitions) {
			Map<String, Object> context = new HashMap<String, Object>();
			context.put("line", line);
			context.put("currentEvent", currentEvent);
			context.put("currentRecord", currentRecord);

			if (recDef.match(line, context, scriptEngine)) {
				String recordName = recDef.id;
				recordName = Utils.normalizeXmlName(recordName);
				if (!recDef.events.contains(event.getName()))
					continue;// Le record n'est pas valide pour l'event en cours, on passe au type de record
								// suivant
				Record newRecord = new Record(recordName);
				List<Field> fields = parseFields(recDef, line);
				newRecord.getFields().addAll(fields);
				return newRecord;
			}
		}
		return null;
	}

	protected List<Field> parseFields(RecordDefinition recDef, String line) {
		List<Field> result = new ArrayList<Field>();

		if (recDef.delimited) {
			String[] fields = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, recDef.separator);

			for (int i = 1; i <= recDef.fieldsDefinitions.size(); i++) {

				FieldRecordDefinition def = recDef.fieldsDefinitions.get(i - 1);

				String name = recDef.parent.typePrefix ? recDef.id + "_" + def.getName() : def.getName();
				name = Utils.normalizeXmlName(name);
				Field f = new Field(name);
				result.add(f);

				LogHelper.debug("Field " + f.getName() + " created, " + i + "/" + fields.length);
				if (i - 1 <= fields.length - 1) {
					f.setValue(fields[i - 1]);
					LogHelper.debug(f.getName() + "=" + f.getValue());
				} else {
					f.setValue("");
				}

			}
		} else {

			for (FieldRecordDefinition def : recDef.fieldsDefinitions) {
				String name = config.typePrefix ? recDef.id + "_" + def.getName() : def.getName();
				name = Utils.normalizeXmlName(name);
				Field f = new Field(name);
				result.add(f);

				String value = StringUtils.substring(line, (def.posStartValue - 1), def.posEndValue);
				if (!def.keepSpace) {
					if (def.keepLeadingSpace && !def.keepTrailingSpace) {
						f.setValue(StringUtils.stripEnd(value, null));
					} else if (!def.keepLeadingSpace && def.keepTrailingSpace) {
						f.setValue(StringUtils.stripStart(value, null));
					} else if (!def.keepLeadingSpace && !def.keepTrailingSpace) {
						f.setValue(value.trim());
					} else
						f.setValue(value);
				} else {
					f.setValue(value.trim());
				}
			}
		}

		return result;
	}

	@Override
	public String getCharset() {

		return charset;
	}

	@Override
	public void setCharset(String charsetName) {
		this.charset = charsetName;
	}

	@Override
	public String toString() {
		return "RecordParser [config=" + config + ", charset=" + charset + "]";
	}

}
