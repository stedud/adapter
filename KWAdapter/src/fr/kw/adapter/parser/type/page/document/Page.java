package fr.kw.adapter.parser.type.page.document;

import fr.kw.adapter.parser.type.page.settings.PageDocumentDefinition;

public class Page {

	protected Object nativePage = null;
	protected PageDocumentDefinition pageDocumentDefinition = null;

	private Page() {
	}

	public Page(Object nativePage) {

		this.nativePage = nativePage;
	}

	public Object getNativePage() {
		return nativePage;
	}

	public void setNativePage(Object nativePage) {
		this.nativePage = nativePage;
	}

	public PageDocumentDefinition getPageDocumentDefinition() {
		return pageDocumentDefinition;
	}

	public void setPageDocumentDefinition(PageDocumentDefinition pageDocumentDefinition) {
		this.pageDocumentDefinition = pageDocumentDefinition;
	}

}
