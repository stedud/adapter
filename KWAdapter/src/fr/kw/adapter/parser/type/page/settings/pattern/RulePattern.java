package fr.kw.adapter.parser.type.page.settings.pattern;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;

import fr.freemarker.FreeMarkerHelper;
import fr.kw.adapter.parser.script.ScriptEngineHelperStatic;
import fr.kw.adapter.parser.type.page.FileProcessException;
import fr.kw.adapter.parser.type.page.document.Page;
import fr.kw.adapter.parser.type.page.pagein.data.TextBlock;
import fr.kw.adapter.parser.type.page.settings.PageSettingsException;

public class RulePattern extends APatternDefinition {
	
	protected String ruleScript = null;

	protected Map<String, MatchPattern> matchPatterns = new HashMap<String, MatchPattern>();

	protected List<APatternDefinition> patterns = new ArrayList<APatternDefinition>();

	public RulePattern() {

	}

	
	public boolean match(TextBlock textBlock,  Map<String,Object> pageContext, int yoffset, ScriptEngine scriptEngine) throws FileProcessException, IOException, PageSettingsException {

		if (this.patterns.size() == 0 && this.matchPatterns.size() > 0 && StringUtils.isBlank(ruleScript)) {
			for (MatchPattern pattern : this.matchPatterns.values()) {
				if (this.patterns.size() != 0)
					this.addPattern(new ANDPattern());
				this.addPattern(pattern);
			}
		}

		return matchRule(textBlock, yoffset, this, matchPatterns, new HashMap<String, Boolean>(), pageContext, scriptEngine);
	}
	
	
	public boolean match(Page docPage,ScriptEngine scriptEngine) throws FileProcessException, IOException, PageSettingsException {

		if (this.patterns.size() == 0 && this.matchPatterns.size() > 0) {
			for (MatchPattern pattern : this.matchPatterns.values()) {
				if (this.patterns.size() != 0)
					this.addPattern(new ANDPattern());
				this.addPattern(pattern);
			}
		}

		return matchRule(docPage, this, matchPatterns, new HashMap<String, Boolean>(),scriptEngine);
	}

	protected static boolean matchRule(Page docPage, RulePattern rule, Map<String, MatchPattern> matchPatterns,
			Map<String, Boolean> patternValues, ScriptEngine scriptEngine) throws FileProcessException, IOException, PageSettingsException {

		boolean currentMatch = true;

		
		if (rule.ruleScript == null)
		{
			List<APatternDefinition> patterns = rule.patterns;
	
			APatternDefinition operator = null;
			APatternDefinition not = null;
	
			for (APatternDefinition pattern : patterns) {
	
				if (pattern instanceof MatchPattern || pattern instanceof RulePattern
						|| pattern instanceof MatchPatternLink) {
					if (pattern instanceof MatchPatternLink) {
						if (!matchPatterns.containsKey(((MatchPatternLink) pattern).getName()))
							throw new PageSettingsException(
									"Pattern '" + ((MatchPatternLink) pattern).getName() + "' does not exist");
	
						pattern = matchPatterns.get(((MatchPatternLink) pattern).getName());
	
					}
	
					boolean isMatching = true;
					if (pattern instanceof MatchPattern) {
						MatchPattern matchPattern = (MatchPattern) pattern;
						if (patternValues.containsKey(matchPattern.getName())) {
							isMatching = patternValues.get(matchPattern.getName());
						} else {
							isMatching = matchPattern.match(docPage);
							patternValues.put(matchPattern.getName(), isMatching);
						}
					} else {
						isMatching = matchRule(docPage, (RulePattern) pattern, matchPatterns, patternValues, scriptEngine);
					}
	
					if (not != null) {
						isMatching = !isMatching;
						not = null;
					}
	
					if (operator == null) {
						currentMatch = isMatching;
					}
	
					if (operator != null) {
						if (operator instanceof ANDPattern) {
							currentMatch = currentMatch && isMatching;
						} else if (operator instanceof ORPattern) {
							currentMatch = currentMatch || isMatching;
						}
						operator = null;
					}
	
				} else if (pattern instanceof ANDPattern || pattern instanceof ORPattern) {
					operator = pattern;
				} else if (pattern instanceof NOTPattern) {
					not = pattern;
				}
	
			}
		}
		else
		{//javascript rule
			Map<String, Object> ruleContext = new HashMap<String, Object>();
			

			for (MatchPattern matchPattern : matchPatterns.values())
			{
				boolean isMatching = matchPattern.match(docPage);
				ruleContext.put(matchPattern.getName(), String.valueOf(isMatching));
				scriptEngine.put(matchPattern.getName(), Boolean.valueOf(isMatching));
			}
			
			String script = FreeMarkerHelper.parseExpression(rule.ruleScript,rule.ruleScript, ruleContext);
	
			try {
				
				
				Object result = ScriptEngineHelperStatic.eval(script, scriptEngine);
				currentMatch = Boolean.parseBoolean(result.toString());	
				
				
				
			} catch (ScriptException e) {
				throw new PageSettingsException("Error while parsing rule '" + rule.ruleScript + "' (" + script + ") : " + e.getMessage());
			}
			
			
		}

		return currentMatch;
	}

