package fr.kw.adapter.parser.type.page.pagein;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.script.ScriptEngine;

import org.apache.commons.lang3.StringUtils;

import de.kwsoft.mtext.mffmfd.DocumentGeneratorException;
import fr.kw.adapter.document.DataSourceException;
import fr.kw.adapter.parser.DataDescriptionException;
import fr.kw.adapter.parser.event.Event;
import fr.kw.adapter.parser.event.Record;
import fr.kw.adapter.parser.event.structure.EventStructure;
import fr.kw.adapter.parser.process.ParseProcessConfiguration;
import fr.kw.adapter.parser.script.ScriptEngineHelperStatic;
import fr.kw.adapter.parser.type.page.FileProcessException;
import fr.kw.adapter.parser.type.page.document.Page;
import fr.kw.adapter.parser.type.page.pagein.data.TextBlock;
import fr.kw.adapter.parser.type.page.pagein.settings.FrameBlockDefinition;
import fr.kw.adapter.parser.type.page.pagein.settings.FrameDefinition;
import fr.kw.adapter.parser.type.page.pagein.settings.PageDefinition;
import fr.kw.adapter.parser.type.page.pagein.tools.PageInUtils;
import fr.kw.adapter.parser.type.page.settings.DocumentParserDefinition;
import fr.kw.adapter.parser.type.page.settings.PageDocumentDefinition;
import fr.kw.adapter.parser.type.page.settings.PageSettingsException;
import fr.kw.adapter.parser.type.page.settings.field.FieldDefinition;
import fr.kw.adapter.parser.type.page.settings.field.PageType;
import fr.kw.adapter.parser.type.page.settings.pattern.RulePattern;
import fr.utils.LogHelper;
import fr.utils.Utils;

public class PageInProcess {
	protected Map<String, DocumentParserDefinition> documentDefinitions = new HashMap<String, DocumentParserDefinition>();
	protected ParseProcessConfiguration configuration;

	public PageInProcess(ParseProcessConfiguration configuration) {
		this.configuration = configuration;

	}

	public void addDocumentDefinition(DocumentParserDefinition docDef) {
		this.documentDefinitions.put(docDef.getName(), docDef);
	}

	public void addDocumentDefinitions(List<DocumentParserDefinition> docDefs) {
		for (DocumentParserDefinition docDef : docDefs) {
			this.addDocumentDefinition(docDef);
		}
	}

