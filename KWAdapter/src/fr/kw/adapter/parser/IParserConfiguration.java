package fr.kw.adapter.parser;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import fr.kw.adapter.parser.process.ParseProcessConfiguration;

public interface IParserConfiguration {

	void load(File descriptionFile, ParseProcessConfiguration rootConfig) throws IOException;

	String getId();

	void setId(String id);

	public Map<String, String> getMetadata();

	public ParseProcessConfiguration getRootConfiguration();
}