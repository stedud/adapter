package fr.kw.adapter.parser.type.page.pagein.settings;

import java.util.ArrayList;
import java.util.List;

import fr.kw.adapter.parser.type.page.settings.field.FieldDefinition;
import fr.kw.adapter.parser.type.page.settings.pattern.RulePattern;

public class FrameBlockDefinition {
	
	protected String name;
	protected int h = 1;
	protected RulePattern rulePattern = new RulePattern();
	
	protected List<FieldDefinition> fields = new ArrayList<FieldDefinition>();

	public FrameBlockDefinition(String name) {
		this.name = name;
	}

	public int getH() {
		return h;
	}

	public void setH(int h) {
		this.h = h;
	}

	public RulePattern getRulePattern() {
		return rulePattern;
	}

	public List<FieldDefinition> getFields() {
		return fields;
	}

	public void setRulePattern(RulePattern rulePattern) {
		this.rulePattern = rulePattern;
	}

	public String getName() {
		return name;
	}

	
}