	protected static boolean matchRule(TextBlock textBlock, int yoffset, RulePattern rule, Map<String, MatchPattern> matchPatterns,
			Map<String, Boolean> patternValues,  Map<String,Object> pageContext, ScriptEngine scriptEngine) throws FileProcessException, IOException, PageSettingsException {

		boolean currentMatch = true;

		if (rule.ruleScript == null)
		{
		List<APatternDefinition> patterns = rule.patterns;

		APatternDefinition operator = null;
		APatternDefinition not = null;

		for (APatternDefinition pattern : patterns) {

			if (pattern instanceof MatchPattern || pattern instanceof RulePattern
					|| pattern instanceof MatchPatternLink) {
				if (pattern instanceof MatchPatternLink) {
					if (!matchPatterns.containsKey(((MatchPatternLink) pattern).getName()))
						throw new PageSettingsException(
								"Pattern '" + ((MatchPatternLink) pattern).getName() + "' does not exist");

					pattern = matchPatterns.get(((MatchPatternLink) pattern).getName());

				}

				boolean isMatching = true;
				if (pattern instanceof MatchPattern) {
					MatchPattern matchPattern = (MatchPattern) pattern;
					if (patternValues.containsKey(matchPattern.getName())) {
						isMatching = patternValues.get(matchPattern.getName());
					} else {
						isMatching = matchPattern.match(textBlock, matchPattern.isIgnoreLine(), matchPattern.isIgnoreColumn(), yoffset);
						patternValues.put(matchPattern.getName(), isMatching);
					}
				} else {
					isMatching = matchRule(textBlock, yoffset, (RulePattern) pattern, matchPatterns, patternValues, pageContext, scriptEngine);
				}

				if (not != null) {
					isMatching = !isMatching;
					not = null;
				}

				if (operator == null) {
					currentMatch = isMatching;
				}

				if (operator != null) {
					if (operator instanceof ANDPattern) {
						currentMatch = currentMatch && isMatching;
					} else if (operator instanceof ORPattern) {
						currentMatch = currentMatch || isMatching;
					}
					operator = null;
				}

			} else if (pattern instanceof ANDPattern || pattern instanceof ORPattern) {
				operator = pattern;
			} else if (pattern instanceof NOTPattern) {
				not = pattern;
			}

		}
		}
		else
		{//javascript rule
			Map<String, Object> ruleContext = new HashMap<String, Object>();
			ruleContext.putAll(pageContext);
			
			
			for (String key : pageContext.keySet() )
			{
				scriptEngine.put(key, pageContext.get(key));
			}
			for (MatchPattern matchPattern : matchPatterns.values())
			{
				boolean isMatching = matchPattern.match(textBlock,matchPattern.isIgnoreLine(), matchPattern.isIgnoreColumn(), yoffset);
				ruleContext.put(matchPattern.getName(), String.valueOf(isMatching));
				scriptEngine.put(matchPattern.getName(), Boolean.valueOf(isMatching));
			}
			
			
			String script = FreeMarkerHelper.parseExpression(rule.ruleScript,rule.ruleScript, ruleContext);
			
			try {
				
				Object result = ScriptEngineHelperStatic.eval(script, scriptEngine);
				currentMatch = Boolean.parseBoolean(result.toString());					


			} catch (ScriptException e) {
				throw new PageSettingsException("Error while parsing rule '" + rule.ruleScript + "' (" + script + ") : " + e.getMessage());
			}
			
			
		}

		return currentMatch;
	}

	
	public void declareMatchPattern(MatchPattern pattern) {
		this.matchPatterns.put(pattern.getName(), pattern);
	}

	public void addPattern(APatternDefinition pattern) {
		patterns.add(pattern);
		if (pattern instanceof MatchPattern)
			declareMatchPattern((MatchPattern) pattern);

	}

	public Map<String, MatchPattern> getMatchPatterns() {
		return matchPatterns;
	}

	public List<APatternDefinition> getPatterns() {
		return patterns;
	}

	@Override
	public String toString() {
		return "RulePattern [matchPatterns=" + matchPatterns + ", patterns=" + patterns + "]";
	}


	public String getRuleScript() {
		return ruleScript;
	}


	public void setRuleScript(String ruleScript) {
		this.ruleScript = ruleScript;
	}

}
