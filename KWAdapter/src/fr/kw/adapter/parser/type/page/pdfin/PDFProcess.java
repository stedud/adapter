package fr.kw.adapter.parser.type.page.pdfin;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;

import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.pdfbox.util.Matrix;

import de.kwsoft.mtext.mffmfd.DocumentGeneratorException;
import de.kwsoft.mtext.mffmfd.PageInfo;
import de.kwsoft.mtext.mffmfd.embed.MfdForEmbeddedDocument;
									  
import fr.kw.adapter.document.DataSourceException;
import fr.kw.adapter.document.Datasource;
import fr.kw.adapter.document.DatasourceType;
import fr.kw.adapter.document.Document;
import fr.kw.adapter.parser.process.ParseProcessConfiguration;
import fr.kw.adapter.parser.script.ScriptEngineHelperStatic;
import fr.kw.adapter.parser.type.ParserType;
import fr.kw.adapter.parser.type.page.FileProcessException;
														 
import fr.kw.adapter.parser.type.page.document.Field;
import fr.kw.adapter.parser.type.page.document.Page;
import fr.kw.adapter.parser.type.page.settings.DocumentParserDefinition;
import fr.kw.adapter.parser.type.page.settings.PageDocumentDefinition;
import fr.kw.adapter.parser.type.page.settings.PageSettingsException;
import fr.kw.adapter.parser.type.page.settings.Unit;
import fr.kw.adapter.parser.type.page.settings.field.FieldDefinition;
import fr.kw.adapter.parser.type.page.settings.field.PageType;
import fr.kw.adapter.parser.type.page.settings.pattern.RulePattern;
import fr.utils.LogHelper;
import fr.utils.Utils;

/**
 * @author stedu
 *
 */
public class PDFProcess {

	public static final double UN_MM_EN_POINTS = 2.83465;
	public static final double UN_POINT_EN_MM = 1 / UN_MM_EN_POINTS;
	protected Map<String, DocumentParserDefinition> documentDefinitions = new HashMap<String, DocumentParserDefinition>();
	protected ParseProcessConfiguration configuration;

	public PDFProcess(ParseProcessConfiguration configuration) {
		this.configuration = configuration;

	}

	public void addDocumentDefinition(DocumentParserDefinition docDef) {
		this.documentDefinitions.put(docDef.getName(), docDef);
	}

	public void addDocumentDefinitions(List<DocumentParserDefinition> docDefs) {
		for (DocumentParserDefinition docDef : docDefs) {
			this.addDocumentDefinition(docDef);
		}
	}

	protected static PDDocument loadPDF(File pdf) throws FileProcessException {
		try {

			PDDocument document = PDDocument.load(pdf, MemoryUsageSetting.setupMixed(1024*1024*5));
						   
						  
			return document;
			// textStripper = new PDFTextStripperByArea();
		} catch (InvalidPasswordException e) {
			// TODO Auto-generated catch block
			throw new FileProcessException(e);
		} catch (IOException e) {
			throw new FileProcessException(e);
		}
	}

	/**
	 * Resize the page contents (not the page size itself) accroding to the scale
	 * factor. If scale factor is <=0, an autoscale is made depending on page
	 * contents.
	 * 
	 * @param pdfFile
	 * @param scale
	 * @throws FileProcessException
	 */
	public static void scalePDF(File pdfFile, double scale) throws FileProcessException {

  
		PDDocument pdf = loadPDF(pdfFile);
		try {
			PDPageTree tree = pdf.getDocumentCatalog().getPages();
			Iterator<PDPage> iterator = tree.iterator();
			while (iterator.hasNext()) {

				// System.out.println(PDRectangle.A4.getWidth() + "/" +
				// PDRectangle.A4.getHeight());
				PDPage page = iterator.next();

				boolean landscape = page.getMediaBox().getWidth() > page.getMediaBox().getHeight();
				PDRectangle pageSizeRef = getStandardPageSize(page);
																															 
				float factor = 1f;
				if (scale <= 0) {
					if (pageSizeRef != null) {
						float maxWidth = landscape ? pageSizeRef.getHeight() * 0.97f : pageSizeRef.getWidth() * 0.97f;// 3%
																														// de
																														// marge
						float maxHeight = landscape ? pageSizeRef.getWidth() * 0.97f : pageSizeRef.getHeight() * 0.97f;// 3%
																														// de
																														// marge


						if (page.getMediaBox().getWidth() > maxWidth || page.getMediaBox().getHeight() > maxHeight) {
	   
							float fWidth = maxWidth / page.getMediaBox().getWidth();
							float fHeight = maxHeight / page.getMediaBox().getHeight();

							if (fWidth > fHeight) {
								factor = fHeight;
							} else {
								factor = fWidth;
							}
						}
					}
				} else {
					factor = (float) scale;
				}
	

				PDPageContentStream contentStream = new PDPageContentStream(pdf, page,
						PDPageContentStream.AppendMode.PREPEND, false);
				contentStream.transform(Matrix.getScaleInstance(factor, factor));
				contentStream.close();

			}

			pdf.save(pdfFile);
			pdf.close();

		} catch (IOException e) {
			throw new FileProcessException("Could not scale " + pdfFile, e);
		}

	}

