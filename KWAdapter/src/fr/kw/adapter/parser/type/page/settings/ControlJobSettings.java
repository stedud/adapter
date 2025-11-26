package fr.kw.adapter.parser.type.page.settings;

import java.util.HashMap;
import java.util.Map;

public class ControlJobSettings {

	// key : document type, value : control job template (freemarker)
	protected Map<String, String> controlJobTemplates = new HashMap<String, String>();

	public ControlJobSettings() {
		// TODO Auto-generated constructor stub
	}

	public Map<String, String> getControlJobTemplates() {
		return controlJobTemplates;
	}

}
