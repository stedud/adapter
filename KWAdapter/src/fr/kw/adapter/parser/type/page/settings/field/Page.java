package fr.kw.adapter.parser.type.page.settings.field;

public class Page {

	protected PageType pageType;
	protected int exactPage = 1;

	public Page(PageType pageType) {
		super();
		this.pageType = pageType;
	}

	public PageType getPageType() {
		return pageType;
	}

	public void setPageType(PageType pageType) {
		this.pageType = pageType;
	}

	public int getExactPage() {
		return exactPage;
	}

}
