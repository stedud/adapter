package fr.kw.adapter.parser.type.recordin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.matcher.StringMatcher;
import org.apache.commons.text.matcher.StringMatcherFactory;
import org.apache.pdfbox.util.Hex;

import fr.kw.adapter.parser.IParserConfiguration;
import fr.kw.adapter.parser.process.ParseProcessConfiguration;
import fr.kw.adapter.parser.script.Script;
import fr.kw.adapter.parser.script.ScriptEngineHelper;
import fr.kw.adapter.parser.type.fieldin.FieldParserConfiguration;
import fr.utils.LogHelper;
import fr.utils.Utils;

public class RecordParserConfiguration implements IParserConfiguration {

	private static final String RECORDIN = "STREAMIN";

	protected Map<String, String> metadata = new HashMap<String, String>();

	protected static String hexToAscii(String hexStr) {
		StringBuilder output = new StringBuilder("");
		int i = 0;
		while (i < hexStr.length()) {
			String str = hexStr.substring(i, i + 2);

			try {
				output.append(new String(Hex.decodeHex(str)));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			i += 2;
		}
		return output.toString();
	}

	public static Boolean isRecordDescriptor(File descriptionFile) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(descriptionFile));
			int lineNum = 0;
			String line;

			while ((line = br.readLine()) != null && lineNum < 10) {
				lineNum++;
				if (line.trim().toUpperCase().startsWith(RECORDIN)) {

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
					LogHelper.error(
							"Error while closing description file '" + descriptionFile + "' : " + e.getMessage(), e);
				}
		}

