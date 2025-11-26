package fr.kw.adapter.parser.type.page.settings;

import java.util.ArrayList;
import java.util.List;

import fr.kw.adapter.parser.process.ParseProcessConfiguration;
import fr.kw.adapter.parser.type.page.settings.field.FieldDefinition;

public class DocumentParserDefinition {

	protected String name;
	protected ParseProcessConfiguration rootConfiguration;
	protected List<PageDocumentDefinition> pageDefinitions = new ArrayList<PageDocumentDefinition>();

	public DocumentParserDefinition(ParseProcessConfiguration rootConfig) {
		this.rootConfiguration = rootConfig;

	}

	public List<PageDocumentDefinition> getPageDefinitions() {
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
