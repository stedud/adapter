package fr.kw.adapter.parser.type.rdi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.script.ScriptEngine;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import fr.kw.adapter.parser.DataDescriptionException;
import fr.kw.adapter.parser.IParser;
import fr.kw.adapter.parser.event.Event;
import fr.kw.adapter.parser.event.Field;
import fr.kw.adapter.parser.event.Record;
import fr.kw.adapter.parser.event.structure.EventStructure;
import fr.utils.LogHelper;
import fr.utils.Utils;

public class R3rdiParser implements IParser<Record, String> {

	protected Map<String, String> metadata = new HashMap<String, String>();
	protected R3rdiParserConfiguration config;
	protected String charset = null;
	protected boolean includeMode = false;
	protected String includeFieldTail = "";
	protected String includeFieldPrefix = "";
	private Field includeField = null;

	static Properties headerProperties = new Properties();
	static Properties archiveParameters = new Properties();
	static Properties archiveIndex = new Properties();

	static {
		try {
			ClassLoader classLoader = R3rdiParser.class.getClassLoader();
			InputStream stream = classLoader
					.getSystemResourceAsStream("fr/kw/adapter/parser/type/rdi/RDIHeader.properties");
			if (stream == null)
				throw new IOException("Could not find resource in classpath RDIHeader.properties");
			headerProperties.load(stream);
			stream.close();

			stream = classLoader
					.getSystemResourceAsStream("fr/kw/adapter/parser/type/rdi/RDIArchiveParameters.properties");
			if (stream == null)
				throw new IOException("Could not find resource in classpath RDIArchiveParameters.properties");
			archiveParameters.load(stream);
			stream.close();

			stream = classLoader.getSystemResourceAsStream("fr/kw/adapter/parser/type/rdi/RDIArchiveIndex.properties");
			if (stream == null)
				throw new IOException("Could not find resource in classpath RDIArchiveIndex.properties");
			archiveIndex.load(stream);
			stream.close();

		} catch (IOException e) {
			LogHelper.error("Error while loading SAP configuration files : " + e.getMessage(), e);
		}

	}

	public static void main(String[] args) {
		System.out.println("test");
	}

	public R3rdiParser(R3rdiParserConfiguration config) {
		this.config = config;
	}

	@Override
	public Event parseEvent(Event currentEvent, String eventCandidate, ScriptEngine scriptEngine) throws DataDescriptionException {
		RdiLineType type = getType(eventCandidate);
		if (type != RdiLineType.HEADER)
			return null;

		Map<String, Field> fields = parseHeaders(eventCandidate, headerProperties);
		Record header = new Record("header");
		header.getFields().addAll(fields.values());
		String eventName = fields.get("r3_formname").getValue().trim();
		eventName = Utils.normalizeXmlName(eventName);
		Event newEvent = new Event(eventName);
		if (StringUtils.isNotBlank(newEvent.getName())) {
			// Search for EventStructure
			File eventStructureFile = this.config.getRootConfiguration().getEventStructure(newEvent.getName());
			LogHelper.info("Event " + newEvent.getName() + ", structure file:" + eventStructureFile.getAbsolutePath());
			if (eventStructureFile.canRead()) {
				LogHelper.info("Loading structure file:" + eventStructureFile.getPath());
				EventStructure eventStructure = EventStructure.load(eventStructureFile);
				newEvent.setStructure(eventStructure);
			}
		}
		newEvent.insertRecord(header);

		return newEvent;
	}

	@Override
	public Record parseFields(Event currentEvent, Record currentRecord, String line, ScriptEngine scriptEngine) throws DataDescriptionException {
		switch (getType(line)) {
		case DATA:
			if (includeMode) {
				this.appendIncludeField(line);
				return null;
			} else {
				if (this.config.isSimple()) {

					// simple mode
					String fieldName = StringUtils.substringBefore(line, " ");
					String recordName = StringUtils.substringBefore(fieldName, "-");
					recordName = Utils.normalizeXmlName(recordName);
					String fieldValue = StringUtils.substringAfter(line, " ");
					fieldName = Utils.normalizeXmlName(fieldName);
					Field f = new Field(fieldName);

					f.setValue(fieldValue);

					return manageFieldRecord(f, currentRecord, recordName);

				} else {// standard mode
					String field = StringUtils.substring(line, 1, 1 + 8).trim();
					String recordName = field;

					if (StringUtils.isBlank(field))
						return null;

					if (field.trim().contains(" ")) {
						return null;
					} else {
						String field2 = StringUtils.substring(line, 11, 11 + 29).trim();
						String field3 = StringUtils.substring(line, 41, 41 + 130).trim();
						if (StringUtils.isNotBlank(field2)) {
							field = field + "_" + field2;
							recordName = recordName + "_" + field2;
						}
						if (StringUtils.isNotBlank(field3))
							field = field + "_" + field3;
					}

					String fieldValue = StringUtils.substring(line, 175);
					field = Utils.normalizeXmlName(field);
					Field f = new Field(field);

					f.setValue(fieldValue);
					return manageFieldRecord(f, currentRecord, recordName);
				}
			}
			// break;
		case PARAMETER: {
			Map<String, Field> fields = parseHeaders(line, archiveParameters);

			Record newRecord = new Record("archiveParameters");
			newRecord.getFields().addAll(fields.values());
			return newRecord;
			// break;
		}
		case ARCHIVE: {
			Map<String, Field> fields = parseHeaders(line, archiveIndex);

			Record newRecord = new Record("archiveIndex");
			newRecord.getFields().addAll(fields.values());
			return newRecord;
			// break;
		}
		case INCLUDE_BEGIN:
			includeMode = true;
			includeFieldTail = parseIncludeFieldTail(line);
			includeFieldTail = Utils.normalizeXmlName(includeFieldTail);
			return null;
		// break;
		case INCLUDE_END:
			if (includeField != null) {
				Record r = new Record(includeFieldPrefix);
				r.getFields().add(includeField);
				includeField = null;
				includeMode = false;
				includeFieldPrefix = "";
				includeFieldTail = "";
				return r;
			} else
				return null;
			// break;
		case CONTROL: {
			List<Field> fields = parseControl(line);
			Record newRecord = new Record("control");
			newRecord.getFields().addAll(fields);
			return newRecord;

			// break;
		}
		case SORT:
			// Ignored
			break;

		case HEADER:
			throw new DataDescriptionException(
					"Found Header record in parseFields() function. Header record should be caught by parseEvent() function");
		// Should not happen. Headers must be caught by parseEvent() method
		// break;
		case NONE:
			// Do nothing...
			break;

		}

		return null;
	}

