package fr.kw.adapter.parser.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.script.ScriptEngine;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.kwsoft.mtext.mffmfd.DocumentGeneratorException;
import de.kwsoft.mtext.util.streamio.StringInputStream;
import fr.freemarker.FreeMarkerException;
import fr.freemarker.FreeMarkerHelper;
import fr.kw.adapter.document.DataSourceException;
import fr.kw.adapter.document.Datasource;
import fr.kw.adapter.document.DatasourceType;
import fr.kw.adapter.document.Document;
import fr.kw.adapter.document.configuration.DatasourceConfiguration;
import fr.kw.adapter.document.configuration.DocumentConfiguration;
import fr.kw.adapter.document.configuration.DocumentConfigurationException;
import fr.kw.adapter.parser.DataDescriptionException;
import fr.kw.adapter.parser.IParser;
import fr.kw.adapter.parser.event.Event;
import fr.kw.adapter.parser.event.Field;
import fr.kw.adapter.parser.event.Record;
import fr.kw.adapter.parser.jdbc.JdbcException;
import fr.kw.adapter.parser.jdbc.JdbcManager;
import fr.kw.adapter.parser.jdbc.JdbcQuery;
import fr.kw.adapter.parser.jdbc.JdbcSettings;
import fr.kw.adapter.parser.script.ScriptEngineHelper;
import fr.kw.adapter.parser.script.ScriptEngineHelperStatic;
import fr.kw.adapter.parser.script.ScriptException;
import fr.kw.adapter.parser.type.ParserType;
import fr.kw.adapter.parser.type.fieldin.FieldParser;
import fr.kw.adapter.parser.type.fieldin.FieldParserConfiguration;
import fr.kw.adapter.parser.type.page.FileProcessException;
import fr.kw.adapter.parser.type.page.otfin.OTFProcess;
import fr.kw.adapter.parser.type.page.pagein.PageInProcess;
import fr.kw.adapter.parser.type.page.pagein.settings.PageInParserDefinitionLoader;
import fr.kw.adapter.parser.type.page.settings.DocumentParserDefinition;
import fr.kw.adapter.parser.type.page.settings.PageParserDefinitionLoader;
import fr.kw.adapter.parser.type.page.settings.PageSettingsException;
import fr.kw.adapter.parser.type.rdi.R3rdiParser;
import fr.kw.adapter.parser.type.rdi.R3rdiParserConfiguration;
import fr.kw.adapter.parser.type.recordin.RecordParser;
import fr.kw.adapter.parser.type.recordin.RecordParserConfiguration;
import fr.kw.adapter.parser.type.xml.XmlParser;
import fr.kw.adapter.parser.type.xml.XmlParserConfiguration;
import fr.utils.LogHelper;
import fr.utils.Utils;
import fr.utils.configuration.ConfigurationException;

public class ParseProcess {



	public static final String PDFIN_FILE_EXTENSION = "pdfin";

	public static final String SAPGOFIN_FILE_EXTENSION = "sapgofin";

	protected ParseProcessConfiguration configuration;
	protected HashMap<String, Document> additionalDataSources;
	protected List<DocumentParserDefinition> otfParsers = new ArrayList<>();
	protected List<DocumentParserDefinition> pageInParsers = new ArrayList<>();
	protected List<IParser<?, ?>> parsers = new ArrayList<IParser<?, ?>>();

	protected List<IParser<?, ?>> xmlParsers = new ArrayList<IParser<?, ?>>();

	protected List<DocumentParserDefinition> pdfParsers = new ArrayList<>();
	private DocumentBuilderFactory xmlDocumentBuilderFactory;
	private FileFilter fileFilter;

	private ScriptEngineHelper scriptEngineHelper;

	public ParseProcess(ParseProcessConfiguration configuration)
			throws DataDescriptionException, PageSettingsException {
		this.scriptEngineHelper = new ScriptEngineHelper();

		PageParserDefinitionLoader pageTypeLoader = null;
		this.configuration = configuration;
		List<File> allParsers = this.configuration.getParsers();
		for (File parserFile : allParsers) {
			if (!parserFile.canRead()) {
				LogHelper.error("Could not read " + parserFile.getPath());
				throw new DataDescriptionException("Could not read " + parserFile.getPath());
			}
			ParserType parserType = getParserType(parserFile);
			switch (parserType) {
			case XML: {
				IParser<?, ?> xmlParser = loadParser(parserFile);
				xmlParsers.add(xmlParser);
			}
				break;
			case PAGE_PDF: {
				
				if (pageTypeLoader == null)
					pageTypeLoader = new PageParserDefinitionLoader();

				DocumentParserDefinition pdfParser = pageTypeLoader.load(parserFile, configuration);
				pdfParsers.add(pdfParser);
				LogHelper.info("PDF Parser " + pdfParser.getName() + " loaded");
			}
				break;
			case PAGE_SAPGOF: {
				
				if (pageTypeLoader == null)
					pageTypeLoader = new PageParserDefinitionLoader();

				DocumentParserDefinition otfParser = pageTypeLoader.load(parserFile, configuration);
				otfParsers.add(otfParser);
				LogHelper.info("OTF Parser " + otfParser.getName() + " loaded");
			}
			case PAGE: {
				if (pageTypeLoader == null)
					pageTypeLoader = new PageInParserDefinitionLoader();

				DocumentParserDefinition pageInParser = pageTypeLoader.load(parserFile, configuration);
				pageInParsers.add(pageInParser);
				LogHelper.info("PageIn Parser " + pageInParser.getName() + " loaded");
			}
				break;
			default: {
				IParser<?, ?> parser = loadParser(parserFile);
				parsers.add(parser);
			}

			}

		}

	}

