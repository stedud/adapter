package fr.kw.adapter.parser.type.fieldin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import fr.kw.adapter.parser.IParserConfiguration;
import fr.kw.adapter.parser.process.ParseProcessConfiguration;
import fr.kw.adapter.parser.type.recordin.RecordParserConfiguration;
import fr.utils.LogHelper;
import fr.utils.Utils;

public class FieldParserConfiguration implements IParserConfiguration {

	protected Map<String, String> metadata = new HashMap<String, String>();

	private static final String ALIAS_EVENT_PATH = "AliasEventPath";
	private static final String ALWAYS_CREATE_FIELD = "AlwaysCreateField";
	private static final String BEST_MATCH_EVENT = "BestMatchEvent";
	private static final String CHR_SEP_EVENT = "ChrSepEvent";
	private static final String CHR_SEP_FIELD = "ChrSepField";
	private static final String CHR_SEP_PREFIX = "ChrSepPrefix";
	private static final String CHR_START_VARIABLE = "ChrStartVariable";
	private static final String END_EVENT_DESC = "EndEventDesc";
	private static final String END_VALUE_DESC = "EndValueDesc";
	private static final String FIELD_CONT_STRING = "FieldContString";
	private static final String FIELDIN = "FIELDIN";
	private static final String FIX_LEN_EVENT = "FixLenEvent";
	private static final String FIX_LEN_FIELD = "FixLenField";
	private static final String IGNORE_BLANK_FIELD_VALUES = "IgnoreBlankFieldValues";
	private static final String KEEP_FIELD_SPACES = "KeepFieldSpaces";
	private static final String LABEL_END_EVENT = "LabelEndEvent";
	private static final String LABEL_FIELD_CONT = "LabelFieldCont";
	private static final String LABEL_PAGE_BREAK = "LabelPageBreak";
	private static final String LABEL_PREFIX = "LabelPrefix";
	private static final String LABEL_START_EVENT = "LabelStartEvent";
	private static final String LABEL_START_VARIABLE = "LabelStartVariable";
	private static final String POS_EVENT = "PosEvent";
	private static final String POS_FIELD = "PosField";
	private static final String POS_LABEL_END_EVENT = "PosLabelEndEvent";
	private static final String POS_LABEL_FIELD_CONT = "PosLabelFieldCont";
	private static final String POS_LABEL_PREFIX = "PosLabelPrefix";
	private static final String POS_LABEL_START_EVENT = "PosLabelStartEvent";
	private static final String POS_LABEL_START_VARIABLE = "PosLabelStartVariable";
	private static final String POS_PAGE_BREAK = "PosPageBreak";
	private static final String POS_PREFIX = "PosPrefix";
	private static final String POS_VALUE = "PosValue";
	private static final String SCRIPT_EVENT = "ScriptEvent";
	private static final String SCRIPT_FIELD = "ScriptField";
	private static final String SCRIPT_PREFIX = "ScriptPrefix";
	private static final String POS_LABEL_START_INCLUDE = "PosStartInclude";
	private static final String POS_LABEL_END_INCLUDE = "PosEndInclude";
	private static final String LABEL_END_INCLUDE = "LabelEndInclude";
	private static final String LABEL_START_INCLUDE = "LabelStartInclude";
	private static final String INCLUDE_CONT_STRING = "IncludeContString";
	private static final String CHARSET = "ENCODING";
	private static final String METADATA = "METADATA";

