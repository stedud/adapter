package fr.kw.adapter.parser.type.page.settings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.kw.adapter.parser.process.ParseProcessConfiguration;
import fr.kw.adapter.parser.type.page.settings.field.FieldDefinition;
import fr.kw.adapter.parser.type.page.settings.field.PageType;
import fr.kw.adapter.parser.type.page.settings.pattern.ANDPattern;
import fr.kw.adapter.parser.type.page.settings.pattern.MatchPattern;
import fr.kw.adapter.parser.type.page.settings.pattern.MatchPatternLink;
import fr.kw.adapter.parser.type.page.settings.pattern.NOTPattern;
import fr.kw.adapter.parser.type.page.settings.pattern.ORPattern;
import fr.kw.adapter.parser.type.page.settings.pattern.RulePattern;

public class PageParserDefinitionLoader implements DocumentParserDefinitionLoader {

	public PageParserDefinitionLoader() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public DocumentParserDefinition load(File file, ParseProcessConfiguration rootConfig) throws PageSettingsException {

		LineIterator lineIterator = null;

		DocumentParserDefinition documentDefinition = new DocumentParserDefinition(rootConfig);
		documentDefinition.setName(FilenameUtils.getBaseName(file.getName()));

		ByteArrayOutputStream lxf = new ByteArrayOutputStream();

		try {
			lineIterator = FileUtils.lineIterator(file, StandardCharsets.UTF_8.name());

			boolean inDefinition = false;

			while (lineIterator.hasNext()) {
				String line = ((String) lineIterator.nextLine()).trim();
				if (line.startsWith("<") || inDefinition) {
					if (!inDefinition) {
						inDefinition = true;
					}
					if (!line.startsWith("<!DOCTYPE")) {
						lxf.write(line.getBytes(StandardCharsets.UTF_8));
						lxf.write("\n".getBytes(StandardCharsets.UTF_8));
					}
				}
				if (line.startsWith("</s-lxfin>")) {
					inDefinition = false;
				}

			}
			lineIterator.close();
			// Le lxf dans StringBuffer

			DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder xmlBuilder = xmlFactory.newDocumentBuilder();
			Document xmlDoc = xmlBuilder.parse(new ByteArrayInputStream(lxf.toByteArray()));
			lxf = null;
			Element root = xmlDoc.getDocumentElement();
			NodeList pages = root.getElementsByTagName("page");

			if (pages.getLength() == 0)
				throw new PageSettingsException(
						"'page' element not found in PDFIN definition '" + documentDefinition.getName() + "'");

			for (int j = 0; j < pages.getLength(); j++) {
				PageDocumentDefinition pageDef = new PageDocumentDefinition();

				// pageDef.setName(documentDefinition.getName());
				Element pageElt = (Element) pages.item(j);
				boolean boxesFirst = false;
				String boxesFirstStr = pageElt.getAttribute("boxesFirst");
				if (StringUtils.isNotBlank(boxesFirstStr)) {
					boxesFirst = Boolean.parseBoolean(boxesFirstStr);
				}

				String xoffsetStr = pageElt.getAttribute("xoffset");
				int xoffset = 0;
				if (!StringUtils.isBlank(xoffsetStr)) {
					xoffset = Integer.parseInt(xoffsetStr);
				}
				String yoffsetStr = pageElt.getAttribute("yoffset");
				int yoffset = 0;
				if (!StringUtils.isBlank(yoffsetStr)) {
					yoffset = Integer.parseInt(yoffsetStr);
				}

				pageDef.setxOffset(xoffset);
				pageDef.setyOffset(yoffset);
				pageDef.setBoxesFirst(boxesFirst);

				String type = pageElt.getAttribute("type");
				if (StringUtils.isBlank(type))
					type = "ANY";
				pageDef.setPageType(PageType.valueOf(type));

				NodeList patterns = pageElt.getElementsByTagName("pattern");
				NodeList fields = pageElt.getElementsByTagName("field");

				String rule = pageElt.getAttribute("rule");
				RulePattern rulePattern = parseRule(rule);
				RulePattern currentDocPattern = pageDef.getMatchRule();
				pageDef.setMatchRule(rulePattern);

				for (int i = 0; i < patterns.getLength(); i++) {

					Element patternElt = (Element) patterns.item(i);
					String name = patternElt.getAttribute("name");
					MatchPattern matchPattern = new MatchPattern(name);

					String position = patternElt.getAttribute("position");
					String size = patternElt.getAttribute("size");
					String options = patternElt.getAttribute("options");
					String match = patternElt.getAttribute("match");

					String script = patternElt.getTextContent();

					matchPattern.setPosition(parsePointPt(position));
					matchPattern.setDimension(parsePointPt(size));
					if (!StringUtils.isBlank(options)) {
						if (options.compareTo("20") == 0)
							matchPattern.setWildcards(false);
						else
							matchPattern.setWildcards(true);
					}
					matchPattern.setPattern(match);
					matchPattern.setScript(script);

					pageDef.getMatchRule().declareMatchPattern(matchPattern);

				}

				for (int i = 0; i < fields.getLength(); i++) {

					Element fieldElt = (Element) fields.item(i);
					String name = fieldElt.getAttribute("name");
					FieldDefinition f = new FieldDefinition(name);

					String translate = fieldElt.getAttribute("translate");
					String position = fieldElt.getAttribute("position");
					String size = fieldElt.getAttribute("size");
					String options = fieldElt.getAttribute("options");
					String script = fieldElt.getTextContent();
					// String fieldType = fieldElt.getAttribute("type");
					// if (StringUtils.isBlank(fieldType)) fieldType = "ANY";
					// PageType pageType = PageType.valueOf(fieldType);

					f.getPage().setPageType(pageDef.getPageType());
					f.setTranslate(parsePointPt(translate));
					f.setPosition(parsePointPt(position));
					f.setDimension(parsePointPt(size));
					// f.getPage().setPageType(pageType);
					FieldOptions fieldOptions = getFieldOptions(options);
					f.setKeepSpace(fieldOptions.keepSpace);
					f.setRemoveText(fieldOptions.removeText);

					f.setScript(script);

					pageDef.getFields().add(f);

				}
				documentDefinition.getPageDefinitions().add(pageDef);
			}

		} catch (IOException e) {
			throw new PageSettingsException("Could not read " + file, e);
		} catch (ParserConfigurationException e) {
			throw new PageSettingsException("Could not read " + file, e);

		} catch (SAXException e) {
			throw new PageSettingsException("Could not read " + file, e);
		}
		return documentDefinition;
	}