	public List<Event> parse(File pagein) throws IOException, FileProcessException, PageSettingsException,
			DocumentGeneratorException, DataSourceException {
		ScriptEngine scriptEngine = ScriptEngineHelperStatic.getScriptEngine("javascript", true);
		List<Event> result = new ArrayList<Event>();
		Map<String, EventStructure> eventStructures = new HashMap<String, EventStructure>();

		Event currentPageInEvent = null;
		PageDefinition currentPageDef = null;
		Scanner pageIterator = null;
		int pageNum = 0;
		try {
		List<TextBlock> pages = new ArrayList<TextBlock>();
		pageIterator = Utils.getPageScanner(pagein, StandardCharsets.UTF_8.name());
		while (pageIterator.hasNext())
		{
			String pageStrs = pageIterator.next();
			String[] pageLines = pageStrs.split(Utils.LFRegHex);
			TextBlock pageTextBlock = new TextBlock();
			for (String line : pageLines)pageTextBlock.addLine(StringUtils.removeStart(line, "\uFEFF"));
			TextBlock[] textPages = PageInUtils.getPages(pagein);
		

			pageNum++;
			Page page = new Page(pageTextBlock);

			boolean newDocTriggered = false;

			Map<String, fr.kw.adapter.parser.event.Field> pageFieldsContext = new HashMap<String, fr.kw.adapter.parser.event.Field>();

			for (DocumentParserDefinition docParserDefinition : documentDefinitions.values()) {

				for (PageDocumentDefinition pageDefinition : docParserDefinition.getPageDefinitions()) {
					if (!(pageDefinition instanceof PageDefinition))
						continue;

					PageDefinition pageInDefinition = (PageDefinition) pageDefinition;

					Map<String, Object> matchContext = new HashMap<String, Object>();
					if (currentPageInEvent != null) {

						for (fr.kw.adapter.parser.event.Field f : currentPageInEvent.getFields()) {
							matchContext.put(f.getName(), f.getValue());
						}

					}

					try {
						pageFieldsContext = this.getRootPageFields(page, pageInDefinition);
						Map<String, Object> pageContext = new HashMap<String, Object>();
						for (fr.kw.adapter.parser.event.Field f : pageFieldsContext.values()) {
							pageContext.put(f.getName(), f.getValue());
						}
						matchContext.put("page", pageContext);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					page.setPageDocumentDefinition(pageInDefinition);
					RulePattern docPatternRule = pageInDefinition.getMatchRule();

					boolean docPatternRuleMatchResult = docPatternRule == null ? true
							: docPatternRule.match((TextBlock) page.getNativePage(), matchContext, 0,scriptEngine);

					boolean newDoc = docPatternRule == null ? true
							: docPatternRuleMatchResult && (pageInDefinition.getPageType() == PageType.FIRST
									|| pageInDefinition.getPageType() == PageType.ANY);
					boolean inDoc = currentPageInEvent == null ? false
							: (docPatternRule == null || docPatternRuleMatchResult)
									&& (pageInDefinition.getPageType() == PageType.NEXT
											|| pageInDefinition.getPageType() == PageType.LAST
											|| pageInDefinition.getPageType() == PageType.ANY);
					inDoc = inDoc && (currentPageInEvent != null && currentPageInEvent.getName()
							.compareTo(docParserDefinition.getName()) == 0);

					boolean lastDoc = inDoc && pageInDefinition.getPageType() == PageType.LAST;

					if (newDoc) {
						currentPageDef = pageInDefinition;

						newDocTriggered = true;
						LogHelper.info("New doc '" + docParserDefinition.getName() + "' triggered at page " + pageNum);

					} else if (inDoc && currentPageInEvent != null && currentPageInEvent.getName()
							.compareTo(docParserDefinition.getName()) == 0) {
						
						LogHelper.info("Page " + pageNum + " triggered in doc '" + docParserDefinition.getName());

					}

					if (!newDoc && !inDoc) {// passage au parser suivant
											// TODO : cloturer event en cours si besoin
						continue;
					}
					page.setPageDocumentDefinition(currentPageDef);
					if (newDocTriggered) {

						if (currentPageInEvent != null) {
							// cloture de l'event en cours si besoin

						}
						// Creation nouvel event
						// TODO

						try {
							currentPageInEvent = new Event(pageInDefinition.getEvent());
							
							EventStructure structure = null;
							if (eventStructures.containsKey(pageInDefinition.getEvent())) {
								structure = eventStructures.get(pageInDefinition.getEvent());
							} else {
								File structureFile = configuration.getEventStructure(pageInDefinition.getEvent());
								if (structureFile != null) {
									structure = EventStructure.load(structureFile);

								}
								
								eventStructures.put(pageInDefinition.getEvent(), structure);
								currentPageInEvent.setStructure(structure);
							}

						} catch (DataDescriptionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						result.add(currentPageInEvent);

					}
					if (inDoc || newDoc) {
						if (currentPageInEvent != null) {
							insertPageFields(page, pageInDefinition, currentPageInEvent, scriptEngine);

						}
						break;// La page est identifiée, on passe à la page suivante

					}
				}
			}

		}
		}catch (FileNotFoundException e)
		{
			throw new FileProcessException("Input file '" + pagein + "' was not found : " + e.getMessage(), e);
		}
		finally {
			
			if (pageIterator != null) pageIterator.close();
		}
		

		LogHelper.info("Processing " + pagein);
		File tmpFolder = Utils.getTmpFolder();
		tmpFolder.mkdirs();

		return result;

	}

	private Map<String, fr.kw.adapter.parser.event.Field> getRootPageFields(Page page,
			PageDefinition pageInDefinition) {
		Map<String, fr.kw.adapter.parser.event.Field> pageFields = new HashMap<String, fr.kw.adapter.parser.event.Field>();

		for (FieldDefinition fieldDefinition : pageInDefinition.getFields()) {
			TextBlock pageTextBlock = (TextBlock) page.getNativePage();
			TextBlock text = pageTextBlock.getTextAt(fieldDefinition.getPosition().getX(),
					fieldDefinition.getPosition().getY(), fieldDefinition.getDimension().getX(),
					fieldDefinition.getDimension().getY());
			fr.kw.adapter.parser.event.Field f = new fr.kw.adapter.parser.event.Field(fieldDefinition.getName(), fieldDefinition.isKeepSpace() ?
					text.toString() : text.toString().trim());
			pageFields.put(f.getName(), f);
		}

		return pageFields;
	}

	private void insertPageFields(Page page, PageDefinition pageInDefinition, Event event, ScriptEngine scriptEngine)
			throws FileProcessException, IOException, PageSettingsException {

		for (FieldDefinition fieldDefinition : pageInDefinition.getFields()) {
			TextBlock pageTextBlock = (TextBlock) page.getNativePage();
			TextBlock text = pageTextBlock.getTextAt(fieldDefinition.getPosition().getX(),
					fieldDefinition.getPosition().getY(), fieldDefinition.getDimension().getX(),
					fieldDefinition.getDimension().getY());
			fr.kw.adapter.parser.event.Field f = new fr.kw.adapter.parser.event.Field(fieldDefinition.getName(),
					fieldDefinition.isKeepSpace() ?
					text.toString() : text.toString().trim());
			event.insertField(f);
		}
		Map<String, Object> context = event != null ? event.getContext() : new HashMap<String, Object>();
		for (FrameDefinition frameDef : pageInDefinition.getFrames()) {
			
			TextBlock frameTextBlock = ((TextBlock) page.getNativePage()).getTextAt(frameDef.getPosition().getX(),
					frameDef.getPosition().getY(), frameDef.getDimension().getX(), frameDef.getDimension().getY());
			int currentPos = 1;// line number
			while (currentPos <= frameDef.getDimension().getY()) {
				boolean matched = false;
				

				for (FrameBlockDefinition blockDef : frameDef.getBlocks()) {

					boolean blockMatch = this.blockMatch(blockDef, frameDef, frameTextBlock, context, currentPos - 1,scriptEngine);
					if (blockMatch) {
						matched = true;
						Record r = new Record(blockDef.getName());
						TextBlock recordText = frameTextBlock.getTextAt(1, currentPos, frameTextBlock.getWidth(),
								blockDef.getH());
						// TODO:Lire les champs, créer le record, l'insérer dans l'event
						for (FieldDefinition fieldDef : blockDef.getFields()) {
							TextBlock fieldValue = recordText.getTextAt(fieldDef.getPosition().getX(),
									fieldDef.getPosition().getY(), fieldDef.getDimension().getX(),
									fieldDef.getDimension().getY());
							r.getFields().add(
									new fr.kw.adapter.parser.event.Field(fieldDef.getName(), fieldDef.isKeepSpace() ? fieldValue.toString() : fieldValue.toString().trim()));
						}
						event.insertRecord(r);
						currentPos = currentPos + blockDef.getH();
						continue;
					}

				}
				if (!matched) currentPos++;
			}

		}
	}

	private boolean blockMatch(FrameBlockDefinition blockDef, FrameDefinition frameDef, TextBlock frameContent,
			Map<String, Object> context, int position, ScriptEngine scriptEngine) throws FileProcessException, IOException, PageSettingsException {

		RulePattern rulePattern = blockDef.getRulePattern();

		return rulePattern.match(frameContent, context, position,scriptEngine);

	}

}
