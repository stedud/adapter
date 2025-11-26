package fr.kw.adapter.parser.script;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class ScriptEngineHelperStatic {

	protected static Map<String, ScriptEngine> scriptEngines = new HashMap<String, ScriptEngine>();
	private static ScriptEngineManager manager = null;

	public static void main(String[] args) {
		ScriptEngineManager mgr = new ScriptEngineManager();
		List<ScriptEngineFactory> factories = mgr.getEngineFactories();
		for (ScriptEngineFactory factory : factories) {
			System.out.println("ScriptEngineFactory Info");
			String engName = factory.getEngineName();
			String engVersion = factory.getEngineVersion();
			String langName = factory.getLanguageName();
			String langVersion = factory.getLanguageVersion();
			System.out.printf("\tScript Engine: %s (%s)\n", engName, engVersion);
			List<String> engNames = factory.getNames();
			for (String name : engNames) {
				System.out.printf("\tEngine Alias: %s\n", name);
			}
			System.out.printf("\tLanguage: %s (%s)\n", langName, langVersion);
		}
	}
	
	
	public static ScriptEngine getScriptEngine(String extensionOrName, boolean forceNew) {
		
		if (forceNew)
		{
			ScriptEngineManager engineManager = new ScriptEngineManager();

			ScriptEngine moteur = engineManager.getEngineByExtension(extensionOrName);
			if (moteur == null)
				moteur = engineManager.getEngineByName(extensionOrName);

			Bindings bindings = moteur.getBindings(ScriptContext.ENGINE_SCOPE);
			bindings.put("polyglot.js.allowAllAccess", true);
			return moteur;
		}
		else if (!scriptEngines.containsKey(extensionOrName)) {
			if (manager == null)
				manager = new ScriptEngineManager();

			ScriptEngine moteur = manager.getEngineByExtension(extensionOrName);
			if (moteur == null)
				moteur = manager.getEngineByName(extensionOrName);

			Bindings bindings = moteur.getBindings(ScriptContext.ENGINE_SCOPE);
			bindings.put("polyglot.js.allowAllAccess", true);
			scriptEngines.put(extensionOrName, moteur);
		}
		return scriptEngines.get(extensionOrName);

	}

	public static ScriptEngine getScriptEngine(String extensionOrName) {
		if (!scriptEngines.containsKey(extensionOrName)) {
			if (manager == null)
				manager = new ScriptEngineManager();

			ScriptEngine moteur = manager.getEngineByExtension(extensionOrName);
			if (moteur == null)
				moteur = manager.getEngineByName(extensionOrName);

			Bindings bindings = moteur.getBindings(ScriptContext.ENGINE_SCOPE);
			bindings.put("polyglot.js.allowAllAccess", true);
			scriptEngines.put(extensionOrName, moteur);
		}
		return scriptEngines.get(extensionOrName);

	}
	
	public static  Object eval(String script, ScriptEngine engine) throws ScriptException
	{
		
		Object result = null;
		if (scriptEngines.containsValue(engine))
		{
			synchronized (engine) {
				result = engine.eval(script);	
			}
		}
		else
		{
			result = engine.eval(script);
		}
		return result;
	}

}