	public static RulePattern parseRule(String rule) {
		RulePattern rulePattern = new RulePattern();

		if (rule == null) return rulePattern;
		
		if (rule.contains("${"))
		{
			rulePattern.setRuleScript(rule);
			
		}
		else
		{
			

			int open = 0;
			int close = 0;
			StringBuffer expressionInParenthesis = new StringBuffer();

			rule = StringUtils.replace(rule, "(", " ( ");
			rule = StringUtils.replace(rule, ")", " ) ");

			StringTokenizer itemTokenizer = new StringTokenizer(rule, " ");
			while (itemTokenizer.hasMoreTokens()) {
				String ruleToken = itemTokenizer.nextToken();
				if (StringUtils.isBlank(ruleToken))
					continue;
				switch (ruleToken.toUpperCase()) {
				case "NOT":
					if (open == 0)
						rulePattern.addPattern(new NOTPattern());
					else {
						expressionInParenthesis.append(" NOT ");
					}
					break;
				case "AND":
					if (open == 0)
						rulePattern.addPattern(new ANDPattern());
					else {
						expressionInParenthesis.append(" AND ");
					}
					break;

				case "OR":
					if (open == 0)
						rulePattern.addPattern(new ORPattern());
					else {
						expressionInParenthesis.append(" OR ");
					}
					break;

				case "(":
					if (open > 0) {
						expressionInParenthesis.append(" ( ");
					}
					open++;
					break;

				case ")":
					if (close > 0) {
						expressionInParenthesis.append(" ) ");
					}
					close++;

					if (close == open) {// expression end
						RulePattern subRulePattern = parseRule(expressionInParenthesis.toString());
						rulePattern.addPattern(subRulePattern);
						expressionInParenthesis = new StringBuffer();
						open = 0;
						close = 0;
					}

					break;

				default:
					if (open == 0)
						rulePattern.addPattern(new MatchPatternLink(ruleToken));
					else {
						expressionInParenthesis.append(" ");
						expressionInParenthesis.append(ruleToken);
						expressionInParenthesis.append(" ");
					}

				}

			}

			
		}
		
		
		
		return rulePattern;
	}

