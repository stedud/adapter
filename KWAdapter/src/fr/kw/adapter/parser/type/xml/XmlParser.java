package fr.kw.adapter.parser.type.xml;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngine;

import org.w3c.dom.Node;

import fr.kw.adapter.parser.DataDescriptionException;
import fr.kw.adapter.parser.IParser;
import fr.kw.adapter.parser.event.Event;
import fr.kw.adapter.parser.event.Record;

public class XmlParser implements IParser<Record, Node> {

	protected Map<String, String> metadata = new HashMap<String, String>();
	protected XmlParserConfiguration config;

	protected String charset = StandardCharsets.UTF_8.name();

	public XmlParser(XmlParserConfiguration config) {
		this.config = config;
	}

	@Override
	public Event parseEvent(Event currentEvent, Node eventCandidate, ScriptEngine scriptEngine) throws DataDescriptionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Record parseFields(Event currentEvent, Record currentRecord, Node line, ScriptEngine scriptEngine) throws DataDescriptionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCharset() {
		// TODO Auto-generated method stub
		return charset;
	}

	@Override
	public void setCharset(String charsetName) {
		this.charset = charsetName;

	}

	public XmlParserConfiguration getConfig() {
		return config;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

}
