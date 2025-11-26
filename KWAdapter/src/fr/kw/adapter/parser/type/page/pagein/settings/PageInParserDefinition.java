package fr.kw.adapter.parser.type.page.pagein.settings;

import java.util.ArrayList;
import java.util.List;

import fr.kw.adapter.parser.process.ParseProcessConfiguration;
import fr.kw.adapter.parser.type.page.settings.PageDocumentDefinition;
import fr.kw.adapter.parser.type.page.settings.field.FieldDefinition;

public class PageInParserDefinition {
	protected String name;
	protected ParseProcessConfiguration rootConfiguration;
	protected List<PageDefinition> pageDefinitions = new ArrayList<PageDefinition>();


	public PageInParserDefinition(ParseProcessConfiguration rootConfig) {
		this.rootConfiguration = rootConfig;

	}


	public List<PageDefinition> getPageDefinitions() {
		return pageDefinitions;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<FieldDefinition> getFields() {
		List<FieldDefinition> fields = new ArrayList<FieldDefinition>();
		for (PageDocumentDefinition pageDef : pageDefinitions) {
			fields.addAll(pageDef.getFields());
		}
		return fields;
	}

	public ParseProcessConfiguration getRootConfiguration() {
		return rootConfiguration;
	}
}
