package fr.kw.adapter.parser.type.page;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fr.kw.adapter.document.Document;
import fr.kw.adapter.parser.process.ParseProcessConfiguration;
import fr.kw.adapter.parser.type.page.document.ADocument;
import fr.kw.adapter.parser.type.page.settings.DocumentParserDefinition;
import fr.kw.adapter.parser.type.page.settings.PageSettingsException;
import fr.kw.adapter.parser.type.page.settings.Unit;

public abstract class AFileProcess {

	public static final double UN_MM_EN_POINTS = 2.83465;
	public static final double UN_POINT_EN_MM = 1 / UN_MM_EN_POINTS;
	public static final File DESC_FOLDER = new File("dsc");
	protected List<DocumentParserDefinition> documentDefinitions = new ArrayList<>();
	protected ParseProcessConfiguration config;
	protected int threads = 1;
	protected boolean parseOnly;

	public AFileProcess(ParseProcessConfiguration config, int threads) {

		this.config = config;
		this.threads = threads;

	}

	public abstract void loadDefinitions();

	public List<DocumentParserDefinition> getDocumentDefinitions() {
		return documentDefinitions;
	}

	public static float getPoints(float mm, Unit unit) {

		switch (unit) {
		case MM:
			return (float) (mm * UN_MM_EN_POINTS);
		case POINT:
			return mm;
		default:
			return mm;
		}

	}

	public AFileProcess() {
		super();
	}

	public boolean isParseOnly() {
		return parseOnly;
	}

	public void setParseOnly(boolean parseOnly) {
		this.parseOnly = parseOnly;
	}

	public abstract void process(File pdf, Map<File, List<Document>> documentsToAddInContext)
			throws FileProcessException, IOException, PageSettingsException;

	public ParseProcessConfiguration getConfig() {
		return config;
	}

	public static void addAdditionalDataSources(List<ADocument> mainDocuments,
			Map<File, List<Document>> additionalDocsMap) {

		if (additionalDocsMap == null)
			return;

		for (File addF : additionalDocsMap.keySet()) {

			List<Document> additionalDocs = additionalDocsMap.get(addF);

			for (int i = additionalDocs.size() - 1; i >= 0; i--) {
				Document toAdd = additionalDocs.get(i);
				for (int j = i; j < mainDocuments.size(); j++) {
					ADocument mainDoc = mainDocuments.get(j);

					mainDoc.getAddtionalContext().put(toAdd.getName(), toAdd);

				}
			}
		}

	}

}