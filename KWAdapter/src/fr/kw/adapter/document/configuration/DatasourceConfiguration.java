package fr.kw.adapter.document.configuration;

import org.apache.commons.lang3.StringUtils;

public class DatasourceConfiguration {

	protected String name;
	protected String documentName;

	public DatasourceConfiguration() {
		// TODO Auto-generated constructor stub
	}

	public DatasourceConfiguration(String name) {
		super();
		this.name = name;
	}

	public DatasourceConfiguration(String name, String documentName) {
		super();
		this.name = name;
		this.documentName = documentName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDocumentName() {
		return documentName;
	}

	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}

	/**
	 * Indicate if the datasource can be attached to a document. Return true if the
	 * datasource has no document name, or if the document name is the same.
	 * 
	 * @param documentName
	 * @return
	 */
	public boolean isRelatedToDocument(String documentName) {
		if (StringUtils.isBlank(documentName))
			return true;
		return (StringUtils.compare(documentName, this.getDocumentName()) == 0);

	}

	/**
	 * Indicate if the datasource can be attached to a document. Return true if the
	 * document name is the same.
	 * 
	 * @param documentName
	 * @return
	 */
	public boolean hasSameDocumentName(String documentName) {
		return (StringUtils.compare(documentName, this.getDocumentName()) == 0);

	}

}