	protected static FieldOptions getFieldOptions(String options) {
		FieldOptions fieldOptions = new FieldOptions();
		switch (options) {
		case "3091":
			parseBitsOptions(fieldOptions, "11111");
			break;
		case "1043":
			parseBitsOptions(fieldOptions, "11110");
			break;
		case "3089":
			parseBitsOptions(fieldOptions, "11101");
			break;
		case "1041":
			parseBitsOptions(fieldOptions, "11100");
			break;
		case "2067":
			parseBitsOptions(fieldOptions, "11011");
			break;
		case "19":
			parseBitsOptions(fieldOptions, "11010");
			break;
		case "2065":
			parseBitsOptions(fieldOptions, "11001");
			break;
		case "17":
			parseBitsOptions(fieldOptions, "11000");
			break;
		case "3090":
			parseBitsOptions(fieldOptions, "10111");
			break;
		case "1040":
			parseBitsOptions(fieldOptions, "10100");
			break;
		case "1042":
			parseBitsOptions(fieldOptions, "10110");
			break;
		case "2066":
			parseBitsOptions(fieldOptions, "10011");
			break;
		case "18":
			parseBitsOptions(fieldOptions, "10010");
			break;
		case "2064":
			parseBitsOptions(fieldOptions, "10001");
			break;
		case "3095":
			parseBitsOptions(fieldOptions, "01111");
			break;
		case "1047":
			parseBitsOptions(fieldOptions, "01110");
			break;
		case "3093":
			parseBitsOptions(fieldOptions, "01101");
			break;
		case "1045":
			parseBitsOptions(fieldOptions, "01100");
			break;
		case "2071":
			parseBitsOptions(fieldOptions, "01011");
			break;
		case "23":
			parseBitsOptions(fieldOptions, "01010");
			break;
		case "2069":
			parseBitsOptions(fieldOptions, "01001");
			break;
		case "21":
			parseBitsOptions(fieldOptions, "01000");
			break;
		case "3094":
			parseBitsOptions(fieldOptions, "00111");
			break;
		case "1046":
			parseBitsOptions(fieldOptions, "00110");
			break;
		case "3092":
			parseBitsOptions(fieldOptions, "00101");
			break;
		case "1044":
			parseBitsOptions(fieldOptions, "00100");
			break;
		case "2070":
			parseBitsOptions(fieldOptions, "00011");
			break;
		case "22":
			parseBitsOptions(fieldOptions, "00010");
			break;
		case "2068":
			parseBitsOptions(fieldOptions, "00001");
			break;
		case "20":
			parseBitsOptions(fieldOptions, "00000");
			break;

		default:
			parseBitsOptions(fieldOptions, "10000");
			break;
		}

		return fieldOptions;
	}

	protected static void parseBitsOptions(FieldOptions fieldOptions, String strOptions) {

		fieldOptions.keepSpace = strOptions.charAt(0) == '1';
		fieldOptions.removeText = strOptions.charAt(1) == '1';
		fieldOptions.removeImage = strOptions.charAt(2) == '1';
		fieldOptions.removeGraphic = strOptions.charAt(3) == '1';
		fieldOptions.removeBarcode = strOptions.charAt(4) == '1';

	}

	protected static PointMm parsePointPt(String point) {

		if (StringUtils.isBlank(point))
			return PointMm.ZERO;

		StringTokenizer itemTokenizer = new StringTokenizer(point, ",");
		if (itemTokenizer.countTokens() != 2)
			return PointMm.ZERO;

		float xpt = Float.parseFloat(itemTokenizer.nextToken());
		float ypt = Float.parseFloat(itemTokenizer.nextToken());

		float xmm = PointMm.getMm(xpt);
		float ymm = PointMm.getMm(ypt);

		PointMm pointMm = new PointMm();
		pointMm.setX(xmm);
		pointMm.setY(ymm);

		return pointMm;

	}

}

class FieldOptions {
	protected boolean keepSpace = false;
	protected boolean removeText = false;
	protected boolean removeImage = false;
	protected boolean removeGraphic = false;
	protected boolean removeBarcode = false;

	protected FieldOptions() {
	}
}