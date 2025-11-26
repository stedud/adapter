package fr.kw.adapter.parser.type.page.settings;

import java.util.ArrayList;
import java.util.List;

import fr.kw.adapter.parser.type.page.settings.field.FieldDefinition;
import fr.kw.adapter.parser.type.page.settings.field.PageType;
import fr.kw.adapter.parser.type.page.settings.pattern.RulePattern;

public class PageDocumentDefinition {

	protected PageType pageType = PageType.ANY;
	protected RulePattern matchRule = new RulePattern();
	protected List<FieldDefinition> fields = new ArrayList<FieldDefinition>();
	protected int xOffset = 0;
	protected int yOffset = 0;
	private boolean boxesFirst;

	public PageDocumentDefinition() {
		// TODO Auto-generated constructor stub
	}

	public RulePattern getMatchRule() {
		return matchRule;
	}

	public List<FieldDefinition> getFields() {
		return fields;
	}

	public void setMatchRule(RulePattern matchRule) {
		this.matchRule = matchRule;
	}

	public int getxOffset() {
		return xOffset;
	}

	public void setxOffset(int xOffset) {
		this.xOffset = xOffset;
	}

	public int getyOffset() {
		return yOffset;
	}

	public void setyOffset(int yOffset) {
		this.yOffset = yOffset;
	}

	public PageType getPageType() {
		return pageType;
	}

	public void setPageType(PageType pageType) {
		this.pageType = pageType;
	}

	public boolean isBoxesFirst() {
		return this.boxesFirst;

	}

	public void setBoxesFirst(boolean boxesFirst) {
		this.boxesFirst = boxesFirst;
	}

}
