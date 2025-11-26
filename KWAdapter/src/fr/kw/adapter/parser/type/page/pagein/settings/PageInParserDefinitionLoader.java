package fr.kw.adapter.parser.type.page.pagein.settings;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringTokenizer;

import fr.kw.adapter.parser.process.ParseProcessConfiguration;
import fr.kw.adapter.parser.type.page.settings.DocumentParserDefinition;
import fr.kw.adapter.parser.type.page.settings.PageParserDefinitionLoader;
import fr.kw.adapter.parser.type.page.settings.PageSettingsException;
import fr.kw.adapter.parser.type.page.settings.field.FieldDefinition;
import fr.kw.adapter.parser.type.page.settings.field.PageType;
import fr.kw.adapter.parser.type.page.settings.pattern.ANDPattern;
import fr.kw.adapter.parser.type.page.settings.pattern.MatchPattern;
import fr.kw.adapter.parser.type.page.settings.pattern.RulePattern;
import fr.utils.Utils;

public class PageInParserDefinitionLoader extends PageParserDefinitionLoader {

	public PageInParserDefinitionLoader() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public DocumentParserDefinition load(File file, ParseProcessConfiguration rootConfig) throws PageSettingsException {
		LineIterator lineIterator = null;

		DocumentParserDefinition documentDefinition = new DocumentParserDefinition(rootConfig);
		documentDefinition.setName(FilenameUtils.getBaseName(file.getName()));

		try {
			
			lineIterator = FileUtils.lineIterator(file, StandardCharsets.UTF_8.name());
			
			List<MatchPattern> currentPatterns = new ArrayList<MatchPattern>();
			PageDefinition currentPageDefinition = null;
			FrameDefinition currentFrameDefinition = null;
			FrameBlockDefinition currentBlockDefinition = null;
			
			while (lineIterator.hasNext()) {
				String line = ((String) lineIterator.nextLine()).trim();
				if (line.toLowerCase().startsWith("event "))
				{
					currentPageDefinition = new PageDefinition();
					String event = Utils.parseSingleArgKeyWord(line, "Event");
					currentPageDefinition.setEvent(event);
					documentDefinition.getPageDefinitions().add(currentPageDefinition);
					currentBlockDefinition = null;
					currentFrameDefinition = null;
					
				}else if (line.toLowerCase().startsWith("type ") || line.toLowerCase().startsWith("type\t")) {
					String type = Utils.parseSingleArgKeyWord(line, "type");
					PageType pageType = PageType.FIRST;
					try {
						pageType = PageType.valueOf(type.toUpperCase());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						throw new PageSettingsException("Could not decode page type '" + type + "' : " + e.getMessage(), e);
					}
					if (currentPageDefinition != null) currentPageDefinition.setPageType(pageType);
				}
				else if (line.toLowerCase().startsWith("match \"")) {
					//Match "New_Pattern_1" 0 0 "           " UseWildCards
					line = line.replace('\t', ' ');
					StringTokenizer tokenizer = new StringTokenizer(line, ' ', '"');
					String[] tokens = tokenizer.getTokenArray();
					String patternName = tokens[1];
					String patternString = tokens[4];
					
					Float patternX = Float.parseFloat(tokens[2]);
					Float patternY = Float.parseFloat(tokens[3]);
					if (currentBlockDefinition != null)
						{
							patternX++;
							patternY++;
						}
					
					boolean useWildCards = line.toLowerCase().contains("usewildcards");
					
					MatchPattern matchPattern = new MatchPattern(patternName);
					matchPattern.setPattern(patternString);
					matchPattern.getPosition().setX(patternX);
					matchPattern.getPosition().setY(patternY);
					if (useWildCards)
						matchPattern.getDimension().setX(-1);
					else
						matchPattern.getDimension().setX(patternString.length());
					matchPattern.getDimension().setY(1f);
					matchPattern.setIgnoreColumn(patternX == 0.0);
					matchPattern.setIgnoreLine(patternY == 0.0);
					matchPattern.setWildcards(useWildCards);
					
					currentPatterns.add(matchPattern);
					
				}else if (line.toLowerCase().startsWith("rule \"")) {
					//Rule "New_Pattern_3 AND New_Pattern_4";
					line = StringUtils.removeEnd(line, ";");
					String rule = line.substring("rule \"".length(),line.length()-1);
					RulePattern rulePattern = PageParserDefinitionLoader.parseRule(rule);
					for (MatchPattern pattern : currentPatterns)
					{
						rulePattern.declareMatchPattern(pattern);
					}
					
					if (currentBlockDefinition != null) {
						currentBlockDefinition.setRulePattern(rulePattern);
					}else if (currentPageDefinition != null) {
						currentPageDefinition.setMatchRule(rulePattern);
					}
					currentPatterns.clear();
					
				}else if (line.toLowerCase().startsWith("field \"")) {
					//Field "New_Field_3" 53 1 62 1 $New_Field_3 Keepsp;
					if (currentPatterns.size() > 0)
					{//Rule line was not present -> default Rule
						RulePattern rulePattern = new RulePattern();
						for (MatchPattern pattern : currentPatterns)
						{
							
							rulePattern.declareMatchPattern(pattern);
							if (rulePattern.getPatterns().size() > 0)
								rulePattern.addPattern(new ANDPattern());
							rulePattern.addPattern(pattern);
						}
						
						if (currentBlockDefinition != null) {
							currentBlockDefinition.setRulePattern(rulePattern);
						}else if (currentPageDefinition != null) {
							currentPageDefinition.setMatchRule(rulePattern);
						}
						currentPatterns.clear();
					}	
						
						
					line = StringUtils.removeEnd(line, ";");
					StringTokenizer tokenizer = new StringTokenizer(line, ' ', '"');
					String[] tokens = tokenizer.getTokenArray();
					String fieldName = tokens[1];
					
					float x = Float.parseFloat(tokens[2]);
					float y = Float.parseFloat(tokens[3]);
					float xEnd = Float.parseFloat(tokens[4]);
					float yEnd = Float.parseFloat(tokens[5]);
					
					if (currentBlockDefinition != null)
					{
						x++;
						y++;
						xEnd++;
						yEnd++;
					}
					
					boolean keepSpace = line.toLowerCase().contains("keepsp");
					FieldDefinition fieldDefinition = new FieldDefinition(fieldName);
					fieldDefinition.getPosition().setX(x);
					fieldDefinition.getPosition().setY(y);
					fieldDefinition.getDimension().setX(xEnd- x + 1);
					fieldDefinition.getDimension().setY(yEnd - y + 1);
					fieldDefinition.setKeepSpace(keepSpace);
					
					if (currentBlockDefinition != null) currentBlockDefinition.getFields().add(fieldDefinition);
					else if (currentPageDefinition != null) currentPageDefinition.getFields().add(fieldDefinition);
					
				}
				else if (line.toLowerCase().startsWith("frame ")) {
					//Frame 10 20 74 50
					line = StringUtils.removeEnd(line, ";");
					StringTokenizer tokenizer = new StringTokenizer(line, ' ', '"');
					String[] tokens = tokenizer.getTokenArray();
					String x = tokens[1];
					String y = tokens[2];
					String xEnd = tokens[3];
					String yEnd = tokens[4];
					currentFrameDefinition = new FrameDefinition(line);
					currentFrameDefinition.getPosition().setX(Float.parseFloat(x));
					currentFrameDefinition.getPosition().setY(Float.parseFloat(y));
					currentFrameDefinition.getDimension().setX(Float.parseFloat(xEnd) - Float.parseFloat(x) + 1);
					currentFrameDefinition.getDimension().setY(Float.parseFloat(yEnd) - Float.parseFloat(y) + 1);
					if (currentPageDefinition != null) currentPageDefinition.addFrame(currentFrameDefinition);
					currentBlockDefinition = null;
					
				}else if (line.toLowerCase().startsWith("block \"")) {
					//Block "total"[UsePriority] Lines 5
					line = StringUtils.removeEnd(line, ";");
					StringTokenizer tokenizer = new StringTokenizer(line, ' ', '"');
					String[] tokens = tokenizer.getTokenArray();
					String name = tokens[1];
					String lines = tokens[tokens.length-1];
					currentBlockDefinition = new FrameBlockDefinition(name);
					currentBlockDefinition.setH(Integer.parseInt(lines));
					currentPatterns.clear();
					if (currentFrameDefinition != null) currentFrameDefinition.getBlocks().add(currentBlockDefinition);
					
				}else if (line.toLowerCase().startsWith("end \"")) {
					if (currentBlockDefinition != null)
					{
						currentBlockDefinition = null;
					} else if (currentFrameDefinition != null)
					{
						currentFrameDefinition = null;
					}
					else if (currentPageDefinition != null)
					{
						currentPageDefinition = null;
					}
					
				}
				
				
			}
			lineIterator.close();
			
		} catch (IOException e) {
			throw new PageSettingsException("Could not read " + file, e);
		}
		finally {
			
			if (lineIterator != null)
				try {
					lineIterator.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		return documentDefinition;
	}
	
	

}
