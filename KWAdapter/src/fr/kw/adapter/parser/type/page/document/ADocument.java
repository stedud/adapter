package fr.kw.adapter.parser.type.page.document;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;

import bsh.EvalError;
import bsh.Interpreter;
import fr.freemarker.FreeMarkerException;
import fr.freemarker.FreeMarkerHelper;
import fr.kw.adapter.document.Document;
import fr.kw.adapter.parser.type.page.FileProcessException;
import fr.kw.adapter.parser.type.page.settings.DocumentParserDefinition;
import fr.utils.LogHelper;

public abstract class ADocument {

	protected DocumentParserDefinition definition;
	protected List<Page> pages = new ArrayList<Page>();
	protected Map<String, Field> fields = new HashMap<String, Field>();
	protected Map<String, Document> addtionalContext = new HashMap<String, Document>(1);
	protected File fileResult = null;
	protected ScriptEngine engineJavascript;
	protected Interpreter javaInterpreter;
	protected ScriptEngineManager manager;
	protected Object document;
	protected int position;

	protected Exception error = null;

	public ADocument() {
		super();
	}

	public void addPage(Page page) throws IOException {
		this.pages.add(page);
//		if (page.getPageDocumentDefinition().isBoxesFirst())
//		{
//			if (page.getNativePage() instanceof fr.sap.otf.parser.object.Page)
//			{
//				((fr.sap.otf.parser.object.Page)page.getNativePage()).sortBoxes();
//			}
//		}
	}

	public Map<String, Field> getFields() {
		return fields;
	}

	public DocumentParserDefinition getDefinition() {
		return definition;
	}

	public File getFile() {
		return fileResult;
	}
	
	
	public static void runFieldScriptStatic(Field f, String script, Map<String, Object> context) {
		if (StringUtils.isBlank(script))
			return;

		
			ScriptEngineManager manager = new ScriptEngineManager();
			
		ScriptEngine engine = null;
		if (script.startsWith("//javascript")) {
			ScriptEngine engineJavascript = manager.getEngineByName("javascript");
			engine = engineJavascript;
			
			engine.put("_field", f);
			engine.put(f.getName(), f.getValue() == null ? "" : f.getValue());

			for (String key : context.keySet())
			{
				engine.put(key, context.get(key));
			}
			try {
				Object result = engine.eval(script);
				if (result != null) {

					f.setValue(String.valueOf(result));

				}

			} catch (ScriptException e) {
				LogHelper.error("Error while running script :\n" + script, e);
			}
		} else {
			
			Interpreter javaInterpreter = new Interpreter();
			

			try {

				
				javaInterpreter.set("_field", f);
				javaInterpreter.set(f.getName(), f.getValue() == null ? "" : f.getValue());
				for (String key : context.keySet())
				{
					javaInterpreter.set(key, context.get(key));
				}
				
				Object result = javaInterpreter.eval(script);
				if (result != null) {
					if (result instanceof String) {
						f.setValue((String) result);
					}
				}
				javaInterpreter.unset(f.getName());
				

			} catch (EvalError e) {
				LogHelper.error("Error while running script :\n" + script, e);
			}

		}

	}

	public void runFieldScript(Field f, String script) {
		if (StringUtils.isBlank(script))
			return;

		if (manager == null)
			manager = new ScriptEngineManager();
		ScriptEngine engine = null;
		if (script.startsWith("//javascript")) {
			if (engineJavascript == null)
				engineJavascript = manager.getEngineByName("javascript");
			engine = engineJavascript;
			engine.put("_document", this);
			engine.put("_field", f);
			engine.put(f.getName(), f.getValue() == null ? "" : f.getValue());

			for (Field field : this.fields.values()) {
				engine.put(field.getName(), field.getValue());
			}

			try {
				Object result = engine.eval(script);
				if (result != null) {

					f.setValue(String.valueOf(result));

				}

			} catch (ScriptException e) {
				LogHelper.error("Error while running script :\n" + script, e);
			}
		} else {
			if (javaInterpreter == null) {
				javaInterpreter = new Interpreter();
			}

			try {

				javaInterpreter.set("_document", this);
				javaInterpreter.set("_field", f);
				javaInterpreter.set(f.getName(), f.getValue() == null ? "" : f.getValue());
				for (Field field : this.fields.values()) {
					javaInterpreter.set(field.getName(), field.getValue());
				}
				Object result = javaInterpreter.eval(script);
				if (result != null) {
					if (result instanceof String) {
						f.setValue((String) result);
					}
				}
				javaInterpreter.unset(f.getName());
				for (Field field : this.fields.values()) {
					javaInterpreter.unset(field.getName());
				}

			} catch (EvalError e) {
				LogHelper.error("Error while running script :\n" + script, e);
			}

		}

	}

	public Object getDoc() {
		return document;
	}

	public abstract void createFile(File target) throws IOException, FileProcessException;
	// TODO Auto-generated method stu

	public abstract void init(Object originalDocument);

	public Exception getError() {
		return error;
	}

	public void setError(Exception error) {
		this.error = error;
	}

	public Object getDocument() {
		return document;
	}

	public List<Page> getPages() {
		return pages;
	}

	public String getFieldsAsString() {
		StringBuffer sb = new StringBuffer();
		for (Field f : this.fields.values()) {
			sb.append(f.getName());
			sb.append("=");
			sb.append(f.getValue());
			sb.append("\n");
		}
		return sb.toString();
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public Map<String, Document> getAddtionalContext() {
		return addtionalContext;
	}
	
	public Map<String, Object> getContext()
	{
		Map<String, Object> context = new HashMap<String, Object>();
		for (Field f : this.fields.values())
		{
			context.put(f.getName(), f.getValue());
		}
		if (addtionalContext != null)
		{
			for (Document doc : getAddtionalContext().values()) {
				try {
					Map<String, Object> docContext = FreeMarkerHelper.getContextFromDocument(doc);
					context.put(doc.getName(), docContext);
				} catch (FreeMarkerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
		return context;
	}
	
	public abstract void close();

}