	protected Record manageFieldRecord(Field f, Record currentRecord, String fieldRecordName) {
		if (currentRecord != null) {
			if (currentRecord.getName().compareTo(fieldRecordName) == 0) {
				if (!currentRecord.containsField(f.getName())) {
					currentRecord.getFields().add(f);
					return currentRecord;
				}
			}
			Record r = new Record(fieldRecordName);
			r.getFields().add(f);
			return r;

		} else {
			Record r = new Record(fieldRecordName);
			r.getFields().add(f);
			return r;
		}
	}

	@Override
	public String getCharset() {

		return charset;
	}

	@Override
	public void setCharset(String charsetName) {
		this.charset = charsetName;
	}

	/**
	 * For RDI data, the line type depends on the first characters value (ex : H =
	 * header, D = Data, P = parameter, CINC-BEGIN=include begin, ...
	 * 
	 * @param line
	 * @return
	 */
	public RdiLineType getType(String line) {
		String firstChar = StringUtils.left(line, 1);
		switch (firstChar) {
		case "D":
			return RdiLineType.DATA;
		case "H":
			return RdiLineType.HEADER;
		case "S":
			return RdiLineType.SORT;
		case "I":
			return RdiLineType.ARCHIVE;
		case "P":
			return RdiLineType.PARAMETER;
		case "C":
			if (StringUtils.startsWith(line, "CINC-BEGIN"))
				return RdiLineType.INCLUDE_BEGIN;
			if (StringUtils.startsWith(line, "CINC-END"))
				return RdiLineType.INCLUDE_END;
			return RdiLineType.CONTROL;
		default:
			return RdiLineType.NONE;

		}
	}

	/**
	 * The "header" lines are those starting with "H" (first line of document), "I"
	 * (index archive), "P" (parameter archive). For those records, the fields are
	 * alwys the same in same position. Positions and fields are listed in
	 * corresponding properties files : RDIHeader.properties,
	 * RDIArchiveIndex.properties, RDIArchiveParameters.properties In properties
	 * files, fields are entered in the following way : fieldName=startPos,lenField
	 * ex: r3_formname=22,16 -> r3_formname starts at position 22 and is 16 digits
	 * length
	 * 
	 * @param line
	 * @param props
	 * @return
	 */
	public Map<String, Field> parseHeaders(String line, Properties props) {
		Map<String, Field> fields = new HashMap<String, Field>();

		if (props == null)
			return fields;

		for (Entry<Object, Object> entry : props.entrySet()) {
			String[] positions = StringUtils.split(String.valueOf(entry.getValue()), ',');
			String name = String.valueOf(entry.getKey());
			name = Utils.normalizeXmlName(name);
			Field f = new Field(name);
			int start = NumberUtils.toInt(positions[0]) - 1;
			int length = NumberUtils.toInt(positions[1]);
			String value = StringUtils.substring(line, start, start + length);
			f.setValue(value);
			fields.put(f.getName(), f);
		}

		return fields;
	}

	protected List<Field> parseControl(String line) {
		List<Field> fields = new ArrayList<Field>();

		if (line == null)
			return fields;

		StringTokenizer tokenizer = new StringTokenizer(line.substring(1), " ");

		Field f = null;

		while (tokenizer.hasMoreTokens()) {
			if (f == null) {
				String name = "r3_" + tokenizer.nextToken();
				name = Utils.normalizeXmlName(name);
				f = new Field(name);

				f.setValue("");
				fields.add(f);
			} else {
				f.setValue(tokenizer.nextToken());
				f = null;
			}
		}

		return fields;
	}

	private String parseIncludeFieldTail(String line) {
		// Returns the trailer of the field name
		// ex: CINC-BEGIN 450000482300010 EKPO F03 DE --> EKPO_F03
		String[] tokens = StringUtils.split(line, " ");
		return tokens[2] + "_" + tokens[3];
	}

	private void appendIncludeField(String line) {
		// DMAIN ITEM_TEXT 029This is the Material PO text
		String[] tokens = StringUtils.split(StringUtils.substring(line, 1), " ");
		if (includeField == null) {
			includeFieldPrefix = tokens[0] + "_" + tokens[1];
			includeFieldPrefix = Utils.normalizeXmlName(includeFieldPrefix);
			String name = includeFieldPrefix + "_" + includeFieldTail;
			name = Utils.normalizeXmlName(name);
			includeField = new Field(name);
		}
		int dataStart = StringUtils.indexOf(line, tokens[2]);
		String data = StringUtils.substring(line, dataStart + 3);
		includeField.setValue(includeField.getValue() + data);

	}

	public R3rdiParserConfiguration getConfig() {
		return config;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

}