	public List<Document> parse(File pdf) throws IOException, FileProcessException, PageSettingsException,
			DocumentGeneratorException, DataSourceException {
		ScriptEngine scriptEngine = ScriptEngineHelperStatic.getScriptEngine("javascript", true);
		List<Document> result = new ArrayList<Document>();
		
		File tmpFolder = Utils.getTmpFolder();
		tmpFolder.mkdirs();

		String datePattern = "yyyy-MM-dd";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
		String timePattern = "HH-mm-ss";
		SimpleDateFormat simpleTimeFormat = new SimpleDateFormat(timePattern);
		final String date = simpleDateFormat.format(new Date());
		final String time = simpleTimeFormat.format(new Date());

		PDDocument doc = loadPDF(pdf);

		try {
			PDFDocument currentDoc = null;

			int docNum = 0;
			int nbPages = doc.getNumberOfPages();
			boolean isLastPage = false;
			boolean pageUsed = false;
			PageDocumentDefinition currentPageDef = null;
			DocumentParserDefinition currentDocDef = null;

			// List<Rectangle2D> toRemove = new ArrayList<Rectangle2D>();
			PDPage pdfPage = null;

			for (int pageNum = 1; pageNum <= nbPages; pageNum++) {
				pageUsed = false;
				pdfPage = doc.getPage(pageNum - 1);
				Page page = new Page(pdfPage);
				isLastPage = pageNum == nbPages;

				LogHelper.debug("Analysing page " + pageNum);
				boolean newDocTriggered = false;
				boolean pageInDocTriggered = false;

	
				for (DocumentParserDefinition theDocDef : documentDefinitions.values()) {
	 
					for (PageDocumentDefinition pageDef : theDocDef.getPageDefinitions()) {

																	   
							 
	   
													
	   
	  
		   
																															  
																	  
												 
		
												  
		
											 
							 
										 
						   
	   
	  
						RulePattern docPatternRule = pageDef.getMatchRule();

						boolean newDoc = docPatternRule == null ? true
								: docPatternRule.match(page, scriptEngine) && (pageDef.getPageType() == PageType.FIRST);
						boolean inDoc = currentDoc == null ? false
								: (docPatternRule == null || docPatternRule.match(page,scriptEngine))
										&& (pageDef.getPageType() == PageType.NEXT
												|| pageDef.getPageType() == PageType.LAST
												|| pageDef.getPageType() == PageType.ANY);
						inDoc = inDoc && (currentDoc != null
								&& currentDoc.getDefinition().getName().compareTo(theDocDef.getName()) == 0);

						if (newDoc) {
							currentPageDef = pageDef;
							currentDocDef = theDocDef;
							// toRemove.clear();
							// for (FieldDefinition fieldDef : docDef.getFields()) {
							// if (fieldDef.isRemoveText())
							// toRemove.add(fieldDef.getRectPt());
							// }
							newDocTriggered = true;
							break;
						} else if (inDoc) {
							pageInDocTriggered = true;
							break;
						}
					}
				}
				page.setPageDocumentDefinition(currentPageDef);

				if (newDocTriggered) {

					if (currentDoc != null) {
						closeDoc(currentDoc, FilenameUtils.getBaseName(pdf.getName()), tmpFolder);

						File mfdFile = new File(currentDoc.getFile().getPath() + ".mfd");
						fr.kw.adapter.document.Document document = new fr.kw.adapter.document.Document();

						List<PageInfo> pageInfos = MfdForEmbeddedDocument
								.createMfdForEmbeddedDocument(currentDoc.getFile(), "mfd", mfdFile);// Création du
																									// fichier MFD à
																									// partir du PDF
						Datasource datasource = new Datasource(DatasourceType.PDF, mfdFile);
						if (!datasource.isSameDataFile(mfdFile))
							mfdFile.delete();
						datasource.setName(currentDoc.getDefinition().getName());
						document.setPageInfos(pageInfos);

						document.setMainDatasource(datasource);
						document.setName(currentDoc.getDefinition().getName());
						document.setOrigin(pdf.getPath());
						document.setOriginType(ParserType.PAGE_PDF);
						document.setId(pdf.getPath() + "_" + (docNum+1));
						document.setEnvironment(configuration.get("ENVIRONMENT", ""));
						document.setNum(docNum+1);
						for (Field field : currentDoc.getFields().values()) {
							document.getMetadata().put(field.getName(), field.getValue());
						}

						if (currentDoc.getFile().exists())
						{
							if (!currentDoc.getFile().delete()) {
								LogHelper.warn(this.getClass().getSimpleName() + ", Could not delete " + currentDoc.getFile());
							}
						}
						result.add(document);
					}

					docNum++;

					page.setPageDocumentDefinition(currentPageDef);
					currentDoc = new PDFDocument(currentDocDef);
	 
					currentDoc.setPosition(docNum);
					currentDoc.init(doc);

	  
																	
																																		   
		  
												 
	  
					currentDoc.getFields().put("_date", new Field("_date", date));
					currentDoc.getFields().put("_time", new Field("_time", time));
					currentDoc.getFields().put("_docType", new Field("_docType", currentDocDef.getName()));
					currentDoc.getFields().put("_inputFile", new Field("_inputFile", pdf.getPath()));
					currentDoc.getFields().put("_inputFileName", new Field("_inputFileName", pdf.getName()));
					currentDoc.getFields().put("_inputFileBaseName",
							new Field("_inputFileBaseName", FilenameUtils.getBaseName(pdf.getName())));
					currentDoc.getFields().put("_docNum", new Field("_docNum", String.valueOf(docNum)));
					currentDoc.getFields().put("_jobId", new Field("_jobId", pdf.getName()));

					currentDoc.addPage(page);
					pageUsed = true;

				}

				if (isLastPage) {
					if (currentDoc != null) {
						if (!pageUsed && pageInDocTriggered) {
							page.setPageDocumentDefinition(currentPageDef);
							currentDoc.addPage(page);
												 
		
																	  
																																			  
			
												   
		
							pageUsed = true;

						}
	  
						closeDoc(currentDoc, FilenameUtils.getBaseName(pdf.getName()), tmpFolder);

						Document document = new Document();
						File mfdFile = new File(currentDoc.getFile().getPath() + ".mfd");

						List<PageInfo> pageInfos = MfdForEmbeddedDocument
								.createMfdForEmbeddedDocument(currentDoc.getFile(), "mfd", mfdFile);// Création du
																									// fichier MFD à
																									// partir du PDF

						Datasource datasource = new Datasource(DatasourceType.PDF, mfdFile);
						datasource.setName(currentDoc.getDefinition().getName());
						if (!datasource.isSameDataFile(mfdFile))
							mfdFile.delete();

						document.setPageInfos(pageInfos);
						document.setMainDatasource(datasource);
						document.setName(currentDoc.getDefinition().getName());
						document.setOrigin(pdf.getPath());
						document.setOriginType(ParserType.PAGE_PDF);
						document.setId(pdf.getPath() + "_" + (docNum+1));
						document.setEnvironment(configuration.get("ENVIRONMENT", ""));
						document.setNum(docNum+1);
						for (Field field : currentDoc.getFields().values()) {
							document.getMetadata().put(field.getName(), field.getValue());
						}
						if (currentDoc.getFile().exists())
						{
							if (!currentDoc.getFile().delete()) {
								LogHelper.warn(this.getClass().getSimpleName() + ", Could not delete " + currentDoc.getFile());
							}
						}
						result.add(document);
					}

				} else if (!pageUsed) {
					if (currentDoc != null && pageInDocTriggered) {
						page.setPageDocumentDefinition(currentPageDef);
						currentDoc.addPage(page);
												
	   
																	 
																																			 
		   
												  
	   
						pageUsed = true;
					}

				}

				if (!pageUsed)
					LogHelper.warn("Page " + pageNum + " skipped");
			}
			while (!isLastPage)
				;
		} catch (DataSourceException e) {
			throw e;
		} finally {

			doc.close();

		}

		return result;

	}

																																				
  
														   
																		   
  
														

				   
				 
	
																																 
																	 
												
					   
				 
																																 
																		   
							   
													 

   
				
  
  
 
 