		return false;
	}

	protected static void parseEvent(String line, RecordDefinition record) {

		String events = StringUtils.substring(line, "EVENT".length()).trim();
		String[] eventList = StringUtils.split(events, ',');
		record.eventType = RecordType.EVENT;
		for (String it : eventList) {
			record.events.add(Utils.removeSurrounding(it.trim(), "\"").trim());
		}
	}

	protected static void parseField(String line, RecordDefinition record) {
		FieldRecordDefinition fieldDef = new FieldRecordDefinition();

		String[] tokens = StringUtils.split(line, '"');

		fieldDef.setName(tokens[0]);

		boolean startFound = false;

		for (int i = 1; i < tokens.length; i++) {
			String token = tokens[i];
			{
				if (NumberUtils.isDigits(token)) {
					if (!startFound) {
						fieldDef.posStartValue = NumberUtils.toInt(token);
						startFound = true;
					} else
						fieldDef.posEndValue = NumberUtils.toInt(token);

				} else if (token.compareToIgnoreCase("KEEPSP") == 0)
					fieldDef.keepSpace = true;
				else if (token.compareToIgnoreCase("KEEPLEADINGSP") == 0)
					fieldDef.keepLeadingSpace = true;
				else if (token.compareToIgnoreCase("KEEPTRAILINGSP") == 0)
					fieldDef.keepTrailingSpace = true;
			}
		}
		record.fieldsDefinitions.add(fieldDef);
	}

	protected static void parseInEvent(String line, RecordDefinition record) {

		String events = StringUtils.substring(line, "INEVENT".length()).trim();
		String[] eventList = StringUtils.split(events, ',');
		record.eventType = RecordType.IN_EVENT;
		for (String it : eventList) {
			record.events.add(Utils.removeSurrounding(it.trim(), "\"").trim());
		}
	}

	protected static void parseNewEvent(String line, RecordDefinition record) {

		String events = StringUtils.substring(line, "NEWEVENT".length()).trim();
		String[] eventList = StringUtils.split(events, ',');
		record.eventType = RecordType.NEW_EVENT;
		for (String it : eventList) {
			record.events.add(Utils.removeSurrounding(it.trim(), "\"").trim());
		}
	}

	protected static RecordDefinition parseRecord(String line, RecordParserConfiguration descriptor) {

		RecordDefinition recordDef = new RecordDefinition(descriptor);

		StringMatcher delimMatcher = StringMatcherFactory.INSTANCE.splitMatcher();
		StringMatcher quoteMatcher = StringMatcherFactory.INSTANCE.doubleQuoteMatcher();
		org.apache.commons.text.StringTokenizer tokenizer = new org.apache.commons.text.StringTokenizer(line,
				delimMatcher, quoteMatcher);

		String[] tokensTmp = tokenizer.getTokenArray();

		String keyword = "";
		for (String it : tokensTmp) {
			if (StringUtils.isBlank(keyword)) {
				if (StringUtils.containsAny(it.toLowerCase(), "record", "chrsep", "fixpos")) {
					keyword = it.toLowerCase();
				}
			} else {

				if (StringUtils.equalsIgnoreCase(keyword, "record")) {
					recordDef.id = it;
					recordDef.pattern = Pattern.compile("\\Q" + it + "\\E.*");// par defaut
				} else if (StringUtils.equalsIgnoreCase(keyword, "chrsep")) {
					recordDef.separator = parseSeparator(it);
				} else if (StringUtils.equalsIgnoreCase(keyword, "fixpos")) {
					recordDef.delimited = false;
				}
				keyword = "";

			}
		}

		return recordDef;
	}

	public static String parseSeparator(String separator) {
		// ex: , ; <OC>
		StringBuffer delimiter = new StringBuffer();
		String[] values = StringUtils.split(separator, '<');
		for (String val1 : values) {
			String[] finalValues = StringUtils.split(val1, '>');
			int tokenNumber = 0;
			for (String it : finalValues) {
				if (val1.contains(">") && tokenNumber == 0) {
					delimiter.append(hexToAscii(it.toLowerCase()));
				} else
					delimiter.append(it);

				tokenNumber++;
			}

		}
		return delimiter.toString();
	}

	protected static String parseStreamin(String line) {// STREAMIN "ID"

		String id = StringUtils.substring(line, "STREAMIN".length()).trim();
		id = StringUtils.removeStart(StringUtils.removeEnd(id, "\""), "\"");
		return id;
	}

	protected static String parseCharset(String line) {// CHARSET "ISO-8859-1"

		String charset = StringUtils.substring(line, "ENCODING".length()).trim();
		charset = StringUtils.removeStart(StringUtils.removeEnd(charset, "\""), "\"");
		return charset;
	}

	protected String id = "";

	protected String charset = null;

	protected List<RecordDefinition> recordsDefinitions = new ArrayList<RecordDefinition>();

	protected boolean typePrefix = false;

	protected ParseProcessConfiguration rootConfiguration;

	public RecordParserConfiguration() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public ParseProcessConfiguration getRootConfiguration() {

		return rootConfiguration;
	}

	@Override
	public void load(File descriptionFile, ParseProcessConfiguration rootConfig) throws IOException {
		RecordParserConfiguration descriptor = this;

		this.rootConfiguration = rootConfig;
		RecordDefinition currentRecord = null;
		
		

		List<String> lines = FileUtils.readLines(descriptionFile, StandardCharsets.UTF_8.name());

		StringBuffer script = new StringBuffer();
		String scriptType = "js";
		int nbOpenedBRackets = 0;

		for (String line : lines) {
			line = StringUtils.substringBefore(line, "//");
			line = line.trim();

			if (nbOpenedBRackets == 0)
				line = StringUtils.removeEnd(line.trim(), ";").trim();

			if (nbOpenedBRackets > 0) {
				String scriptLine = line.trim();

				nbOpenedBRackets = countOpenedBrackets(nbOpenedBRackets, scriptLine);
				if (nbOpenedBRackets == 0) {
					scriptLine = StringUtils.removeEnd(scriptLine, "}");
				}
				script.append("\n");
				script.append(scriptLine);
				if (nbOpenedBRackets == 0) {
					Script recordScript = new Script();
					recordScript.setExtension(scriptType);
					recordScript.setScript(script.toString());
					currentRecord.script = recordScript;
		
					script = new StringBuffer();
				}
			} else if (StringUtils.startsWithIgnoreCase(line, "STREAMIN")) {
				descriptor.id = parseStreamin(line);
			} else if (StringUtils.startsWithIgnoreCase(line, "TYPEPREFIX")) {
				descriptor.typePrefix = true;
			} else if (StringUtils.startsWithIgnoreCase(line, "MATCH")) {
				if (currentRecord != null) {

					if (StringUtils.startsWithIgnoreCase(line.substring("MATCH".length()).trim(), "VALUE")) {
						String pattern = StringUtils.split(line, '"')[1];
						currentRecord.pattern = Pattern.compile("\\Q" + pattern + "\\E.*");
					} else if (StringUtils.startsWithIgnoreCase(line.substring("MATCH".length()).trim(), "SCRIPT")) {

						if (StringUtils.contains(line, "SCRIPT")) {
							String afterScript = StringUtils.substringAfter(line, "SCRIPT");
							if (afterScript.startsWith(":")) {
								String language = afterScript.substring(1);
								language = StringUtils.substringBefore(language, "{");
								if (StringUtils.isNotBlank(language))
									scriptType = language.toLowerCase();
							}

						}

						String scriptLine = StringUtils.substringAfter(line, "{");

						scriptLine.trim();

						nbOpenedBRackets = countOpenedBrackets(nbOpenedBRackets, "{" + scriptLine);
						if (nbOpenedBRackets == 0) {
							scriptLine = StringUtils.removeEnd(scriptLine, "}");
						}
						script = new StringBuffer(scriptLine);

						if (nbOpenedBRackets == 0) {
							Script recordScript = new Script();
							recordScript.setExtension(scriptType);
							recordScript.setScript(script.toString());
							currentRecord.script = recordScript;
							script = new StringBuffer();
						}

					} else {
						String pattern = StringUtils.split(line, '"')[1];
						currentRecord.pattern = Pattern.compile(pattern + ".*");
					}
				}

			} else if (StringUtils.startsWithIgnoreCase(line, "NEWEVENT")) {
				if (currentRecord != null)
					parseNewEvent(line, currentRecord);

			} else if (StringUtils.startsWithIgnoreCase(line, "INEVENT")) {
				if (currentRecord != null)
					parseInEvent(line, currentRecord);

			} else if (StringUtils.startsWithIgnoreCase(line, "EVENT")) {
				if (currentRecord != null)
					parseEvent(line, currentRecord);

			} else if (StringUtils.startsWithIgnoreCase(line, "record")) {
				currentRecord = parseRecord(line, descriptor);
				descriptor.recordsDefinitions.add(currentRecord);

			} else if (StringUtils.startsWithIgnoreCase(line, "CHARSET")) {
				descriptor.charset = parseCharset(line);
			} else if (StringUtils.startsWithIgnoreCase(line, "METADATA")) {
				Map<String, String> metadataList = FieldParserConfiguration.parseMetadata(line);
				this.metadata.putAll(metadataList);
			} else if (StringUtils.startsWithIgnoreCase(line, "\"")) {
				if (currentRecord != null)
					parseField(line, currentRecord);

			}

		}
		if (descriptor != null)

			if (StringUtils.isBlank(descriptor.charset)) {

				descriptor.charset = rootConfig.get("ENCODING", StandardCharsets.UTF_8.name());
			}

	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "RecordParserConfiguration [id=" + id + "]";
	}

	protected int countOpenedBrackets(int startFrom, String line) {
		int counter = startFrom;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '{')
				counter++;
			else if (c == '}')
				counter--;
		}
		return counter;
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

}