	protected static ParserType getParserType(File parserFile) {

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(parserFile));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.trim().toUpperCase().startsWith("STREAMIN"))
					return ParserType.RECORD;
				else if (line.trim().toUpperCase().startsWith("FIELDIN"))
					return ParserType.FIELD;
				else if (line.trim().toUpperCase().startsWith("R3RDI"))
					return ParserType.R3RDI;
				else if (line.trim().toUpperCase().startsWith("XMLIN"))
					return ParserType.XML;
				else if (line.trim().toUpperCase().startsWith("PAGEIN"))
					return ParserType.PAGE;
				else{
					if (parserFile.getName().toLowerCase().endsWith(SAPGOFIN_FILE_EXTENSION)) {
						return ParserType.PAGE_SAPGOF;
					} else if (parserFile.getName().toLowerCase().endsWith(PDFIN_FILE_EXTENSION)) {
						return ParserType.PAGE_PDF;
					}
				}
			}
		} catch (IOException e) {
			LogHelper.error("Could not find parser type for " + parserFile + " : " + e.getMessage(), e);
		} finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					LogHelper.error("Could not close " + parserFile + " : " + e.getMessage(), e);
				}
		}
		return ParserType.NONE;

	}

	public static boolean isGOF(File dataFile) {
		InputStream inStream = null;
		LineIterator lineIterator = null;
		try {
			if (StringUtils.endsWithIgnoreCase(dataFile.getName(), "gof")) {
				return true;
			}

			inStream = new FileInputStream(dataFile);

			String line = "";
			lineIterator = IOUtils.lineIterator(new InputStreamReader(inStream));
			do {
				line = lineIterator.nextLine();
			} while (StringUtils.isBlank(line));

			return StringUtils.containsIgnoreCase(line, "*MAJOR");
		} catch (IOException e) {
			LogHelper.error("Error while checking GOF nature of " + dataFile + " : " + e.getMessage(), e);

		} catch (Throwable e) {
			LogHelper.error("Error while checking GOF nature of " + dataFile + " : " + e.getMessage(), e);

		} finally {
			if (inStream != null)
				try {
					inStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

		}

		return false;
	}

	public static boolean isPDF(File dataFile) {
		InputStream inStream = null;
		LineIterator lineIterator = null;
		try {
			if (StringUtils.endsWithIgnoreCase(dataFile.getName(), "pdf")) {
				return true;
			}

			inStream = new FileInputStream(dataFile);

			String line = "";
			lineIterator = IOUtils.lineIterator(new InputStreamReader(inStream));
			do {
				line = lineIterator.nextLine();
			} while (StringUtils.isBlank(line));

			return StringUtils.contains(StringUtils.trim(line.toUpperCase()), "PDF");
		} catch (IOException e) {
			LogHelper.error("Error while checking pdf nature of " + dataFile + " : " + e.getMessage(), e);

		} catch (Throwable e) {
			LogHelper.error("Error while checking pdf nature of " + dataFile + " : " + e.getMessage(), e);

		} finally {
			try {
				if (inStream != null)
					inStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return false;
	}

	public static boolean isXML(File dataFile) {
		InputStream inStream = null;
		LineIterator lineIterator = null;
		try {
			if (StringUtils.endsWithIgnoreCase(dataFile.getName(), "xml")) {
				return true;
			}
			inStream = new FileInputStream(dataFile);

			String line = "";
			lineIterator = IOUtils.lineIterator(new InputStreamReader(inStream));
			do {
				line = lineIterator.nextLine();
			} while (StringUtils.isBlank(line));

			return StringUtils.trim(line).contains("<");
		} catch (IOException e) {
			LogHelper.error("Error while checking xml nature of " + dataFile + " : " + e.getMessage(), e);

		} catch (Throwable e) {
			LogHelper.error("Error while checking xml nature of " + dataFile + " : " + e.getMessage(), e);

		} finally {
			if (inStream != null)
				try {
					inStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

		}

		return false;
	}

	public static boolean isXML(String data) {
		InputStream inStream = null;
		LineIterator lineIterator = null;
		try {
			inStream = new StringInputStream(data);

			String line = "";
			lineIterator = IOUtils.lineIterator(new InputStreamReader(inStream));
			do {
				line = lineIterator.nextLine();
			} while (StringUtils.isBlank(line.trim()));

			return line.trim().contains("<");
		} catch (Throwable e) {
			LogHelper.error("Error while checking xml nature of " + data + " : " + e.getMessage(), e);

		} finally {
			if (inStream != null)
				try {
					inStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

		}

		return false;
	}

	protected void addCompanionDatasources(Document doc, Map<String, Document> pendingForDataSource)
			throws DocumentConfigurationException, ConfigurationException, JdbcException {
		DocumentConfiguration docConfig = configuration.getDocumentConfiguration(doc.getName());
		for (Document companion : pendingForDataSource.values()) {
			if (companion.getName().compareTo(doc.getName()) == 0)
				continue;
			for (DatasourceConfiguration dsConf : docConfig.getAdditionalDatasources()) {
				if (dsConf.hasSameDocumentName(companion.getName())) {

					doc.getAdditionalDatasources().put(dsConf.getName(), companion.getMainDatasource());
					for (Datasource ds : companion.getAdditionalDatasources().values()) {
						doc.getAdditionalDatasources().put(ds.getName(), ds);
					}
				}

			}
		}
	}

	public void addOtherDatasources(Document doc)
			throws DocumentConfigurationException, ConfigurationException, JdbcException, IOException {
		DocumentConfiguration docConfig = configuration.getDocumentConfiguration(doc.getName());
		for (DatasourceConfiguration dsConf : docConfig.getAdditionalDatasources()) {

			if (dsConf.getDocumentName().startsWith("JDBC")) {// TODO : JDBC QUERY as data source contents
																// JDBC(pathToJdbcSettings)
				String jdbcSettingsPath = getJdbcSettingsPath(dsConf.getName());
				try {
					Map<String, Object> context = FreeMarkerHelper.getContextFromDocument(doc);

					String jdbcSettingsContent = FileUtils.readFileToString(new File(jdbcSettingsPath));
					jdbcSettingsContent = FreeMarkerHelper.parseExpression(jdbcSettingsContent, dsConf.getName(),
							FreeMarkerHelper.getContextFromDocument(doc));
					// Make the expressions that must be interpreted just before executing the query
					// syntaxly correct for freemarker
					jdbcSettingsContent = StringUtils.replace(jdbcSettingsContent, "&{", "${");

					JdbcSettings jdbcSettings = JdbcSettings.parse(jdbcSettingsContent);
					JdbcManager jdbcManager = new JdbcManager(jdbcSettings);

					Event jdbcEvent = new Event(dsConf.getName());
					for (JdbcQuery query : jdbcSettings.getQueries()) {
						jdbcManager.runQuery(jdbcEvent, query, context);
					}
					File tmpDataFile = File.createTempFile("data_", ".xml");
					// tmpDataFile.deleteOnExit();
					FileUtils.writeByteArrayToFile(tmpDataFile,
							jdbcEvent.toXML().getBytes(StandardCharsets.UTF_8.name()));
					Datasource ds = new Datasource(DatasourceType.XML, dsConf.getName(), tmpDataFile);
					doc.getAdditionalDatasources().put(ds.getName(), ds);
					if (!ds.isSameDataFile(tmpDataFile))
						tmpDataFile.delete();

				} catch (IOException | FreeMarkerException | DataSourceException e) {
					throw new DocumentConfigurationException(
							"Could not parse jdbc settings '" + jdbcSettingsPath + "' : " + e.getMessage(), e);
				} catch (JdbcException e) {
					throw e;
				}
			} else if (dsConf.getDocumentName().startsWith("SCRIPT")) {
				String scriptPath = StringUtils.substringAfter(dsConf.getDocumentName(), "(").trim();

				String scriptMode = StringUtils.substringBefore(dsConf.getDocumentName(), "(").trim();

				scriptPath = StringUtils.removeEnd(scriptPath, ")").trim();
				String script = FileUtils.readFileToString(new File(scriptPath));
				String scriptExtension = FilenameUtils.getExtension(scriptPath).toLowerCase();

				scriptMode = scriptMode.substring("SCRIPT".length());// String after "Script". ex: SCRIPTJAVA or
																		// SCRIPTJYTHON

				String scriptLanguage = "javascript";// default
				if (StringUtils.isNotBlank(scriptMode)) {
					scriptLanguage = scriptMode.toLowerCase();
					if (!Character.isAlphabetic(scriptLanguage.charAt(0)))
						scriptLanguage = scriptLanguage.substring(1);
				} else {
					if (StringUtils.isNotBlank(scriptExtension))
						scriptLanguage = scriptExtension;
				}

				Map<String, Object> context;
				try {
					context = FreeMarkerHelper.getContextFromDocument(doc);
					script = FreeMarkerHelper.parseExpression(script, dsConf.getName(), context);
					ScriptEngine scriptEngine = scriptEngineHelper.getScriptEngine(scriptLanguage);
					scriptEngine.put("doc", doc);

					Object result = scriptEngine.eval(script);
					if (result != null) {
						File tmpDataFile = null;
						if (result instanceof File) {
							tmpDataFile = (File) result;
						} else {
							String resultValue = result.toString();
							tmpDataFile = File.createTempFile("data_", ".xml");
							FileUtils.writeStringToFile(tmpDataFile, resultValue);
						}

						if (ParseProcess.isXML(tmpDataFile)) {
							FileUtils.writeStringToFile(tmpDataFile, result.toString());
							Datasource ds = new Datasource(DatasourceType.XML, dsConf.getName(), tmpDataFile);
							if (!ds.isSameDataFile(tmpDataFile))
								tmpDataFile.delete();
							doc.getAdditionalDatasources().put(ds.getName(), ds);
						} else {
							throw new DocumentConfigurationException(
									"Script '" + scriptPath + "' must return XML data as String.");
						}
					}

				} catch (FreeMarkerException | javax.script.ScriptException | DataSourceException e) {
					throw new DocumentConfigurationException(
							"Could not parse script '" + scriptPath + "' : " + e.getMessage(), e);
				}

			}

		}
	}

	/**
	 * @param dsConf
	 * @return
	 */
	public static String getJdbcSettingsPath(String dsConf) {
		String jdbcSettingsPath = StringUtils.substringAfter(dsConf, "(").trim();
		jdbcSettingsPath = StringUtils.removeEnd(jdbcSettingsPath, ")").trim();
		return jdbcSettingsPath;
	}

	private void closeDocument(Event currEvent, Document doc, Map<String, Document> pendingForDataSource)
			throws UnsupportedEncodingException, IOException, DocumentConfigurationException, ConfigurationException,
			JdbcException, ScriptException, ParseProcessException, TransformerException, DataSourceException {
		if (doc != null && currEvent != null) {
			// Cloture du dernier document en cours

			// TODO : run jdbc datasources in event
//for (Record r : currEvent.getRecords()) {if (r.getName().compareTo("six") == 0) System.out.println(r.toXML());}
			addExternalRecords(currEvent, currEvent, doc);

			DocumentConfiguration docConfig = configuration.getDocumentConfiguration(doc.getName());
			File tmpDataFile = File.createTempFile("data_", ".xml");
			// tmpDataFile.deleteOnExit();
			FileUtils.writeByteArrayToFile(tmpDataFile, currEvent.toXML().getBytes(StandardCharsets.UTF_8.name()));

			applyDocumentFilter(tmpDataFile, doc);

			Datasource mainDatasource = new Datasource(DatasourceType.XML, docConfig.getMainDatasource().getName(),
					tmpDataFile);

			doc.setMainDatasource(mainDatasource);
			for (DatasourceConfiguration dsConf : docConfig.getAdditionalDatasources()) {

				if (dsConf.isRelatedToDocument(currEvent.getName())) {
					Datasource ds = new Datasource(DatasourceType.XML, dsConf.getName(), tmpDataFile);
					doc.getAdditionalDatasources().put(ds.getName(), ds);
				}
			}

			if (!mainDatasource.isSameDataFile(tmpDataFile))
				tmpDataFile.delete();

			addCompanionDatasources(doc, pendingForDataSource);
			addOtherDatasources(doc);
			addExtraDocument(new HashMap<String, List<Document>>(), doc, docConfig);

		}
	}

	private void addExternalRecords(Record currRecord, Event currEvent, Document doc)
			throws JdbcException, ScriptException {

//		LogHelper.info("addJdbcRecords for record " + currRecord.getName());
		if (currRecord.getDefinition() == null)
			return;
		int i = 0;
		for (String externConfig : currRecord.getDefinition().getExternalDataDefinitions()) {
			i++;
			if (externConfig.toLowerCase().startsWith("jdbc"))
				try {
					LogHelper.info("Processing " + externConfig + " for record " + currRecord.getName());
					Map<String, Object> context = FreeMarkerHelper.getContextFromDocument(doc);

					Map<String, Object> eventContext = FreeMarkerHelper.getContextFromRecord(currEvent);
					context.put("event", eventContext);
					context.put(currEvent.getName(), eventContext);

					String settingsPath = getJdbcSettingsPath(externConfig);
					String jdbcSettingsContent = FileUtils.readFileToString(new File(settingsPath));
					jdbcSettingsContent = FreeMarkerHelper.parseExpression(jdbcSettingsContent,
							"jdbc_" + currRecord.getName() + "_" + i, context);

					jdbcSettingsContent = StringUtils.replace(jdbcSettingsContent, "&{", "${");

					JdbcSettings jdbcSettings = JdbcSettings.parse(jdbcSettingsContent);
					JdbcManager jdbcManager = new JdbcManager(jdbcSettings);

					for (JdbcQuery query : jdbcSettings.getQueries()) {
						jdbcManager.runQuery(currRecord, query, context);
					}
				} catch (FreeMarkerException | IOException | JdbcException e) {
					LogHelper.error("Could not process JDBC process '" + externConfig + "' for record "
							+ currRecord.getName() + " : " + e.getMessage(), e);
					if (e instanceof JdbcException)
						throw (JdbcException) e;
					else
						throw new JdbcException("Could not process JDBC process '" + externConfig + "' for record "
								+ currRecord.getName() + " : " + e.getMessage(), e);
				}
			else if (externConfig.toLowerCase().startsWith("script")) {
				try {
					String scriptPath = StringUtils.substringAfter(externConfig, "(").trim();

					String scriptMode = StringUtils.substringBefore(externConfig, "(").trim();

					scriptPath = StringUtils.removeEnd(scriptPath, ")").trim();
					String script = FileUtils.readFileToString(new File(scriptPath));
					String scriptExtension = FilenameUtils.getExtension(scriptPath).toLowerCase();

					scriptMode = scriptMode.substring("SCRIPT".length());// String after "Script". ex: SCRIPTJAVA or
																			// SCRIPTJYTHON

					String scriptLanguage = "javascript";// default
					if (StringUtils.isNotBlank(scriptMode)) {
						scriptLanguage = scriptMode.toLowerCase();
						if (!Character.isAlphabetic(scriptLanguage.charAt(0))) {
							if (scriptLanguage.substring(1).length() >= 1)
								scriptLanguage = scriptLanguage.substring(1);
						}
					} else {
						if (StringUtils.isNotBlank(scriptExtension))
							scriptLanguage = scriptExtension;
					}

					Map<String, Object> context;

					context = FreeMarkerHelper.getContextFromDocument(doc);
					script = FreeMarkerHelper.parseExpression(script, "script_" + currRecord.getName() + "_" + i,
							context);
					ScriptEngine scriptEngine = scriptEngineHelper.getScriptEngine(scriptLanguage);
					scriptEngine.put("doc", doc);
					scriptEngine.put("event", currEvent);
					scriptEngine.put("record", currRecord);
					scriptEngine.put("current", currRecord);

					Object result = scriptEngine.eval(script);
					if (result != null) {
						File tmpDataFile = null;
						if (result instanceof Record) {
							if (!currRecord.getRecords().contains(result)) {
								((Record) result).setParent(currRecord);
								currRecord.getRecords().add((Record) result);
							}
						}

					} else if (result instanceof Field) {
						if (!currRecord.getFields().contains(result)) {
							currRecord.getFields().add((Field) result);
						}
					} else {
						LogHelper.warn("Event script " + externConfig
								+ " should not return value. Record and/or event should be modified by the script.");
					}

				} catch (Exception e) {
					throw new ScriptException("Could not execute " + externConfig + " : " + e.getMessage(), e);
				}

			}

		}
		for (Record child : currRecord.getRecords()) {
			addExternalRecords(child, currEvent, doc);
		}

	}

	protected File applyFileFilter(File inputFile) throws ParseProcessException {

		return this.fileFilter.filter(inputFile);

	}

	protected void applyDocumentFilter(File dataFile, Document doc)
			throws DocumentConfigurationException, ConfigurationException, IOException {
		if (doc == null)
			return;
		File tmpDataFileResult = null;
		DocumentConfiguration docConfig = configuration.getDocumentConfiguration(doc.getName());
		if (StringUtils.isNotBlank(docConfig.getFilter())) {
			if (docConfig.getFilter().toLowerCase().endsWith("xsl")
					|| docConfig.getFilter().toLowerCase().endsWith("xslt")) {// apply xslt filter
				try {
					if (xmlDocumentBuilderFactory == null)
						xmlDocumentBuilderFactory = DocumentBuilderFactory.newInstance();

					StreamSource stylesource = new StreamSource(new File(docConfig.getFilter()));
					tmpDataFileResult = File.createTempFile("data_", ".xml");
					Transformer transformer = TransformerFactory.newInstance().newTransformer(stylesource);
					transformer.transform(new StreamSource(dataFile), new StreamResult(tmpDataFileResult));
					dataFile.delete();
					FileUtils.copyFile(tmpDataFileResult, dataFile);
					tmpDataFileResult.delete();

				} catch (TransformerFactoryConfigurationError | TransformerException e) {

					if (tmpDataFileResult != null)
						tmpDataFileResult.delete();

					throw new DocumentConfigurationException(
							"Could not convert document xml data using xslt " + docConfig.getFilter(), e);
				}

			} else {// apply stdin/stdout filter
				Process process = Runtime.getRuntime().exec(docConfig.getFilter());
				OutputStream procIn = process.getOutputStream();
				InputStream procOut = process.getInputStream();
				OutputStream errors = process.getOutputStream();

				InputStream tmpIS = new FileInputStream(dataFile);
				Utils.pipeStream(tmpIS, procIn);
				tmpDataFileResult = File.createTempFile("data_", ".xml");

				Utils.pipeStream(procOut, new FileOutputStream(tmpDataFileResult));
				File errorFile = File.createTempFile("errors_", ".txt");
				OutputStream errorOS = new FileOutputStream(errorFile);
				Utils.pipeStream(procOut, errorOS);

				try {
					process.waitFor(5000 + dataFile.length(), TimeUnit.MILLISECONDS);
					dataFile.delete();
					FileUtils.copyFile(tmpDataFileResult, dataFile);
					tmpDataFileResult.delete();

				} catch (InterruptedException e) {
					dataFile.delete();
					if (tmpDataFileResult != null)
						tmpDataFileResult.delete();

					throw new DocumentConfigurationException("Time out with document filter : " + docConfig.getFilter(),
							e);
				} finally {
					IOUtils.closeQuietly(procOut);
					IOUtils.closeQuietly(tmpIS);
					IOUtils.closeQuietly(procOut);
					IOUtils.closeQuietly(errorOS);
					IOUtils.closeQuietly(procIn);
					IOUtils.closeQuietly(errors);
				}
			}
		}
	}

	public void clear() {
		additionalDataSources.clear();
	}

	public synchronized List<Document> parse(File dataFile)
			throws ParseProcessException, UnsupportedEncodingException, DocumentConfigurationException, IOException,
			ConfigurationException, JdbcException, ScriptException, TransformerException, DataSourceException {
		List<Document> documents = new ArrayList<Document>();

		ScriptEngine scriptEngine = ScriptEngineHelperStatic.getScriptEngine("javascript", true);
	
		File originalDataFile = dataFile;

		LogHelper.info("Parsing " + dataFile);
		// TODO : détecter si XML ou AFP en priorité
		ParserType dataType = ParserType.NONE;
		if (additionalDataSources == null) {
			additionalDataSources = new HashMap<String, Document>();
		}

		boolean isXml = isXML(dataFile);
		if (isXml) {
			dataType = ParserType.XML;
		} else {
			boolean isPdf = isPDF(dataFile);
			if (isPdf) {
				dataType = ParserType.PAGE_PDF;
			} else {
				boolean isGof = isGOF(dataFile);
				if (isGof)
					dataType = ParserType.PAGE_SAPGOF;
				else {
				}
			}

		}

		switch (dataType) {

		case PAGE_PDF: {
			Map<String, List<Document>> additionalDatasources = new HashMap<String, List<Document>>();

			fr.kw.adapter.parser.type.page.pdfin.PDFProcess pdfProcess = new fr.kw.adapter.parser.type.page.pdfin.PDFProcess(
					configuration);
			pdfProcess.addDocumentDefinitions(pdfParsers);
			List<Document> result;
			try {
				result = pdfProcess.parse(dataFile);
			} catch (IOException | FileProcessException | PageSettingsException | DocumentGeneratorException e1) {
				throw new ParseProcessException(
						"Could not parse PDF file '" + dataFile.getPath() + "' : " + e1.getMessage(), e1);
			}
			// TODO : additional data sources for Control Job Context
			for (Document doc : result) {
				DocumentConfiguration docConfiguration;
				try {
					docConfiguration = configuration.getDocumentConfiguration(doc.getName());
				} catch (ConfigurationException e) {
					LogHelper.error("Could not process pdf document '" + doc.getName() + ", " + doc.getOrigin() + " : "
							+ e.getMessage(), e);
					continue;
				}
				addExtraDocument(additionalDatasources, doc, docConfiguration);
				addOtherDatasources(doc);
				documents.add(doc);
			}
			break;

		}
		case PAGE_SAPGOF: {

			Map<String, List<Document>> additionalDatasources = new HashMap<String, List<Document>>();
			OTFProcess otfProcess = new OTFProcess(configuration);
			otfProcess.addDocumentDefinitions(otfParsers);
			List<Document> result;
			try {
				result = otfProcess.parse(dataFile);
			} catch (IOException | FileProcessException | PageSettingsException | DocumentGeneratorException e1) {
				throw new ParseProcessException(
						"Could not parse OTF file '" + dataFile.getPath() + "' : " + e1.getMessage(), e1);
			}
			// TODO : additional data sources for Control Job Context
			for (Document doc : result) {
				DocumentConfiguration docConfiguration;
				try {
					docConfiguration = configuration.getDocumentConfiguration(doc.getName());
				} catch (ConfigurationException e) {
					LogHelper.error("Could not process OTF document '" + doc.getName() + ", " + doc.getOrigin() + " : "
							+ e.getMessage(), e);
					continue;
				}
				addExtraDocument(additionalDatasources, doc, docConfiguration);
				addOtherDatasources(doc);
				documents.add(doc);
			}
			break;
		}
		


		case XML: {// TODO
					// Find xml document type, put FULL xml in a single document and return it.
					// split will be done by batchProcessor

			Map<String, List<Document>> additionalDatasources = new HashMap<String, List<Document>>();
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = null;
			try {
				docBuilder = docBuilderFactory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				LogHelper.error("Could not process XML file '" + dataFile.getPath() + " : " + e.getMessage(), e);
			}
			XPath xPath = XPathFactory.newInstance().newXPath();

			for (IParser<?, ?> xmlParserGeneric : xmlParsers) {
				// charger le document pour evaluer les expressions xpath

				XmlParser xmlParser = (XmlParser) xmlParserGeneric;
				org.w3c.dom.Document xmlDoc;
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(dataFile);
					xmlDoc = docBuilder.parse(fis);

				} catch (SAXException | IOException e) {
					throw new ParseProcessException(
							"Could not process XML file '" + dataFile.getPath() + " : " + e.getMessage(), e);
				} finally {
					try {
						if (fis != null)
							fis.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				Set<String> keySet = xmlParser.getConfig().getPatterns().keySet();
				boolean found = false;
				for (String docCandidate : keySet) {
					if (found) break;
					String[] patterns = xmlParser.getConfig().getPatterns().get(docCandidate);
					for (String pattern : patterns) {
						if (found) break;
						XPathExpression xPathExpr;
						NodeList result = null;
						try {
							xPathExpr = xPath.compile(pattern);
														result = (NodeList) xPathExpr.evaluate(xmlDoc, XPathConstants.NODESET);
						} catch (XPathExpressionException e) {
							LogHelper.error("Error on XML pattern '" + pattern + "' : " + e.getMessage(), e);
							continue;
						}

						if (result.getLength() > 0) {
							// document matched

							// TODO : apply xslt if defined for this document

							DocumentConfiguration docConfig;
							try {
								docConfig = configuration.getDocumentConfiguration(docCandidate);
							} catch (ConfigurationException e) {
								LogHelper.error("Error on loading document configuration '" + docCandidate + "' : "
										+ e.getMessage(), e);
								continue;
							}
							Document doc = new Document();
							doc.getMetadata().put("inputFile", dataFile.getPath());
							doc.getMetadata().put("inputFileName", dataFile.getName());
							doc.getMetadata().put("docIdx", "1");
							doc.getMetadata().put("timestamp", String.valueOf(System.currentTimeMillis()));
							doc.getMetadata().put("inputFile", dataFile.getPath());
							doc.getMetadata().put("jobId", dataFile.getPath());
							doc.setNum(documents.size() + 1);
							doc.setName(docCandidate);
							doc.setOrigin(dataFile.getName());
							doc.setOriginType(ParserType.XML);
							doc.setId(dataFile.getName() + "_" + doc.getName() + "_" + doc.getNum());

							File tmpDataFile = File.createTempFile("data_", ".xml");
							// FileUtils.copyFile(dataFile, tmpDataFile);
							Utils.saveXML(xmlDoc, tmpDataFile);
							DatasourceConfiguration dsConfig = docConfig.getMainDatasource();
							Datasource mainDatasource = new Datasource(DatasourceType.XML, dsConfig.getName(),
									tmpDataFile);
							doc.setMainDatasource(mainDatasource);
							if (!mainDatasource.isSameDataFile(tmpDataFile))
								tmpDataFile.delete();

							applyDocumentFilter(tmpDataFile, doc);

							addExtraDocument(additionalDatasources, doc, docConfig);

							// TODO : is it supported for XML split ???
							// addCompanionDatasources(doc, additionalDataSources);

							if (StringUtils.isBlank(docConfig.getXmlSplitter())) {
								addOtherDatasources(doc);
							}

							documents.add(doc);

							LogHelper.info("New doc " + doc.getName() + ", " + (documents.size()));
							found = true;
						}

					} // loop xpath patterns

				} // loop xml doc patterns
			}
			break;
		}
		default: {
			Event currEvent = null;
			for (IParser<?, ?> parser : parsers) {
				LogHelper.info("Using parser type " + parser.getClass().getSimpleName());

				if (parser instanceof FieldParser && (dataType == ParserType.NONE || dataType == ParserType.FIELD)) {
					FieldParser fieldParser = (FieldParser) parser;
					Field currField = null;
					// FieldParser fParser = (FieldParser) parser;
					Scanner scanner = Utils.getLineScanner(dataFile, parser.getCharset());
					Document doc = null;
					while (scanner.hasNext()) {
						String line = StringUtils.removeStart(scanner.nextLine(), "\uFEFF");

						Event newEvent = null;
						try {
							newEvent = fieldParser.parseEvent(currEvent, line, scriptEngine);
						} catch (DataDescriptionException e1) {
							// TODO Auto-generated catch block
							LogHelper.error("Error while parsing '" + dataFile + "' : " + e1.getMessage(), e1);
						}
						if (newEvent != null) {
							LogHelper.info("New event " + newEvent.getName());
							dataType = ParserType.FIELD;
							closeDocument(currEvent, doc, additionalDataSources);

							currEvent = newEvent;

							doc = new Document();

							doc.setName(currEvent.getName());
							doc.setOrigin(dataFile.getName());
							doc.setOriginType(ParserType.FIELD);
							doc.getMetadata().put("inputFile", dataFile.getPath());
							doc.getMetadata().put("inputFileName", dataFile.getName());
							doc.getMetadata().put("docIdx", String.valueOf(documents.size() + 1));
							doc.getMetadata().put("timestamp", String.valueOf(System.currentTimeMillis()));
							doc.getMetadata().put("docType", doc.getName());
							doc.getMetadata().put("jobId", dataFile.getPath());
							doc.setNum(documents.size() + 1);
							doc.setId(dataFile.getName() + "_" + doc.getName() + "_" + doc.getNum());
							DocumentConfiguration docConfig;
							try {
								docConfig = configuration.getDocumentConfiguration(doc.getName());
							} catch (ConfigurationException e) {
								throw new ParseProcessException(
										"Could not parse '" + dataFile.getPath() + "' : " + e.getMessage(), e);
							}
							DatasourceConfiguration mainsDsConf = docConfig.getMainDatasource();

							Datasource mainDs = new Datasource(DatasourceType.XML, mainsDsConf.getName());
							doc.setMainDatasource(mainDs);

							for (DatasourceConfiguration dsConf : docConfig.getAdditionalDatasources()) {
								doc.getAdditionalDatasources().put(dsConf.getName(),
										new Datasource(DatasourceType.XML, dsConf.getName()));
							}

							if (docConfig.isAdditionalDocumentConfig()) {
								additionalDataSources.put(doc.getName(), doc);
							}

							documents.add(doc);

							LogHelper.info("New doc " + doc.getName() + ", " + (documents.size()));

						} else if (currEvent != null) {

							try {
								Field newField = fieldParser.parseFields(currEvent, currField, line, scriptEngine);
								if (newField != null) {
									if (currField != newField) {
										if (newField != null)
											currEvent.insertField(newField);
										currField = newField;
									}

								} else {

								}

							} catch (DataDescriptionException e) {
								// TODO Auto-generated catch block
								LogHelper.error("Could not parse " + dataFile + " : " + e.getMessage(), e);
							}
						}
					}
					scanner.close();
					// Dernier event
					closeDocument(currEvent, doc, additionalDataSources);
				} else if (parser instanceof RecordParser
						&& (dataType == ParserType.NONE || dataType == ParserType.RECORD)) {
					RecordParser recordParser = (RecordParser) parser;
					Scanner scanner = Utils.getLineScanner(dataFile, parser.getCharset());
					Document doc = null;
					while (scanner.hasNext()) {
						String line = StringUtils.removeStart(scanner.nextLine(), "\uFEFF");
						Event newEvent = null;
						try {
							newEvent = recordParser.parseEvent(currEvent, line, scriptEngine);

						} catch (DataDescriptionException e1) {
							// TODO Auto-generated catch block
							LogHelper.error("Error while parsing " + dataFile + " : " + e1.getMessage(), e1);
						}

						if (newEvent != null) {
							dataType = ParserType.RECORD;
							closeDocument(currEvent, doc, additionalDataSources);

							currEvent = newEvent;
							doc = new Document();
							doc.setName(currEvent.getName());
							doc.setOrigin(dataFile.getName());
							doc.setOriginType(ParserType.RECORD);
							doc.getMetadata().put("inputFile", dataFile.getPath());
							doc.getMetadata().put("inputFileName", dataFile.getName());
							doc.getMetadata().put("docNum", String.valueOf(documents.size() + 1));
							doc.getMetadata().put("timestamp", String.valueOf(System.currentTimeMillis()));
							doc.getMetadata().put("docType", doc.getName());
							doc.getMetadata().put("jobId", dataFile.getPath());
							doc.setNum(documents.size() + 1);
							doc.setId(dataFile.getName() + "_" + doc.getName() + "_" + doc.getNum());
							DocumentConfiguration docConfig;
							try {
								docConfig = configuration.getDocumentConfiguration(doc.getName());
							} catch (ConfigurationException e) {
								throw new ParseProcessException(
										"Could not parse '" + dataFile.getPath() + "' : " + e.getMessage(), e);
							}
							DatasourceConfiguration mainsDsConf = docConfig.getMainDatasource();

							Datasource mainDs = new Datasource(DatasourceType.XML, mainsDsConf.getName());
							doc.setMainDatasource(mainDs);

							for (DatasourceConfiguration dsConf : docConfig.getAdditionalDatasources()) {
								doc.getAdditionalDatasources().put(dsConf.getName(),
										new Datasource(DatasourceType.XML, dsConf.getName()));
							}

							if (docConfig.isAdditionalDocumentConfig()) {
								additionalDataSources.put(doc.getName(), doc);
							}

							documents.add(doc);

							LogHelper.info("New doc " + doc.getName() + ", " + (documents.size()));

						} else if (currEvent != null) {
							try {
								Record newRecord = recordParser.parseFields(currEvent, currEvent, line, scriptEngine);
								if (newRecord != null) {
									currEvent.insertRecord(newRecord);
								}
							} catch (DataDescriptionException e) {
								// TODO Auto-generated catch block
								LogHelper.error("Error while parsing " + dataFile + " : " + e.getMessage(), e);

							}

						}
					}
					scanner.close();
					// Dernier event
					closeDocument(currEvent, doc, additionalDataSources);
				} else if (parser instanceof R3rdiParser
						&& (dataType == ParserType.NONE || dataType == ParserType.R3RDI)) {
					R3rdiParser r3rdiParser = (R3rdiParser) parser;
					Scanner scanner = Utils.getLineScanner(dataFile, parser.getCharset());
					Document doc = null;
					// int lineNum = 0;
					while (scanner.hasNext()) {
						String line = StringUtils.removeStart(scanner.nextLine(), "\uFEFF");
						// lineNum++;
						// System.out.println("LINE " + lineNum + " : " + line);
						Event newEvent = null;
						try {
							newEvent = r3rdiParser.parseEvent(currEvent, line, scriptEngine);

							// System.out.println("newEvent:" + newEvent);
						} catch (DataDescriptionException e1) {
							LogHelper.error("Error while parsing " + dataFile + " : " + e1.getMessage(), e1);

						}
						if (newEvent != null) {
							dataType = ParserType.R3RDI;

							closeDocument(currEvent, doc, additionalDataSources);

							currEvent = newEvent;
							doc = new Document();
							doc.setName(currEvent.getName());
							doc.setOrigin(dataFile.getName());
							doc.setOriginType(ParserType.R3RDI);
							doc.getMetadata().put("inputFile", dataFile.getPath());
							doc.getMetadata().put("inputFileName", dataFile.getName());
							doc.getMetadata().put("docNum", String.valueOf(documents.size() + 1));
							doc.setNum(documents.size() + 1);
							doc.getMetadata().put("timestamp", String.valueOf(System.currentTimeMillis()));
							doc.getMetadata().put("docType", doc.getName());
							doc.getMetadata().put("jobId", dataFile.getPath());
							doc.setId(dataFile.getName() + "_" + doc.getName() + "_" + doc.getNum());
							DocumentConfiguration docConfig;
							try {
								docConfig = configuration.getDocumentConfiguration(doc.getName());
							} catch (ConfigurationException e) {
								throw new ParseProcessException(
										"Could not parse '" + dataFile.getPath() + "' : " + e.getMessage(), e);
							}
							DatasourceConfiguration mainsDsConf = docConfig.getMainDatasource();

							Datasource mainDs = new Datasource(DatasourceType.XML, mainsDsConf.getName());
							doc.setMainDatasource(mainDs);

							for (DatasourceConfiguration dsConf : docConfig.getAdditionalDatasources()) {
								doc.getAdditionalDatasources().put(dsConf.getName(),
										new Datasource(DatasourceType.XML, dsConf.getName()));
							}

							if (docConfig.isAdditionalDocumentConfig()) {
								additionalDataSources.put(doc.getName(), doc);
							}

							documents.add(doc);

							LogHelper.info("New doc " + doc.getName() + ", " + (documents.size()));
						} else if (currEvent != null) {
							try {
								Record newRecord = r3rdiParser.parseFields(currEvent, currEvent, line, scriptEngine);
								if (newRecord != null) {

									currEvent.insertRecord(newRecord);

								}
							} catch (DataDescriptionException e) {
								LogHelper.error("Error while parsing " + dataFile + " : " + e.getMessage(), e);

							}

						}

					}
					scanner.close();
					// Dernier event
					closeDocument(currEvent, doc, additionalDataSources);
					// break;
				}

			}
			if (documents.size() == 0)
			{//Parsers de type PageIn (lorsque tout a échoué...)
			
				
				Map<String, List<Document>> additionalDatasources = new HashMap<String, List<Document>>();
				PageInProcess pageInProcess = new PageInProcess(configuration);
				pageInProcess.addDocumentDefinitions(pageInParsers);
				
					List<Event> events;
					try {
						events = pageInProcess.parse(dataFile);
					} catch (IOException | FileProcessException | PageSettingsException | DocumentGeneratorException
							| DataSourceException e) {
						throw new ParseProcessException("Could not parse OTF file '" + dataFile.getPath() + "' : " + e.getMessage(), e);
					}
					Document doc = null;
					for (Event event : events)
					{
						
							try {
								DocumentConfiguration docConfig = configuration.getDocumentConfiguration(event.getName());
								
								doc = new Document();
								
								doc.getMetadata().put("inputFile", dataFile.getPath());
								doc.getMetadata().put("inputFileName", dataFile.getName());
								doc.getMetadata().put("docIdx", "1");
								doc.getMetadata().put("timestamp", String.valueOf(System.currentTimeMillis()));
								doc.getMetadata().put("inputFile", dataFile.getPath());
								doc.getMetadata().put("jobId", dataFile.getPath());
								doc.setNum(documents.size() + 1);
								doc.setName(event.getName());
								doc.setOrigin(dataFile.getName());
								doc.setOriginType(ParserType.PAGE);
								doc.setId(dataFile.getName() + "_" + doc.getName() + "_" + doc.getNum());
								
								DatasourceConfiguration mainsDsConf = docConfig.getMainDatasource();

								Datasource mainDs = new Datasource(DatasourceType.XML, mainsDsConf.getName());
								doc.setMainDatasource(mainDs);
								

								for (DatasourceConfiguration dsConf : docConfig.getAdditionalDatasources()) {
									doc.getAdditionalDatasources().put(dsConf.getName(),
											new Datasource(DatasourceType.XML, dsConf.getName()));
								}

								if (docConfig.isAdditionalDocumentConfig()) {
									additionalDataSources.put(doc.getName(), doc);
								}

							

								
								addExtraDocument(additionalDatasources, doc, docConfig);
								addOtherDatasources(doc);
								closeDocument(event, doc, additionalDataSources);
								LogHelper.info("New doc " + doc.getName() + ", " + (documents.size()));

								documents.add(doc);
							} catch (ConfigurationException e) {
								LogHelper.error("Could not process OTF document '" + doc.getName() + ", " + doc.getOrigin() + " : "
										+ e.getMessage(), e);
								
								continue;
							}
						
						
					}
				
				
			}

		}
		}

		if (originalDataFile != dataFile)
			dataFile.delete();

		return documents;

	}

	/**
	 * @param additionalDatasources
	 * @param doc
	 * @param docConfiguration
	 * @return
	 * @throws ParseProcessException
	 * @throws ConfigurationException
	 * @throws IOException
	 * @throws JdbcException
	 * @throws ScriptException
	 * @throws TransformerException
	 * @throws DataSourceException
	 * @throws @throws                UnsupportedEncodingException
	 */
	protected void addExtraDocument(Map<String, List<Document>> additionalDatasources, Document doc,
			DocumentConfiguration docConfiguration) throws ParseProcessException, UnsupportedEncodingException,
			IOException, fr.utils.configuration.ConfigurationException, JdbcException, ScriptException,
			TransformerException, DataSourceException {

		List<String> additionalFilePatterns = docConfiguration.getAdditionalDocumentFiles();
		if (additionalFilePatterns == null)
			return;
		for (String additionalFilePattern : additionalFilePatterns) {

			if (StringUtils.isNotBlank(additionalFilePattern))// && (docConfiguration.getAdditionalDatasources().size()
																// > 0 || docConfiguration.isPreformattedDocument()))
			{// TODO : context freemarker -> évaluer additional file -> parser le fichier ->
				// si le document fait partie des datasources additionnelles configurées, mettre
				// le xml du fichier dans les data sources additionnelles du PDF
				Map<String, Object> context = new HashMap<String, Object>();
				context.putAll(doc.getMetadata());
				context.put("origin", doc.getOrigin());
				context.put("originFileName", FilenameUtils.getName(doc.getOrigin()));
				context.put("originBaseName", FilenameUtils.getBaseName(doc.getOrigin()));
				context.put("originExtension", FilenameUtils.getExtension(doc.getOrigin()));
				for (Entry<String, String> entry : doc.getMetadata().entrySet()) {
					context.put(StringUtils.removeStart(entry.getKey(), "_"), entry.getValue());
				}
				context.put("name", doc.getName());

				String fileName = FreeMarkerHelper.parseExpression(additionalFilePattern,
						doc.getName() + "_additionalFile", context);
				File additionalFile = new File(fileName);

				List<Document> additionalDocuments = additionalDatasources.get(additionalFile.getPath());
				if (additionalDocuments == null) {
					additionalDocuments = this.parse(additionalFile);
					additionalDatasources.put(additionalFile.getPath(), additionalDocuments);
				}
				for (Document extraDoc : additionalDocuments) {
					Datasource mainDSOfExtraDoc = extraDoc.getMainDatasource();
					if (StringUtils.isBlank(mainDSOfExtraDoc.getName()))
						mainDSOfExtraDoc.setName(extraDoc.getName());

					if (docConfiguration.isAdditionalDatasource(extraDoc.getName())) {
						doc.getAdditionalDatasources().put(mainDSOfExtraDoc.getName(), mainDSOfExtraDoc);
					} else {
						doc.getAdditionalContexts().put(mainDSOfExtraDoc.getName(), mainDSOfExtraDoc);
					}
				}
			}
		}

	}

	protected IParser<?, ?> loadParser(File descriptionFile) throws DataDescriptionException {

		if (descriptionFile == null)
			return null;

		while (Utils.isFileInProgess(descriptionFile)) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		LogHelper.info("Loading description" + descriptionFile);
		File f = descriptionFile;
		if (!f.exists())
			throw new DataDescriptionException("Description file '" + descriptionFile + "' not found");

		ParserType type = getParserType(f);

		switch (type) {
		case RECORD: {
			RecordParserConfiguration parserConfig = new RecordParserConfiguration();
			try {
				parserConfig.load(f, configuration);
			} catch (IOException e) {
				throw new DataDescriptionException("Could not load " + f.getPath() + " : " + e.getMessage(), e);
			}
			RecordParser parser = new RecordParser(parserConfig);
			if (parserConfig.getCharset() != null)
				parser.setCharset(parserConfig.getCharset());
			else
				parser.setCharset(configuration.getEncoding());

			return parser;
		}
		case FIELD: {
			FieldParserConfiguration parserConfig = new FieldParserConfiguration();
			try {
				parserConfig.load(f, this.configuration);
			} catch (IOException e) {
				throw new DataDescriptionException("Could not load " + f.getPath() + " : " + e.getMessage(), e);
			}
			FieldParser parser = new FieldParser(parserConfig);

			if (parserConfig.getCharset() != null)
				parser.setCharset(parserConfig.getCharset());
			else
				parser.setCharset(configuration.getEncoding());
			return parser;
		}
		case R3RDI: {
			R3rdiParserConfiguration parserConfig = new R3rdiParserConfiguration();
			try {
				parserConfig.load(f, configuration);
			} catch (IOException e) {
				throw new DataDescriptionException("Could not load " + f.getPath() + " : " + e.getMessage(), e);
			}
			R3rdiParser parser = new R3rdiParser(parserConfig);
			if (parserConfig.getCharset() != null) {
				parser.setCharset(parserConfig.getCharset());
			} else {
				parser.setCharset(configuration.getEncoding());
			}

			return parser;
		}
		case XML: {
			XmlParserConfiguration parserConfig = new XmlParserConfiguration();
			try {
				parserConfig.load(f, configuration);
			} catch (IOException e) {
				throw new DataDescriptionException("Could not load " + f.getPath() + " : " + e.getMessage(), e);
			}
			XmlParser parser = new XmlParser(parserConfig);

			return parser;
		}
		case NONE:
		default:
			throw new DataDescriptionException(
					"Description file '" + descriptionFile + "' not supported (unknown type)");

		}

		// throw new DataDescriptionException("Description file '" + idr3OrFile + "' not
		// loaded, unexpected case.");
	}

	
	
}