	protected void closeDoc(PDFDocument doc, String baseName, File tmpFolder) throws IOException, FileProcessException {
		int nbPages = doc.getPages().size();
		doc.getFields().put("_docPages", new Field("_docPages", String.valueOf(nbPages)));
		int pageNum = 0;
		for (Page page : doc.getPages()) {

			pageNum++;

			for (FieldDefinition fieldDef : doc.getDefinition().getFields()) {
				if (fieldDef.getPage().getPageType() == PageType.ANY
						|| (fieldDef.getPage().getPageType() == PageType.NEXT && pageNum > 1)
						|| (fieldDef.getPage().getPageType() == PageType.LAST && pageNum == nbPages)
						|| (fieldDef.getPage().getPageType() == PageType.EXACT
								&& pageNum == fieldDef.getPage().getExactPage())
						|| (fieldDef.getPage().getPageType() == PageType.FIRST && pageNum == 1)) {
					Field f = null;
					String value;
					try {
						value = extractText((PDPage) page.getNativePage(), fieldDef.getPosition().getX(),
								fieldDef.getPosition().getY(), fieldDef.getDimension().getX(),
								fieldDef.getDimension().getY(), Unit.MM);
						if (value == null)
							value = "";
						f = new Field(fieldDef.getName(), fieldDef.isKeepSpace() ? value : value.trim());
						doc.runFieldScript(f, fieldDef.getScript());
						doc.getFields().put(f.getName(), f);
						if (fieldDef.isRemoveText()) {

						}
					} catch (Exception e) {
						if (f != null)
							doc.setError(new Exception(
									"Exception while evaluating field " + f.getName() + " : " + e.getMessage(), e));
						else
							doc.setError(e);
					}

				}
			}

		}

		File pdfDoc = new File(tmpFolder,  baseName + "_" + doc.getPosition() + ".pdf");

		doc.createFile(pdfDoc);
		doc.close();

	}

