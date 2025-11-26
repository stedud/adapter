package fr.kw.adapter.engine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import de.kwsoft.moms.xf.XfXml2ControlJob;
import de.kwsoft.mtext.api.MTextException;
import de.kwsoft.mtext.mffmfd.PageInfo;
import fr.freemarker.FreeMarkerException;
import fr.kw.adapter.document.Datasource;
import fr.kw.adapter.document.DatasourceType;
import fr.kw.adapter.document.Document;
import fr.kw.adapter.document.DocumentActions;
import fr.kw.adapter.document.configuration.DocumentConfiguration;
import fr.kw.adapter.engine.purge.PurgeSettings;
import fr.kw.adapter.engine.purge.PurgeTask;
import fr.kw.adapter.parser.DataDescriptionException;
import fr.kw.adapter.parser.jdbc.JdbcException;
import fr.kw.adapter.parser.process.FileFilter;
import fr.kw.adapter.parser.process.FilterType;
import fr.kw.adapter.parser.process.ParseProcess;
import fr.kw.adapter.parser.process.ParseProcessException;
import fr.kw.adapter.parser.script.ScriptException;
import fr.kw.adapter.parser.type.ParserType;
import fr.kw.adapter.parser.type.page.pdfin.PDFProcess;
import fr.kw.adapter.parser.type.page.settings.PageSettingsException;
import fr.kw.api.submit.Status;
import fr.kw.api.submit.Submit;
import fr.kw.api.submit.SubmitConfiguration;
import fr.kw.api.submit.xml.splitter.XMLSplitter;
import fr.utils.LogHelper;
import fr.utils.Utils;
import fr.utils.Threading.ExecutorException;
import fr.utils.configuration.BaseConfiguration;
import fr.utils.configuration.ConfigurationException;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class ProcessFile implements Callable<List<Document>> {

	protected File errorFolder = new File(Submit.BAK_FOLDER, Submit.ERRORS_FOLDER);

	protected File bakFolder = new File(Submit.BAK_FOLDER);

	@Option(names = { "-backup", "-doBackup" })
	protected boolean backup = false;

	@Option(names = { "-in", "-input", "-fileName" }, required = false)
	protected File inputFile;
	
	@Option(names = { "-name" }, required = false)
	protected String name;

	@Option(names = { "-conf", "-config" }, required = true)
	protected File configurationFile;

	@Option(names = { "-baseConf", "-baseConfig", "-KWConfig", "-KWConf" }, required = false)
	protected File rootConfigurationFile;

	@Option(names = { "-noRefresh" })
	protected Boolean noRefreshSettings = false;
	
	@Option(names = { "-noPurge" })
	protected Boolean noPurge = false;
	
	@Option(names = { "-refreshOnly" })
	protected Boolean refreshOnly = false;

//	//Scanner configuration is not used as a scanner, but the document will be processed as if the scanner found the file
//	@Option(names= {"-scanConfig"}, required = true)
//	protected File scanConfigurationFile;

	@Option(names = { "-parseOnly", "-parse" })
	protected boolean parseOnly = false;

	@Option(names = "-documentThreads")
	protected int nbDocumentThreads = 1;
	@Option(names = "-pdfDirect")
	protected boolean pdfDirect = false;

	@Option(names = "-pdfScale")
	protected double pdfScale = 1.0;

	@Option(names = { "-pdfControlJob" }, required = false)
	protected File controlJobFile = null;

	private SubmitConfiguration configuration;

	private BaseConfiguration rootConfiguration;

	@Option(names = { "-keepParsed" })
	private boolean keepParsed = false;

	private static boolean daemon = true;

	public ProcessFile() {
		

	}

	public static void main(String[] args) {
		String tmp = System.getProperty("java.io.tmpdir");
		if (StringUtils.isNotBlank(tmp)) {
			File tmpFolder = new File(tmp);
			tmpFolder.mkdirs();
		}

		ProcessFile start = new ProcessFile();
		ProcessFile.setDaemon(false);
		CommandLine cmdLine = new CommandLine(start);

		int result = cmdLine.execute(args);
		

		ThreadManager.stopAll();
		System.out.println("ProcessFile exiting with status " + result);

		System.exit(result);
	}

	@Override
	public List<Document> call() throws Exception {
		File filterResult = null;
		List<Document> documents = null;
		List<Document> toAdd = new ArrayList<Document>();
		try {
			long start = System.currentTimeMillis();

			if (configuration == null)
				configuration =  SubmitConfiguration.get(configurationFile);
			if (rootConfiguration == null) {

				if (rootConfigurationFile != null)
					rootConfiguration = new BaseConfiguration(rootConfigurationFile);
				else {
					rootConfiguration = configuration.getMainConfiguration();

				}
			}
			if (rootConfiguration == null)
				throw new ConfigurationException("Main configuration not found");

			this.rootConfigurationFile = rootConfiguration.getPropertiesFile();
			
			if (!noRefreshSettings && !ProcessFile.isDaemon()) {
				if (StringUtils.isNotBlank(rootConfiguration.get("KW_SYNCHRO_SETTINGS_PROJECT"))) {
					KWFilesSynchronizer kwSynchronizer = new KWFilesSynchronizer("kwSettingsSynchro",
							rootConfiguration);
					kwSynchronizer.kwSynchronize();
					configuration.refresh();
				}
				if (StringUtils.isNotBlank(rootConfiguration.get("SYNCHRO_SETTINGS_FOLDER"))) {
					LocalFilesSynchronizer localSynchronizer = new LocalFilesSynchronizer("localSettingsSynchro",
							rootConfiguration);
					localSynchronizer.synchronize();
					configuration.refresh();
				}
			}
			
			if (refreshOnly) return documents;

			if (!StringUtils.isBlank(configuration.get("BAK_FOLDER"))) {
				bakFolder = new File(configuration.get("BAK_FOLDER"),
						FilenameUtils.getBaseName(configuration.getPropertiesFile().getName()));
				if (!StringUtils.isBlank(configuration.get("ERROR_FOLDER"))) {
					errorFolder = new File(configuration.get("ERROR_FOLDER"));
				} else {
					errorFolder = new File(bakFolder, "errors");
				}
			} else {
				if (!StringUtils.isBlank(configuration.get("ERROR_FOLDER"))) {
					errorFolder = new File(configuration.get("ERROR_FOLDER"));
				}
			}
			
		File originalInputFile = inputFile;
			
			if (this.inputFile == null)
			{
				inputFile = new File(Utils.getTmpFolder(), name);
				inputFile.deleteOnExit();
				originalInputFile = inputFile;
					
				FileOutputStream fos = new FileOutputStream(inputFile);
				IOUtils.copy(System.in, fos);
				IOUtils.closeQuietly(fos);
				
				LogHelper.info("STDIN data saved to " + inputFile);
				
			}
			else
			{
				File tmpFile = new File(Utils.getTmpFolder(), inputFile.getName());
				FileUtils.copyFile(inputFile, tmpFile,false);
				inputFile = tmpFile;
				inputFile.setLastModified(System.currentTimeMillis());
			}

			if (backup) {

				bakFolder.mkdirs();
				
					File backupFile = new File(bakFolder, this.inputFile.getName());
					if (backupFile.exists())
						backupFile.delete();
					FileUtils.copyFile(this.inputFile, backupFile, false);
					LogHelper.info("Input file " + inputFile + " saved to " + backupFile);

			}



			if (!ProcessFile.isDaemon()) {
				if (! noPurge)
				{
					List<PurgeSettings> purgeSettings = configuration.getPurgeSettings();
					{
						for (PurgeSettings settings : purgeSettings) {
							PurgeTask purgeTask = new PurgeTask(settings);
							Thread t = new Thread(purgeTask);
							
							t.run();// Lancment des purges en background dans un thread séparé
						}
					}
				}
			}

			if (configuration.isParseOnly())
				this.parseOnly = true;
			if (configuration.isKeepParsed())
				this.keepParsed = true;

//			//charger les settigns de scan après la synchro
//			if (scanConfigurationFile != null)
//			{
//				ScannerConfiguration scanConfig = new ScannerConfiguration(scanConfigurationFile);
//				FileConfiguration current = configuration;
//				while (configuration.getParent() != null)
//				{
//					current = configuration.getParent();
//				}
//				current.setParent(scanConfig);	
//			}
//			
			ParseProcess parser = new ParseProcess(configuration);
			
			if (StringUtils.isNotBlank(configuration.getFilterType())) {
				
				String filterType = configuration.getFilterType().toUpperCase();
				FilterType type = FilterType.NONE;
				try {
					type = FilterType.valueOf(filterType);
				} catch (Exception e) {
					LogHelper.error("Invalid value for FILTER_TYPE:" + filterType + " (allowed : JDE, COMMAND)");
					throw new ParseProcessException(
							"Invalid value for FILTER_TYPE:" + filterType + " (allowed : JDE, COMMAND)");
				}

				FileFilter fileFilter = new FileFilter();
				fileFilter.setType(type);
				fileFilter.setArguments(configuration.get("FILTER_ARGUMENTS", null));
				fileFilter.setBackFolder(configuration.get("FILTER_BACKUP_FOLDER", null));
				fileFilter.setResultExtension(configuration.get("FILTER_RESULT_EXTENSION", null));

				LogHelper.info("Applying filter " + fileFilter + " to " + inputFile.getPath());
				filterResult = fileFilter.filter(inputFile);
				//inputFile.delete();
				inputFile = filterResult;

			}
			documents = null;
			toAdd = new ArrayList<Document>();

			if (controlJobFile != null && pdfDirect) {

				if (pdfScale != 1.0) {
					LogHelper.info("Scaling PDF (" + pdfScale + ")");
					
					PDFProcess.scalePDF(inputFile, pdfScale);
				}

				Document pdfDoc = new Document();

				// TODO : si pdfScale != 1.0 : mettre les pages du pdf à l'échelle demandée

				Datasource pdfDatasource = new Datasource(DatasourceType.MFD);
				pdfDatasource.setName(FilenameUtils.getBaseName(inputFile.getName()));
				File tmpFolder = Utils.getTmpFolder();
				tmpFolder.mkdirs();
				File mfdFile = new File(tmpFolder, FilenameUtils.getBaseName(inputFile.getName()) + ".mfd");
				// mfdFile.deleteOnExit();

				boolean renamed = false;
				File pdfForMfd = null;
				String originalFileName = inputFile.getName();
				String extension = FilenameUtils.getExtension(inputFile.getName());
				if ("pdf".compareToIgnoreCase(extension) != 0) {
					renamed = true;
					String nameWithExtension = inputFile.getName() + ".pdf";
					pdfForMfd = new File(inputFile.getParent(), nameWithExtension);
					inputFile.renameTo(pdfForMfd);
					LogHelper.info(inputFile + " renamed to " + pdfForMfd);

				} else {
					pdfForMfd = inputFile;
				}
				List<PageInfo> pageInfo = PDFProcess.pdf2mfd(pdfForMfd, mfdFile);
				pdfDatasource.writeData(mfdFile);
				if (!pdfDatasource.isSameDataFile(mfdFile))
					mfdFile.delete();

				if (renamed) {
					File originalFile = new File(inputFile.getParent(), originalFileName);
					pdfForMfd.renameTo(originalFile);
					LogHelper.info(pdfForMfd + " renamed to " + originalFile);
					inputFile = originalFile;
				}

				pdfDoc.setMainDatasource(pdfDatasource);
				pdfDoc.setId(originalInputFile.getPath());
				pdfDoc.setPageInfos(pageInfo);

				pdfDoc.setNum(1);
				pdfDoc.setName(originalInputFile.getName());
				pdfDoc.setOrigin("N/A");
				pdfDoc.setOriginType(ParserType.PAGE_PDF);

				DocumentActions docActions = new DocumentActions();
				docActions.setPrinter("OMS");
				docActions.setSplitted(false);
				LogHelper.info(originalInputFile + " using Control Job File " + controlJobFile);
				docActions.setControlJob(controlJobFile.getName());
				docActions.setControlJobFile(controlJobFile);
				pdfDoc.setDocumentActions(docActions);

				File mcjFile = new File(controlJobFile.getParent(),
						FilenameUtils.getBaseName(controlJobFile.getName()) + ".mcj");
				mcjFile.deleteOnExit();
				XfXml2ControlJob.convert(false, controlJobFile, pdfDoc.getPageInfos(), mcjFile);// Création fichier MCJ

				documents = new ArrayList<Document>();
				LogHelper.info(originalInputFile + " added to document list for MOMS Print");
				documents.add(pdfDoc);

			} else {
				documents = parser.parse(inputFile);

				parser.clear();

				XMLSplitter splitter = null;
				for (Document doc : documents) {
					try {
						DocumentConfiguration docConfig = configuration.getDocumentConfiguration(doc.getName());
						if (StringUtils.isNotBlank(docConfig.getXmlSplitter())) {
							DocumentActions actions = doc.getDocumentActions();
							if (actions == null) {
								actions = docConfig.createDocumentActions(doc);
								doc.setDocumentActions(actions);
							}
							if (splitter == null)
								splitter = new XMLSplitter(configuration);
							LogHelper.info("Splitting " + doc.getOrigin() + " with " + actions.getXmlSplitter());
							List<Datasource> splitDatasources = splitter.split(doc.getMainDatasource(),
									actions.getXmlSplitter());
							LogHelper.info("Document " + doc.getOrigin() + " splitted into " + splitDatasources.size()
									+ " files by " + actions.getXmlSplitter());
							if (splitDatasources.size() > 0) {

								
								for (int i = 1; i <= splitDatasources.size(); i++) {
									//TODO : reanalyse XML 
									//IF same document name : like before
									//OTherwise apply document settings
									Datasource ds = splitDatasources.get(i-1);
									
									File xmlFile = new File(Utils.getTmpFolder(), doc.getId() + "." + String.format("%07d", i));
									Utils.copyInputStreamToFile(ds.getDataInputStreamToClose(), xmlFile);
									
									boolean reparsed = false;
									List<Document> xmlDocuments = parser.parse(xmlFile);
									for (Document xmlDoc : xmlDocuments)
									{

											DocumentConfiguration subDocConfig = configuration.getDocumentConfiguration(xmlDoc.getName());
											xmlDoc.setOrigin(doc.getOrigin());
											xmlDoc.setNum(i);
											
											xmlDoc.setEnvironment(subDocConfig.getEnvironment());
											xmlDoc.setId(doc.getId() + "." + xmlDoc.getName() + "." + String.format("%07d", i));
											
											xmlDoc.setOriginType(doc.getOriginType());
											xmlDoc.setDocumentActions(subDocConfig.createDocumentActions(xmlDoc));
											xmlDoc.getDocumentActions().setSplitted(true);
											ds.setName(subDocConfig.getMainDatasource().getName());
											xmlDoc.setMainDatasource(ds);
											LogHelper.info("Split document " + xmlDoc.getId() + " analysed.");
											toAdd.add(xmlDoc);
											parser.addOtherDatasources(xmlDoc);
											reparsed = true;
										
									}
								
									if (! reparsed)
									{
										Document splitDoc = new Document();
										splitDoc.setEnvironment(docConfig.getEnvironment());
										splitDoc.setId(doc.getId() + "." + String.format("%07d", i));
										splitDoc.setName(doc.getName());
										splitDoc.setOrigin(doc.getOrigin());
										splitDoc.setOriginType(doc.getOriginType());
										splitDoc.setNum(i);
										
										ds.setName(docConfig.getMainDatasource().getName());
										splitDoc.setMainDatasource(ds);
										splitDoc.setDocumentActions(docConfig.createDocumentActions(splitDoc));
										LogHelper.info("Split document " + splitDoc.getId() + " analysed.");
										splitDoc.getDocumentActions().setSplitted(true);
										toAdd.add(splitDoc);
										parser.addOtherDatasources(splitDoc);
									}

								}
							}
							doc.getDocumentActions().setPrinterStatus(Status.IGNORE);
							doc.getDocumentActions().setReturnStatus(Status.IGNORE);
							doc.getDocumentActions().setTonicStatus(Status.IGNORE);
							doc.getDocumentActions().setSplitted(true);
						}
					} catch (ConfigurationException | FreeMarkerException | IllegalArgumentException
							| MTextException e) {
						doc.getDocumentActions().getExceptions().add(e);
						doc.getDocumentActions().getErrorMessages()
								.add("Error occured during Splitting : " + e.getMessage());
						if (doc.getDocumentActions().isPrint())
							doc.getDocumentActions().setPrinterStatus(Status.KO);
						if (doc.getDocumentActions().isResponse())
							doc.getDocumentActions().setReturnStatus(Status.KO);
						if (doc.getDocumentActions().isSaveInTonic())
							doc.getDocumentActions().setTonicStatus(Status.KO);

					}

				}
			}

			documents.addAll(toAdd);
			
			if (parseOnly || keepParsed) {
				LogHelper.info("Saving documents information...");
				File parseResultFolder = this.bakFolder;

				parseResultFolder.mkdirs();
				for (Document doc : documents) {
					File destination = new File(parseResultFolder, Utils.normalizeXmlName(doc.getName()));
					destination = new File(destination, Utils.normalizeXmlName(FilenameUtils.getName(doc.getId())));
					LogHelper.info("Saving document " + doc.getId() + " to " + destination);
					Utils.saveDocument(doc, configuration.getDocumentConfiguration(doc.getName()), destination);

				}
				LogHelper.info("All documents information saved.");
			}

			if (!parseOnly) {
				LogHelper.info("Submitting documents to KW...");
				Submit submitDocuments = new Submit(configuration);
				submitDocuments.setErrorFolder(getErrorFolder());
				submitDocuments.submit(documents);

			}
			LogHelper.info("PARSE_ONLY=" + parseOnly);
			LogHelper.info("KEEP_PARSED=" + keepParsed);
			
			

			long stop = System.currentTimeMillis();
			Duration duration = Duration.ofMillis(stop - start);

			LogHelper.info("Processed " + documents.size() + " documents in " + duration);

			if (pdfDirect) {
				boolean ok = true;
				for (Document doc : documents) {
					if (doc.getDocumentActions().getGlobalStatus() != Status.OK) {
						throw new ExecutorException("Error when submitting PDF");
					}
				}

			}

			return documents;
		} catch (ConfigurationException | DataDescriptionException | PageSettingsException | ParseProcessException
				| IOException | JdbcException | ScriptException | ExecutorException | FreeMarkerException
				| ParserConfigurationException | TransformerException e) {
			LogHelper.error(e.getMessage(), e);
			throw e;
		} finally {
			
			if (documents != null)
				for (Document doc : documents) {
					doc.clean();

				}
			if (toAdd != null)
				for (Document doc : toAdd) {
					doc.clean();
				}
			if (filterResult != null)
			{
				if (filterResult.exists())
				{
					filterResult.delete();
				}
				
			}
			if (inputFile != null)	inputFile.delete();//inputFile a été remplacé par une copie dans un dossier temporaire

		}
	}

	public File getInputFile() {
		return inputFile;
	}

	public void setInputFile(File inputFile) {
		this.inputFile = inputFile;
	}

	public File getConfigurationFile() {
		return configurationFile;
	}

	public void setConfigurationFile(File configurationFile) {
		this.configurationFile = configurationFile;
	}

	public boolean isParseOnly() {
		return parseOnly;
	}

	public void setParseOnly(boolean parseOnly) {
		this.parseOnly = parseOnly;
	}

	public File getErrorFolder() {
		return errorFolder;
	}

	public void setErrorFolder(File errorFolder) {
		this.errorFolder = errorFolder;
	}

	public boolean isKeepParsed() {
		return keepParsed;
	}

	public void setKeepParsed(boolean keepParsed) {
		this.keepParsed = keepParsed;
	}

	public static boolean isDaemon() {
		return daemon;
	}

	public static void setDaemon(boolean daemon) {
		ProcessFile.daemon = daemon;
	}

	public BaseConfiguration getRootConfiguration() {
		return rootConfiguration;
	}

	public void setRootConfiguration(BaseConfiguration rootConfiguration) {
		this.rootConfiguration = rootConfiguration;
	}

}
