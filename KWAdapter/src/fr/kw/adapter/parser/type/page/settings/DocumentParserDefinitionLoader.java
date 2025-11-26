package fr.kw.adapter.parser.type.page.settings;

import java.io.File;

import fr.kw.adapter.parser.process.ParseProcessConfiguration;

public interface DocumentParserDefinitionLoader {

	public DocumentParserDefinition load(File file, ParseProcessConfiguration rootConfig) throws PageSettingsException;

}
