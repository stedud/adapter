package fr.kw.adapter.parser;

import javax.script.ScriptEngine;

import fr.kw.adapter.parser.event.Event;

public interface IParser<f extends Object, t extends Object> {

	public Event parseEvent(Event currentEvent, t eventCandidate, ScriptEngine scriptEngine) throws DataDescriptionException;

	public f parseFields(Event currentEvent, f currentItem, t line, ScriptEngine scriptEngine) throws DataDescriptionException;

	public String getCharset();

	public void setCharset(String charsetName);

}