	public static String extractText(PDPage page, float x, float y, float w, float h, Unit unit)
			throws FileProcessException, IOException {

		if (w == 0.0 && h == 0.0 && x == 0.0 && y == 0.0)
			return "";

		PDFTextStripperByArea textStripper = new PDFTextStripperByArea();
		Rectangle2D rect = new java.awt.geom.Rectangle2D.Float(getPoints(x, unit), getPoints(y, unit),
				getPoints(w, unit), getPoints(h, unit));
		String regionName = "region_" + x + "_" + y + "_" + w + "_" + h;
		textStripper.addRegion(regionName, rect);
		textStripper.extractRegions(page);

		String textForRegion = textStripper.getTextForRegion(regionName);
		char last = 0;
		if (textForRegion.length() > 0) {
			last = textForRegion.charAt(textForRegion.length() - 1);
		}

		while (textForRegion.length() > 0 && (last == '\n' || last == '\r')) {
			textForRegion = textForRegion.substring(0, Math.max(0, textForRegion.length() - 1 - 1));
			if (textForRegion.length() > 0) {
				last = textForRegion.charAt(textForRegion.length() - 1);
			} else {
				last = 0;
			}
		}

		return textForRegion;
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

	public static List<PageInfo> pdf2mfd(File pdf, File targetMfd) throws DocumentGeneratorException {

		List<PageInfo> pageInfos = MfdForEmbeddedDocument.createMfdForEmbeddedDocument(pdf, "mfd", targetMfd);// Création
																												// du
																												// fichier
																												// MFD à
																												// partir
																												// du
																												// PDF
		return pageInfos;

	}

	public static PDRectangle getStandardPageSize(PDPage page) {
		boolean landscape = page.getMediaBox().getWidth() > page.getMediaBox().getHeight();
		double height = landscape ? page.getMediaBox().getWidth() : page.getMediaBox().getHeight();
		if (height <= PDRectangle.A6.getHeight() * 1.1 && height >= PDRectangle.A6.getHeight() * 0.9)
			return PDRectangle.A6;
		if (height <= PDRectangle.A5.getHeight() * 1.1 && height >= PDRectangle.A5.getHeight() * 0.9)
			return PDRectangle.A5;
		if (height <= PDRectangle.A4.getHeight() * 1.1 && height >= PDRectangle.A4.getHeight() * 0.9)
			return PDRectangle.A4;
		if (height <= PDRectangle.A3.getHeight() * 1.1 && height >= PDRectangle.A3.getHeight() * 0.9)
			return PDRectangle.A3;
		if (height <= PDRectangle.A2.getHeight() * 1.1 && height >= PDRectangle.A2.getHeight() * 0.9)
			return PDRectangle.A2;
		if (height <= PDRectangle.A1.getHeight() * 1.1 && height >= PDRectangle.A1.getHeight() * 0.9)
			return PDRectangle.A1;
		if (height <= PDRectangle.A0.getHeight() * 1.1 && height >= PDRectangle.A0.getHeight() * 0.9)
			return PDRectangle.A0;
		if (height <= PDRectangle.LEGAL.getHeight() * 1.1 && height >= PDRectangle.LEGAL.getHeight() * 0.9)
			return PDRectangle.LEGAL;
		if (height <= PDRectangle.LETTER.getHeight() * 1.1 && height >= PDRectangle.LETTER.getHeight() * 0.9)
			return PDRectangle.LETTER;

		return null;

	}
}