	public static Boolean isFieldDescriptor(File descriptionFile) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(descriptionFile));
			int lineNum = 0;
			String line;

			while ((line = br.readLine()) != null && lineNum < 10) {
				lineNum++;
				if (line.trim().toUpperCase().startsWith(FIELDIN)) {

					return true;
				}
			}
		} catch (IOException e) {
			LogHelper.error("Error while loading description file '" + descriptionFile + "' : " + e.getMessage(), e);
		} finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {
					LogHelper.error("Error while closing " + descriptionFile + " : " + e.getMessage(), e);

				}
		}

		return false;
	}

	private String charset = null;

	@Override
	public void load(File descriptionFile, ParseProcessConfiguration mainConf) throws IOException {
		this.rootConfiguration = mainConf;
		FieldParserConfiguration descriptor = this;

		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(descriptionFile));
			String line = null;
			while ((line = br.readLine()) != null) {
				line = StringUtils.substringBefore(line, "//");
				line = StringUtils.trimToEmpty(line);
				line = StringUtils.removeEnd(line.trim(), ";");
				if (StringUtils.startsWithIgnoreCase(line, FIELDIN)) {
					descriptor.id = parseFieldin(line);

				} else if (StringUtils.startsWithIgnoreCase(line, LABEL_START_EVENT)) {
					descriptor.labelStartEvent = parseLabelStartEvent(line);

				} else if (StringUtils.startsWithIgnoreCase(line, POS_LABEL_START_EVENT)) {
					descriptor.posLabelStartEvent = parsePosLabelStartEvent(line);
				} else if (StringUtils.startsWithIgnoreCase(line, END_EVENT_DESC)) {
					descriptor.endEventDesc = parseEndEventDesc(line);
				} else if (StringUtils.startsWithIgnoreCase(line, LABEL_END_EVENT)) {
					descriptor.labelEndEvent = parseLabelEndEvent(line);
				} else if (StringUtils.startsWithIgnoreCase(line, POS_LABEL_END_EVENT)) {
					descriptor.posLabelEndEvent = parsePosLabelEndEvent(line);
				} else if (StringUtils.startsWithIgnoreCase(line, LABEL_PAGE_BREAK)) {
					// NOT supported
				} else if (StringUtils.startsWithIgnoreCase(line, POS_PAGE_BREAK)) {
					// NOT supported
				} else if (StringUtils.startsWithIgnoreCase(line, POS_EVENT)) {
					descriptor.posEvent = parsePosEvent(line);
				} else if (StringUtils.startsWithIgnoreCase(line, CHR_SEP_EVENT)) {
					descriptor.chrSepEvent = parseChrSepEvent(line);
				} else if (StringUtils.startsWithIgnoreCase(line, FIX_LEN_EVENT)) {
					descriptor.fixLenEvent = parseFixLenEvent(line);
				} else if (StringUtils.startsWithIgnoreCase(line, BEST_MATCH_EVENT)) {
					// NOT supported
				} else if (StringUtils.startsWithIgnoreCase(line, ALIAS_EVENT_PATH)) {
					// NOT supported
				} else if (StringUtils.startsWithIgnoreCase(line, SCRIPT_EVENT)) {
					// NOT supported
				} else if (StringUtils.startsWithIgnoreCase(line, POS_FIELD)) {
					descriptor.posField = parsePosField(line);
				} else if (StringUtils.startsWithIgnoreCase(line, FIX_LEN_FIELD)) {
					descriptor.fixLenField = parseFixLenField(line);
				} else if (StringUtils.startsWithIgnoreCase(line, CHR_SEP_FIELD)) {
					descriptor.chrSepField = parseChrSepField(line);
				} else if (StringUtils.startsWithIgnoreCase(line, SCRIPT_FIELD)) {
					// NOT supported
				} else if (StringUtils.startsWithIgnoreCase(line, LABEL_PREFIX)) {
					descriptor.labelPrefix = parseLabelPrefix(line);
				} else if (StringUtils.startsWithIgnoreCase(line, POS_LABEL_PREFIX)) {
					descriptor.posLabelPrefix = parsePosLabelPrefix(line);
				} else if (StringUtils.startsWithIgnoreCase(line, POS_PREFIX)) {
					descriptor.posPrefix = parsePosPrefix(line);
				} else if (StringUtils.startsWithIgnoreCase(line, CHR_SEP_PREFIX)) {
					descriptor.chrSepPrefix = parseChrSepPrefix(line);
				} else if (StringUtils.startsWithIgnoreCase(line, SCRIPT_PREFIX)) {
					// NOT supported
				} else if (StringUtils.startsWithIgnoreCase(line, LABEL_START_VARIABLE)) {
					// NOT supported
				} else if (StringUtils.startsWithIgnoreCase(line, POS_LABEL_START_VARIABLE)) {
					// NOT supported
				} else if (StringUtils.startsWithIgnoreCase(line, CHR_START_VARIABLE)) {
					// NOT supported
				} else if (StringUtils.startsWithIgnoreCase(line, LABEL_FIELD_CONT)) {
					descriptor.labelFieldCont = parseLabelFieldCont(line);
				} else if (StringUtils.startsWithIgnoreCase(line, POS_LABEL_FIELD_CONT)) {
					descriptor.posLabelFieldCont = parsePosLabelFieldCont(line);
				} else if (StringUtils.startsWithIgnoreCase(line, FIELD_CONT_STRING)) {
					descriptor.fieldContString = parseFieldContString(line);
				} else if (StringUtils.startsWithIgnoreCase(line, POS_VALUE)) {
					descriptor.posStartValue = parsePosValue(line);
				} else if (StringUtils.startsWithIgnoreCase(line, IGNORE_BLANK_FIELD_VALUES)) {
					// NOT supported
				} else if (StringUtils.startsWithIgnoreCase(line, KEEP_FIELD_SPACES)) {
					descriptor.keepSpace = parseKeepFieldSpaces(line);
				} else if (StringUtils.startsWithIgnoreCase(line, END_VALUE_DESC)) {
					descriptor.posEndValue = parseEndValueDesc(line);
				} else if (StringUtils.startsWithIgnoreCase(line, POS_LABEL_START_INCLUDE)) {
					descriptor.posLabelStartInclude = parsePosLabelStartInclude(line);
				} else if (StringUtils.startsWithIgnoreCase(line, POS_LABEL_END_INCLUDE)) {
					descriptor.posLabelEndInclude = parsePosLabelEndInclude(line);
				} else if (StringUtils.startsWithIgnoreCase(line, LABEL_START_INCLUDE)) {
					descriptor.labelStartInclude = parseLabelStartInclude(line);
				} else if (StringUtils.startsWithIgnoreCase(line, LABEL_END_INCLUDE)) {
					descriptor.labelEndInclude = parseLabelEndInclude(line);
				} else if (StringUtils.startsWithIgnoreCase(line, LABEL_END_INCLUDE)) {
					descriptor.labelEndInclude = parseLabelEndInclude(line);
				} else if (StringUtils.startsWithIgnoreCase(line, INCLUDE_CONT_STRING)) {
					descriptor.includeContString = parseIncludeContString(line);
				} else if (StringUtils.startsWithIgnoreCase(line, CHARSET)) {
					descriptor.charset = parseCharset(line);
				} else if (StringUtils.startsWithIgnoreCase(line, METADATA)) {

					Map<String, String> metadata = parseMetadata(line);
					for (Entry<String, String> entry : metadata.entrySet()) {
						descriptor.getMetadata().putAll(metadata);
					}
				}

			}
		} catch (FileNotFoundException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			if (descriptor != null)

				if (StringUtils.isBlank(descriptor.charset)) {

					descriptor.charset = mainConf.get(CHARSET, StandardCharsets.UTF_8.name());
				}
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {
					LogHelper.error("Error while closing " + descriptionFile + " : " + e.getMessage(), e);
				}
		}

	}

	private static String parseChrSepEvent(String line) {
		String value = Utils.parseSingleArgKeyWord(line, "ChrSepEvent");
		value = RecordParserConfiguration.parseSeparator(value);
		return value;
	}

	private static String parseLabelStartInclude(String line) {
		return Utils.parseSingleArgKeyWord(line, "LabelStartInclude");
	}

	private static String parseIncludeContString(String line) {
		String value = Utils.parseSingleArgKeyWord(line, "IncludeContString");
		value = RecordParserConfiguration.parseSeparator(value);
		return value;
	}

	private static String parseLabelEndInclude(String line) {
		return Utils.parseSingleArgKeyWord(line, "LabelEndInclude");
	}

	private static String parseChrSepField(String line) {
		String value = Utils.parseSingleArgKeyWord(line, "ChrSepField");
		value = RecordParserConfiguration.parseSeparator(value);
		return value;
	}

	private static String parseChrSepPrefix(String line) {
		String value = Utils.parseSingleArgKeyWord(line, "ChrSepPrefix");
		value = RecordParserConfiguration.parseSeparator(value);
		return value;
	}

	private static String parseEndEventDesc(String line) {
		return Utils.parseSingleArgKeyWord(line, "EndEventDesc");
	}

	private static Integer parseEndValueDesc(String line) {
		String value = Utils.parseSingleArgKeyWord(line, END_VALUE_DESC);
		return NumberUtils.toInt(value);
	}

	private static Integer parsePosLabelStartInclude(String line) {
		String value = Utils.parseSingleArgKeyWord(line, POS_LABEL_START_INCLUDE);
		return NumberUtils.toInt(value);
	}

	private static Integer parsePosLabelEndInclude(String line) {
		String value = Utils.parseSingleArgKeyWord(line, POS_LABEL_END_INCLUDE);
		return NumberUtils.toInt(value);
	}

	private static String parseFieldContString(String line) {
		String value = Utils.parseSingleArgKeyWord(line, "FieldContString");
		value = RecordParserConfiguration.parseSeparator(value);
		return value;
	}

	private static String parseFieldin(String line) {// FieldIn "ID"
		return Utils.parseSingleArgKeyWord(line, "FIELDIN");
	}

	private static Integer parseFixLenEvent(String line) {
		String value = Utils.parseSingleArgKeyWord(line, "FixLenEvent");
		return NumberUtils.toInt(value);
	}

	private static Integer parseFixLenField(String line) {
		String value = Utils.parseSingleArgKeyWord(line, "FixLenField");
		return NumberUtils.toInt(value);
	}

	private static Boolean parseKeepFieldSpaces(String line) {
		return true;
	}

	private static String parseLabelEndEvent(String line) {
		return Utils.parseSingleArgKeyWord(line, "LabelEndEvent");
	}

	private static String parseLabelFieldCont(String line) {
		String value = Utils.parseSingleArgKeyWord(line, "LabelFieldCont");
		value = RecordParserConfiguration.parseSeparator(value);
		return value;
	}

	private static String parseLabelPrefix(String line) {
		String value = Utils.parseSingleArgKeyWord(line, "LabelPrefix");
		return value;
	}

	private static String parseLabelStartEvent(String line) {// labelStartEvent "BEGIN";
		return Utils.parseSingleArgKeyWord(line, "labelStartEvent");
	}

	private static String parseCharset(String line) {// CHARSET "ISO-8859-1";
		return Utils.parseSingleArgKeyWord(line, CHARSET);
	}

	private static Integer parsePosEvent(String line) {
		String value = Utils.parseSingleArgKeyWord(line, "PosEvent");
		return NumberUtils.toInt(value);
	}

	private static Integer parsePosField(String line) {
		String value = Utils.parseSingleArgKeyWord(line, "PosField");
		return NumberUtils.toInt(value);
	}

	private static Integer parsePosLabelEndEvent(String line) {
		String posLabel = Utils.parseSingleArgKeyWord(line, "PosLabelEndEvent");
		return NumberUtils.toInt(posLabel);
	}

	private static Integer parsePosLabelFieldCont(String line) {
		String value = Utils.parseSingleArgKeyWord(line, "PosLabelFieldCont");
		return NumberUtils.toInt(value);
	}

	private static Integer parsePosLabelPrefix(String line) {
		String value = Utils.parseSingleArgKeyWord(line, "PosLabelPrefix");
		return NumberUtils.toInt(value);
	}

	private static Integer parsePosLabelStartEvent(String line) {
		String posLabel = Utils.parseSingleArgKeyWord(line, "posLabelStartEvent");
		return NumberUtils.toInt(posLabel);
	}

	private static Integer parsePosPrefix(String line) {
		String value = Utils.parseSingleArgKeyWord(line, "PosPrefix");
		return NumberUtils.toInt(value);
	}

	private static Integer parsePosValue(String line) {
		String value = Utils.parseSingleArgKeyWord(line, "PosValue");
		return NumberUtils.toInt(value);
	}

	public static Map<String, String> parseMetadata(String line) {// METADATA "jobId,inputFile";
		String metadataList = Utils.parseSingleArgKeyWord(line, METADATA);
		String[] metadataArr = StringUtils.split(metadataList, ',');
		Map<String, String> metadata = new HashMap<String, String>();
		for (String key : metadataArr) {
			metadata.put(key, key);
		}

		return metadata;
	}

	protected String chrSepEvent = "";

	protected String endEventDesc = "\n";

	protected Integer fixLenEvent = 0;

	protected String id = "";

	protected String labelEndEvent = "";

	protected String labelStartEvent = "";

	protected Integer posEvent = 0;

	protected Integer posLabelEndEvent = 0;

	protected Integer posLabelStartEvent = 0;

	protected Boolean typePrefix = false;

	protected Integer posField = 0;
	protected Integer fixLenField = 0;
	protected String chrSepField = "";
	protected String labelPrefix = "";
	protected Integer posLabelPrefix = 0;
	protected Integer posPrefix = 0;
	protected String chrSepPrefix = "";
	protected String labelFieldCont = "";
	protected Integer posLabelFieldCont = 0;
	protected String fieldContString = "";
	protected Integer posStartValue = 0;
	protected Boolean keepSpace = false;
	protected Integer posEndValue = 0;
	protected Integer posLabelStartInclude = 0;
	protected Integer posLabelEndInclude = 0;
	protected String labelStartInclude = "";
	protected String labelEndInclude = "";
	protected String includeContString = " ";

	private ParseProcessConfiguration rootConfiguration;

	public FieldParserConfiguration() {
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "RecordParserConfiguration [id=" + getId() + "]";
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public ParseProcessConfiguration getRootConfiguration() {
		return rootConfiguration;
	}
}
