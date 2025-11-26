package fr.kw.adapter.parser.type.recordin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import fr.kw.adapter.parser.script.Script;
import fr.kw.adapter.parser.script.ScriptEngineHelperStatic;

public class RecordDefinition {

	protected RecordParserConfiguration parent;
	protected boolean delimited = true;
	protected List<String> events = new ArrayList<String>();
	protected RecordType eventType = RecordType.IN_EVENT;
	protected List<FieldRecordDefinition> fieldsDefinitions = new ArrayList<FieldRecordDefinition>();
	protected String id = "";
	protected Pattern pattern = Pattern.compile(".*");
	protected Script script = null;

	protected String separator = ";";
	protected Map<String, Object> originalContextValues = new HashMap<String, Object>();

	public RecordDefinition(RecordParserConfiguration parent) {
		this.parent = parent;

	}

	public boolean match(String line, Map<String, Object> context, ScriptEngine scriptEngine) {
		if (script != null && script.getScript() != null) {

			

			if (context != null)
				pushContext(scriptEngine, context);

			try {
				Object result = scriptEngine.eval(script.getScript());
				if (context != null)
					removeContext(scriptEngine, context);

				return Boolean.parseBoolean(result == null ? "false" : result.toString());
			} catch (ScriptException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}

		}

		return pattern.matcher(line).matches();
	}

	protected void pushContext(ScriptEngine engine, Map<String, Object> context) {
		originalContextValues.clear();
		for (Entry<String, Object> entry : context.entrySet()) {
			if (engine.get(entry.getKey()) != null) {// sauvegarde valeur d'origine
				originalContextValues.put(entry.getKey(), engine.get(entry.getKey()));
			}
			engine.put(entry.getKey(), entry.getValue());
		}
	}

	protected void removeContext(ScriptEngine engine, Map<String, Object> context) {
		originalContextValues.clear();
		for (Entry<String, Object> entry : context.entrySet()) {
			if (originalContextValues.get(entry.getKey()) != null) {// remplacer la valeur par celle d'origine
				engine.put(entry.getKey(), originalContextValues.get(entry.getKey()));
			} else {// supprimer la valeur
				engine.getBindings(ScriptContext.ENGINE_SCOPE).remove(entry.getKey());
			}
		}
	}

}
