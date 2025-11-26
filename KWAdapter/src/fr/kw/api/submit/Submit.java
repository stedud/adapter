package fr.kw.api.submit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.InputSource;

import de.kwsoft.moms.xf.XfException;
import de.kwsoft.moms.xf.XfXml2ControlJob;
import de.kwsoft.mtext.mffmfd.DocumentGeneratorException;
import de.kwsoft.mtext.tools.BatchProcessor;
import de.kwsoft.mtext.tools.batchprocessor.api.BatchProcessorConfigurator;
import de.kwsoft.mtext.tools.batchprocessor.api.BatchProcessorException;
import fr.freemarker.FreeMarkerException;
import fr.freemarker.FreeMarkerHelper;
import fr.kw.adapter.document.DataSourceException;
import fr.kw.adapter.document.Datasource;
import fr.kw.adapter.document.Document;
import fr.kw.adapter.document.DocumentActions;
import fr.kw.adapter.document.configuration.DocumentConfiguration;
import fr.kw.adapter.engine.ThreadManager;
import fr.kw.api.rest.moms.MomsClientAPI;
import fr.kw.api.rest.moms.search.LogicalOperator;
import fr.kw.api.rest.moms.search.Operator;
import fr.kw.api.rest.moms.search.SearchParameter;
import fr.kw.api.rest.mtext.JobExecutionStrategy;
import fr.kw.api.rest.mtext.MtextClientAPI;
import fr.kw.api.rest.mtext.Response;
import fr.utils.LogHelper;
import fr.utils.Pair;
import fr.utils.Utils;
import fr.utils.Threading.ExecutorException;
import fr.utils.Threading.RunnableExecutor;
import fr.utils.Threading.Task;

public class Submit {

	public static String ERRORS_FOLDER = "errors";
	public static String BAK_FOLDER = "bak";
	protected File errorFolder = new File(BAK_FOLDER, ERRORS_FOLDER);

	private final class LongComparator implements Comparator<Long> {
		@Override
		public int compare(Long o1, Long o2) {

			return Long.compare(o1, o2);
		}
	}

	private SubmitConfiguration globalConfiguration;
	private MomsClientAPI momsClient = null;
	private MtextClientAPI mtextClient = null;

	public Submit(SubmitConfiguration configuration) throws ExecutorException {
		this.globalConfiguration = configuration;
		if (!StringUtils.isBlank(configuration.get("BAK_FOLDER"))) {
			BAK_FOLDER = configuration.get("BAK_FOLDER");
			if (!StringUtils.isBlank(configuration.get("ERROR_FOLDER"))) {
				ERRORS_FOLDER = configuration.get("ERROR_FOLDER");
			}
		} else {
			if (!StringUtils.isBlank(configuration.get("ERROR_FOLDER"))) {
				ERRORS_FOLDER = configuration.get("ERROR_FOLDER");
			}
		}
		errorFolder = new File(BAK_FOLDER, ERRORS_FOLDER);

	}

	public void submit(List<Document> documents) {

		synchronized (this) {
			RunnableExecutor executor = ThreadManager.geThreadExecutor(ThreadManager.DOCUMENTS_THREAD_POOL);
			if (executor == null || executor.getNbThreads() != globalConfiguration.getDocumentThreadPoolSize(1)) {
				try {
					ThreadManager.init(ThreadManager.DOCUMENTS_THREAD_POOL,
							globalConfiguration.getDocumentThreadPoolSize(1));
				} catch (ExecutorException e) {
					LogHelper.error("Unable to process documents, Exception in ThreadManager : " + e.getMessage(), e);
					return;
				}
			}

		}
		long start = System.currentTimeMillis();
		LogHelper.info("Submitting " + documents.size() + " documents with "
				+ globalConfiguration.getDocumentThreadPoolSize(1) + " thread(s)");

		// several categories :
		// - OMS direct (preformated documents
		// - response only
		// - print or save
		// - (print or save) and response
		// For response : export rest API
		// other can be done with batch processor
		List<Document> omsOrPrint = new ArrayList<Document>();
		List<Document> preformattedDocuments = new ArrayList<Document>();
		List<Document> responseTransientDocuments = new ArrayList<Document>();

		for (Document doc : documents) {
			try {
				DocumentConfiguration docConfig = globalConfiguration.getDocumentConfiguration(doc.getName());
				DocumentActions config = doc.getDocumentActions();

				if (config == null) {
					config = docConfig.createDocumentActions(doc);
					doc.setDocumentActions(config);
				}

				if (config.getGlobalStatus() != Status.PENDING) {
					continue;
				} else if (config.isPreformattedDocument() || config.getControlJobFile() != null) {
					preformattedDocuments.add(doc);
				} else if (StringUtils.isNotBlank(config.getTonicPath())
						|| StringUtils.isNotBlank(config.getPrinter())) {
					omsOrPrint.add(doc);
				} else if (config.isResponse() && StringUtils.isBlank(config.getTonicPath())) {
					responseTransientDocuments.add(doc);
				} else {
					doc.getDocumentActions().getExceptions().add(
							new Exception("No action defined for '" + doc.getId() + "' type '" + doc.getName() + "'"));
					doc.getDocumentActions().getErrorMessages()
							.add("No action defined for '" + doc.getId() + "' type '" + doc.getName() + "'");

					LogHelper.error("No action defined for '" + doc.getId() + "' type '" + doc.getName() + "'");
				}

			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				DocumentActions config = doc.getDocumentActions();
				if (config == null) {
					config = new DocumentActions();
					doc.setDocumentActions(config);
				}

				config.getExceptions().add(e);
				config.getErrorMessages()
						.add("Error occured while preparing the document output configuration : " + e.getMessage());
			}
		}

		// Priority to document responses only
		processTransientDocumentResponse(responseTransientDocuments.toArray(new Document[] {}));

		// print/moms/response Documents
		processTonic_Print_Response(omsOrPrint.toArray(new Document[] {}));
		boolean noErrors = true;
		for (Document doc : omsOrPrint) {

			if (doc.getDocumentActions().getPrinterStatus() == Status.KO
					&& !doc.getDocumentActions().isUpdateOmsParametersOnError()) {
				noErrors = false;
				break;
			}

		}

		updateMomsDocuments(omsOrPrint.toArray(new Document[] {}), noErrors);

		// TODO : preformatted documents
		processPreformattedDocuments(preformattedDocuments.toArray(new Document[] {}));
		noErrors = true;
		for (Document doc : preformattedDocuments) {

			LogHelper.info("Document " + doc.getId() + " : printerStatus=" + doc.getDocumentActions().getPrinterStatus());
			if (doc.getDocumentActions().getPrinterStatus() == Status.KO
					&& !doc.getDocumentActions().isUpdateOmsParametersOnError()) {
				noErrors = false;
				break;
			}

		}
		if (noErrors)
		{
			LogHelper.info("Updating document status in MOMS...");
			updateMomsDocuments(preformattedDocuments.toArray(new Document[] {}), noErrors);
			LogHelper.info("Update done.");
		}
		else
			LogHelper.warn("Errors occured during process, documents status won't be updated in MOMS");

		backupDocumentsOnError(documents.toArray(new Document[] {}));

		int nbSuccess = 0;
		int nbFailed = 0;
		int nbNotProcessed = 0;
		for (Document doc : documents) {
			if (doc.getDocumentActions().getGlobalStatus() == Status.KO) {
				nbFailed++;
			} else if (doc.getDocumentActions().getGlobalStatus() == Status.OK) {
				nbSuccess++;
			} else {
				nbNotProcessed++;
			}
			doc.clean();
		}

		// TODO : purge temp files
		long stop = System.currentTimeMillis();
		Duration duration = Duration.ofMillis(stop - start);
		LogHelper.info("Job submission terminated, duration : " + duration + ", documents : " + documents.size()
				+ ", success : " + nbSuccess + ", failed : " + nbFailed + ", ignored : " + nbNotProcessed);

	}

	protected void processPreformattedDocuments(Document[] docs) {

		List<Task> tasks = new ArrayList<Task>(docs.length);
		for (Document doc : docs) {
			Task task = new Task(doc.getId()) {

				@Override
				public void run() {
					this.started = true;
					try {
						Response response = processPreformattedDocument(doc);
						if (response.isSuccess()) {
							JsonReader jsonReader = Json
									.createReader(new StringReader(String.valueOf(response.getResponse())));
							JsonObject json = jsonReader.readObject();
							int inputID = json.getInt("inputId");
							doc.getDocumentActions().setPrinterStatus(Status.OK);
							doc.getDocumentActions().addPrintId(inputID);
							LogHelper.info("Document '" + doc.getId() + "' sent to MOMS with id " + inputID);
						} else {
							doc.getDocumentActions().setPrinterStatus(Status.KO);
							doc.getDocumentActions().getErrorMessages().add("Could not send preformatted document '"
									+ doc.getId() + "' to MOMS : " + new String(response.getBody()));
							doc.getDocumentActions().getExceptions()
									.add(new Exception("Could not send preformatted document '" + doc.getId()
											+ "' to MOMS : " + new String(response.getBody())));
							LogHelper.error("Could not send preformatted document '" + doc.getId() + "' to MOMS : "
									+  new String(response.getBody()));
						}
					} catch (Throwable e) {
						doc.getDocumentActions().setPrinterStatus(Status.KO);
						doc.getDocumentActions().getErrorMessages().add("Could not send preformatted document '"
								+ doc.getId() + "' to MOMS : " + Utils.getStackTrace(e));
						doc.getDocumentActions().getExceptions().add(e);
						LogHelper.error("Could not send preformatted document '" + doc.getId() + "' to MOMS : "
								+ Utils.getStackTrace(e));
					} finally {
						this.terminated = true;
					}
				}
			};

			tasks.add(task);

			ThreadManager.geThreadExecutor(ThreadManager.DOCUMENTS_THREAD_POOL).enqueueTask(task);
		}

		Task.waitAllTerminated(tasks);

	}

	protected Response processPreformattedDocument(Document doc)
			throws IOException, FreeMarkerException, DocumentGeneratorException, XfException, DataSourceException {
		if (!doc.getDocumentActions().isPreformattedDocument())
			return null;

		File mfd = File.createTempFile(doc.getId(), ".mfd");
		Utils.copyInputStreamToFile(doc.getMainDatasource().getDataInputStreamToClose(), mfd);// InputStreamClosed in
																								// copyInputStreamToFile
		File xmlxf = null;
		if (doc.getDocumentActions().getControlJobFile() == null) {
			Map<String, Object> context = FreeMarkerHelper.getContextFromDocument(doc);

			xmlxf = FreeMarkerHelper.getInstance().createXmlXf(doc.getDocumentActions().getControlJob(), context);// Creation
																													// fichier
																													// XMLXF
																													// qui
																													// sera
																													// converti
																													// en
																													// MCJ
																													// à
																													// partir
																													// d'un
																													// template
																													// freemarker
			// l'objet thisDoc contient une hashmap utilisée par Freemaker pour faire des
			// tests et substitutions
		} else {
			xmlxf = doc.getDocumentActions().getControlJobFile();
		}

		File mcjFile = new File(xmlxf.getParent(), FilenameUtils.getBaseName(xmlxf.getName()) + ".mcj");
		XfXml2ControlJob.convert(false, xmlxf, doc.getPageInfos(), mcjFile);// Création fichier MCJ

		Response response = null;
		try {

			response = getMomsClient().send(mfd, mcjFile, globalConfiguration.getKWUser(), null,
					globalConfiguration.getKWPlainPassword());
			return response;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw e;
		} catch (Throwable t) {
			throw new DocumentGeneratorException(
					"Could not process preformatted document '" + doc.getId() + "' : " + t.getMessage(), t);

		} finally {

			if (mcjFile != null)
				mcjFile.delete();
			if (xmlxf != null)
				xmlxf.delete();
			if (mfd != null)
				mfd.delete();
		}

	}

	protected void processTransientDocumentResponse(Document[] documents) {
		List<Task> tasks = new ArrayList<Task>(documents.length);
		for (Document doc : documents) {

			Task task = new Task(doc.getId()) {

				@Override
				public void run() {
					this.started = true;
					List<InputStream> inputStreams = new ArrayList<InputStream>();
					try {

						InputStream mainInputStream = doc.getMainDatasource().getDataInputStreamToClose();

						inputStreams.add(mainInputStream);
						Map<String, String> dataBindings = new HashMap<String, String>();
						dataBindings.put(doc.getMainDatasource().getName(),
								IOUtils.toString(mainInputStream, StandardCharsets.UTF_8.name()));
						for (Entry<String, Datasource> entry : doc.getAdditionalDatasources().entrySet()) {
							InputStream is = entry.getValue().getDataInputStreamToClose();
							inputStreams.add(is);

							dataBindings.put(entry.getValue().getName(),
									IOUtils.toString(is, StandardCharsets.UTF_8.name()));
						}

						String mimeType = doc.getDocumentActions().getMimeType();

						Response result = getMtextClient().templateExport(
								doc.getDocumentActions().getTemplate().replaceFirst("//", "/"), dataBindings,
								doc.getId(), mimeType, JobExecutionStrategy.ROLLBACK_ON_FAILURE, null, true, null,
								doc.getDocumentActions().getMetadata(), null, null, null,
								globalConfiguration.getKWUser(), globalConfiguration.getKWCryptedPassword(),
								globalConfiguration.getKWPlainPassword());
						if (result.isSuccess()) {
							doc.getDocumentActions().setReturnStatus(Status.OK);
							FileUtils.writeByteArrayToFile(new File(doc.getDocumentActions().getReturnPath()),
									result.getBody());
							doc.getDocumentActions().setReturnStatus(Status.OK);
							LogHelper.info("Document '" + doc.getId() + "' exported to '"
									+ doc.getDocumentActions().getReturnPath() + "'");
						} else {
							doc.getDocumentActions().setReturnStatus(Status.KO);

							doc.getDocumentActions().getErrorMessages()
									.add("Could not export  document '" + doc.getId() + "' to '"
											+ doc.getDocumentActions().getReturnPath() + "', : "
											+ new String(result.getBody()));
							doc.getDocumentActions().getExceptions()
									.add(new Exception("Could not export  document '" + doc.getId() + "' to '"
											+ doc.getDocumentActions().getReturnPath() + "', : "
											+ new String(result.getBody())));
							LogHelper.error("Could not export  document '" + doc.getId() + "' to '"
									+ doc.getDocumentActions().getReturnPath() + "', : " + new String(result.getBody()));

						}

					} catch (Throwable e) {
						// TODO Auto-generated catch block

						doc.getDocumentActions().setPrinterStatus(Status.KO);

						doc.getDocumentActions().getErrorMessages()
								.add("Could not export document '" + doc.getId() + "' to '"
										+ doc.getDocumentActions().getReturnPath() + "', : " + Utils.getStackTrace(e));
						doc.getDocumentActions().getExceptions()
								.add(new Exception("Could not export document '" + doc.getId() + "' to '"
										+ doc.getDocumentActions().getReturnPath() + "', : " + Utils.getStackTrace(e)));
						LogHelper.error("Could not export document '" + doc.getId() + "' to '"
								+ doc.getDocumentActions().getReturnPath() + "', : " + Utils.getStackTrace(e));

					} finally {
						this.terminated = true;
						for (InputStream is : inputStreams) {
							IOUtils.closeQuietly(is);
						}
					}
				}
			};

			tasks.add(task);

			ThreadManager.geThreadExecutor(ThreadManager.DOCUMENTS_THREAD_POOL).enqueueTask(task);

		}

		Task.waitAllTerminated(tasks);

	}

	protected void processDocumentsForBatchProcessor(InputSource[] documentsInputSources,
			BatchProcessorConfigurator batchConfigurator) {
		try {
			if (documentsInputSources.length > 0) {

				BatchProcessor.execute(documentsInputSources, batchConfigurator);
				LogHelper.error("BatchProcessor.execute has exited.");

			}
		} catch (BatchProcessorException e) {
			// TODO Auto-generated catch block
//			LogHelper.error("BatchProcessor.execute raised an error : " + e.getMessage());
			LogHelper.error("BatchProcessor.execute raised an error : " + Utils.getStackTrace(e));

		}
	}

	protected void backupDocumentsOnError(Document[] documents) {
		// Save document on errors after BAtch Processor
		List<Task> tasks = new ArrayList<Task>(documents.length);

		for (Document doc : documents) {

			Task task = new Task(doc.getName()) {

				@Override
				public void run() {
					this.started = true;
					try {
						if (doc.getDocumentActions().getErrorMessages().size() != 0) {
							File bakFolder = new File(getErrorFolder(), Utils.normalizeXmlName(doc.getName()));
							bakFolder = new File(bakFolder, Utils.normalizeXmlName(doc.getOrigin()));
							bakFolder = new File(bakFolder, String.format("%10d", doc.getNum()));
							if (!bakFolder.exists())
								bakFolder.mkdirs();

							Utils.saveDocument(doc, globalConfiguration.getDocumentConfiguration(doc.getName()),
									bakFolder);

						} else {
							// no error
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						this.terminated = true;
					}

				}
			};

			tasks.add(task);
			ThreadManager.geThreadExecutor(ThreadManager.DOCUMENTS_THREAD_POOL).enqueueTask(task);
		}

		Task.waitAllTerminated(tasks);
	}

	protected void updateMomsDocuments(Document[] documents, boolean noError) {

		LinkedHashMap<String, List<Document>> updates = new LinkedHashMap<String, List<Document>>();
		LinkedHashMap<String, List<Document>> statusUpdates = new LinkedHashMap<String, List<Document>>();

		for (Document doc : documents) {
			doc.getDocumentActions().getPrintIds().sort(new LongComparator());
		}

		Document[] documentsArr = documents.clone();
		Arrays.sort(documentsArr, new Comparator<Document>() {

			@Override
			public int compare(Document o1, Document o2) {
				long id1 = o1.getDocumentActions().getPrintIds().size() == 0 ? -1
						: o1.getDocumentActions().getPrintIds().get(0);
				long id2 = o2.getDocumentActions().getPrintIds().size() == 0 ? -1
						: o2.getDocumentActions().getPrintIds().get(0);

				return Long.compare(id1, id2);
			}
		});

		for (Document doc : documentsArr) {

			if (doc.getDocumentActions().getPrinterStatus() != Status.OK
					&& doc.getDocumentActions().getPrinterStatus() != Status.KO)
			{
				LogHelper.warn("Skipping update status for " + doc.getId());
				continue;
			}

			for (Entry<String, String> entry : doc.getDocumentActions().getUpdateOmsParameters().entrySet()) {

				String key = entry.getKey() + "=" + entry.getValue();
				List<Document> docs = updates.get(key);
				if (docs == null)
					docs = new ArrayList<Document>();
				updates.put(key, docs);
				docs.add(doc);
			}
			if (StringUtils.isNotEmpty(globalConfiguration.getJobStatusMetadata())) {
				String keyStatus = globalConfiguration.getJobStatusMetadata() + "=" + (noError ? "SUCCESS" : "FAILED");
				List<Document> docs = statusUpdates.get(keyStatus);
				if (docs == null)
					docs = new ArrayList<Document>();
				statusUpdates.put(keyStatus, docs);
				docs.add(doc);
			}

			if (StringUtils.isNotEmpty(globalConfiguration.getDocStatusMetadata())) {
				String keyStatus = globalConfiguration.getDocStatusMetadata() + "="
						+ (doc.getDocumentActions().getPrinterStatus() == Status.OK ? "SUCCESS" : "FAILED");
				List<Document> docs = statusUpdates.get(keyStatus);
				if (docs == null)
					docs = new ArrayList<Document>();
				statusUpdates.put(keyStatus, docs);
				docs.add(doc);
			}
		}

		Iterator<Entry<String, List<Document>>> statusUpdateIterator = statusUpdates.entrySet().iterator();

		while (statusUpdateIterator.hasNext()) {
			Entry<String, List<Document>> next = statusUpdateIterator.next();
			updates.put(next.getKey(), next.getValue());
		}
		Set<Entry<String, List<Document>>> entries = updates.entrySet();

		for (Entry<String, List<Document>> updateList : entries) {
			List<Long> ids = new ArrayList<Long>();
			for (Document doc : updateList.getValue()) {
				ids.addAll(doc.getDocumentActions().getPrintIds());
			}
			ids.sort(new LongComparator());

			List<Pair> pairs = new ArrayList<Pair>();
			Pair pair = null;
			long lastId = -1;

			for (long id : ids) {

				if (lastId == -1) {
					pair = new Pair();
					pairs.add(pair);
					pair.setFirstValue(id);
					lastId = (long) pair.getFirstValue();
				} else if (id - lastId > 1) {// rupture
					pair = new Pair();
					pairs.add(pair);
					pair.setFirstValue(id);
					lastId = (long) pair.getFirstValue();
				} else {
					pair.setSecondValue(id);
					lastId = (long) pair.getSecondValue();
				}
			}

			List<SearchParameter> searchParams = new ArrayList<SearchParameter>();
			for (Pair aPair : pairs) {
				SearchParameter param = new SearchParameter();
				param.setName("KW_INPUT_ID");
				searchParams.add(param);
				if (pairs.size() == 1) {
					param.setLogicalOperator(LogicalOperator.AND);
				} else {
					param.setLogicalOperator(LogicalOperator.OR);
				}
				if (aPair.getSecondValue() != null) {
					param.setOperator(Operator.BETWEEN);
					param.addValue(String.valueOf(aPair.getFirstValue()));
					param.addValue(String.valueOf(aPair.getSecondValue()));
				} else {
					param.setOperator(Operator.EQ);
					param.addValue(String.valueOf(aPair.getFirstValue()));
				}
			}
			if (searchParams.size() > 0) {

				String key = StringUtils.substringBefore(updateList.getKey(), "=");
				String value = StringUtils.substringAfter(updateList.getKey(), "=");
				Map<String, String> parameter = new HashMap<String, String>(1);
				parameter.put(key, value);
				try {
					getMomsClient().updateMedata(searchParams, parameter, globalConfiguration.getKWUser(), null,
							globalConfiguration.getKWPlainPassword());
					LogHelper
							.info("Updated '" + updateList.getKey() + "' for document with OMS ID=" + pairs.toString());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	protected void processTonic_Print_Response(Document[] documents) {

		List<Task> tasks = new ArrayList<Task>(documents.length);

		for (Document doc : documents) {

			Task task = new Task(doc.getId()) {

				@Override
				public void run() {
					this.started = true;
					try {
						List<InputStream> inputStreams = new ArrayList<InputStream>();
						Map<String, String> dataBinding = new HashMap<String, String>();
						try {

							InputStream mainInputStream = doc.getMainDatasource().getDataInputStreamToClose();
							inputStreams.add(mainInputStream);

							dataBinding.put(doc.getMainDatasource().getName(),
									IOUtils.toString(mainInputStream, StandardCharsets.UTF_8.name()));
							for (Entry<String, Datasource> entry : doc.getAdditionalDatasources().entrySet()) {
								InputStream is = entry.getValue().getDataInputStreamToClose();
								inputStreams.add(is);

								dataBinding.put(entry.getValue().getName(),
										IOUtils.toString(is, StandardCharsets.UTF_8.name()));

							}
						} catch (IOException | DataSourceException e1) {
							doc.getDocumentActions().setTonicStatus(Status.KO);

							doc.getDocumentActions().getErrorMessages()
									.add("Could not create data binding for tonic document '" + doc.getId() + "' : "
											+ Utils.getStackTrace(e1));
							doc.getDocumentActions().getExceptions().add(e1);
							LogHelper.error("Could not create data binding for tonic document '" + doc.getId() + "' : "
									+ Utils.getStackTrace(e1));
							return;// on passe au doc suivant...
						} finally {
							for (InputStream is : inputStreams) {
								IOUtils.closeQuietly(is);
							}
						}

						if (doc.getDocumentActions().isSaveInTonic()) {
							Response result = null;
							try {
								// Create
								result = getMtextClient().templateCreateDocument(
										doc.getDocumentActions().getTemplate().replaceFirst("//", "/"), dataBinding,
										doc.getDocumentActions().getTonicPath(), true,
										JobExecutionStrategy.ROLLBACK_ON_FAILURE, null, true, null,
										doc.getDocumentActions().getMetadata(), null, null, null,
										globalConfiguration.getKWUser(), null,
										globalConfiguration.getKWPlainPassword());
							} catch (IOException e) {
								// error
								doc.getDocumentActions().setTonicStatus(Status.KO);

								doc.getDocumentActions().getErrorMessages().add("Could not create tonic document '"
										+ doc.getId() + "' : " + Utils.getStackTrace(e));
								doc.getDocumentActions().getExceptions().add(e);
								LogHelper.error("Could not create tonic document '" + doc.getId() + "' : "
										+ Utils.getStackTrace(e));
								return;// On passe au doc suivant
							}

							if (!result.isSuccess()) {
								// failed
								doc.getDocumentActions().setTonicStatus(Status.KO);

								doc.getDocumentActions().getErrorMessages().add("Could not create tonic document '"
										+ doc.getId() + "' : " + result.getResponse());
								doc.getDocumentActions().getExceptions()
										.add(new Exception("Could not create tonic document '" + doc.getId() + "' : "
												+ result.getResponse()));
								LogHelper.error("Could not create tonic document '" + doc.getId() + "' : "
										+ result.getResponse());
								return;// On passe au doc suivant
							} else {// success
								LogHelper
										.info("Tonic document created '" + doc.getId() + "' : " + result.getResponse());
								doc.getDocumentActions().setTonicStatus(Status.OK);
								doc.getDocumentActions().setTonicPathResult(String.valueOf(result.getResponse()));
								if (doc.getDocumentActions().isResponse()) {// Response
									String mimeType = doc.getDocumentActions().getMimeType();//.getMimeType(doc.getDocumentActions().getReturnPath());
									try {
										result = getMtextClient().documentExport(
												doc.getDocumentActions().getTonicPath(), mimeType,
												JobExecutionStrategy.ROLLBACK_ON_FAILURE, null, null,
												globalConfiguration.getKWUser(), null,
												globalConfiguration.getKWPlainPassword());
									} catch (IOException e) {
										// Error
										doc.getDocumentActions().setReturnStatus(Status.KO);

										doc.getDocumentActions().getErrorMessages()
												.add("Could not export tonic document '" + doc.getId() + "', path : '"
														+ doc.getDocumentActions().getTonicPath() + "', printer : '"
														+ doc.getDocumentActions().getPrinter() + "', : "
														+ Utils.getStackTrace(e));
										doc.getDocumentActions().getExceptions()
												.add(new Exception("Could not export tonic document '" + doc.getId()
														+ "', path : '" + doc.getDocumentActions().getTonicPath()
														+ "', printer : '" + doc.getDocumentActions().getPrinter()
														+ "', : " + Utils.getStackTrace(e)));
										LogHelper.error("Could not export tonic document '" + doc.getId()
												+ "', path : '" + doc.getDocumentActions().getTonicPath()
												+ "', printer : '" + doc.getDocumentActions().getPrinter() + "', : "
												+ Utils.getStackTrace(e));
										return;
									}
									if (result.isSuccess()) {
										try {
											// Save on disk
											FileUtils.writeByteArrayToFile(
													new File(doc.getDocumentActions().getReturnPath()),
													result.getBody());
											doc.getDocumentActions().setReturnStatus(Status.OK);
											LogHelper.info("Tonic document '" + doc.getId() + "' exported to '"
													+ doc.getDocumentActions().getReturnPath() + "'");

										} catch (IOException e) {
											doc.getDocumentActions().setPrinterStatus(Status.KO);

											doc.getDocumentActions().getErrorMessages()
													.add("Could not export tonic document '" + doc.getId()
															+ "', path : '" + doc.getDocumentActions().getTonicPath()
															+ "', response : '"
															+ doc.getDocumentActions().getReturnPath() + "', : "
															+ Utils.getStackTrace(e));
											doc.getDocumentActions().getExceptions()
													.add(new Exception("Could not export tonic document '" + doc.getId()
															+ "', path : '" + doc.getDocumentActions().getTonicPath()
															+ "', response : '"
															+ doc.getDocumentActions().getReturnPath() + "', : "
															+ Utils.getStackTrace(e)));
											LogHelper.error("Could not export tonic document '" + doc.getId()
													+ "', path : '" + doc.getDocumentActions().getTonicPath()
													+ "', response : '" + doc.getDocumentActions().getReturnPath()
													+ "', : " + Utils.getStackTrace(e));
											return;
										}

									} else {// failed
										doc.getDocumentActions().setReturnStatus(Status.KO);

										doc.getDocumentActions().getErrorMessages()
												.add("Could not export tonic document '" + doc.getId() + "', path : '"
														+ doc.getDocumentActions().getTonicPath() + "', response : '"
														+ doc.getDocumentActions().getReturnPath() + "', : "
														+ new String(result.getBody()));
										doc.getDocumentActions().getExceptions()
												.add(new Exception("Could not export tonic document '" + doc.getId()
														+ "', path : '" + doc.getDocumentActions().getTonicPath()
														+ "', response : '" + doc.getDocumentActions().getReturnPath()
														+ "', : " +new String(result.getBody())));
										LogHelper.error("Could not export tonic document '" + doc.getId()
												+ "', path : '" + doc.getDocumentActions().getTonicPath()
												+ "', response : '" + doc.getDocumentActions().getReturnPath() + "', : "
												+new String(result.getBody()));
										return;
									}

								}

								if (doc.getDocumentActions().isPrint()) {
									try {
										// Print document
										result = getMtextClient().documentPrint(doc.getDocumentActions().getTonicPath(),
												doc.getDocumentActions().getPrinter(), true, false,
												JobExecutionStrategy.ROLLBACK_ON_FAILURE, null,
												globalConfiguration.getKWUser(), null,
												globalConfiguration.getKWPlainPassword());
									} catch (IOException e) {
										// Error
										doc.getDocumentActions().setPrinterStatus(Status.KO);

										doc.getDocumentActions().getErrorMessages()
												.add("Could not print tonic document '" + doc.getId() + "', path : '"
														+ doc.getDocumentActions().getTonicPath() + "', printer : '"
														+ doc.getDocumentActions().getPrinter() + "', : "
														+ Utils.getStackTrace(e));
										doc.getDocumentActions().getExceptions()
												.add(new Exception("Could not print tonic document '" + doc.getId()
														+ "', path : '" + doc.getDocumentActions().getTonicPath()
														+ "', printer : '" + doc.getDocumentActions().getPrinter()
														+ "', : " + Utils.getStackTrace(e)));
										LogHelper.error("Could not print tonic document '" + doc.getId() + "', path : '"
												+ doc.getDocumentActions().getTonicPath() + "', printer : '"
												+ doc.getDocumentActions().getPrinter() + "', : "
												+ Utils.getStackTrace(e));
										return;
									}
									if (result.isSuccess()) {// Success
										JsonReader jsonReader = Json
												.createReader(new StringReader(String.valueOf(result.getResponse())));
										JsonObject json = jsonReader.readObject();
										int inputID = json.getJsonObject("entries").getInt("InputId");

										doc.getDocumentActions().addPrintId(inputID);
										doc.getDocumentActions().setPrinterStatus(Status.OK);

										LogHelper.info("Tonic document '" + doc.getId() + "' printed to '"
												+ doc.getDocumentActions().getPrinter() + "' : "
												+ result.getResponse());
									} else {// failed
										doc.getDocumentActions().setPrinterStatus(Status.KO);

										doc.getDocumentActions().getErrorMessages()
												.add("Could not print tonic document '" + doc.getId() + "', path : '"
														+ doc.getDocumentActions().getTonicPath() + "', printer : '"
														+ doc.getDocumentActions().getPrinter() + "', : "
														+ new String(result.getBody()));
										doc.getDocumentActions().getExceptions()
												.add(new Exception("Could not print tonic document '" + doc.getId()
														+ "', path : '" + doc.getDocumentActions().getTonicPath()
														+ "', printer : '" + doc.getDocumentActions().getPrinter()
														+ "', : " + new String(result.getBody())));
										LogHelper.error("Could not print tonic document '" + doc.getId() + "', path : '"
												+ doc.getDocumentActions().getTonicPath() + "', printer : '"
												+ doc.getDocumentActions().getPrinter() + "', : "
												+new String(result.getBody()));
										return;
									}
								}
							}

						} else if (doc.getDocumentActions().isPrint()) {// transient, juste print to OMS
							Response result = null;
							try {
								// print
								result = getMtextClient().templatePrint(
										doc.getDocumentActions().getTemplate().replaceFirst("//", "/"), dataBinding,
										UUID.randomUUID().toString(), doc.getDocumentActions().getPrinter(),
										JobExecutionStrategy.ROLLBACK_ON_FAILURE, null, true, null,
										doc.getDocumentActions().getMetadata(), null, null, null,
										globalConfiguration.getKWUser(), null,
										globalConfiguration.getKWPlainPassword());
							} catch (IOException e) {
								doc.getDocumentActions().setPrinterStatus(Status.KO);

								doc.getDocumentActions().getErrorMessages()
										.add("Could not print tonic document '" + doc.getId() + "', printer : '"
												+ doc.getDocumentActions().getPrinter() + "', "
												+ Utils.getStackTrace(e));
								doc.getDocumentActions().getExceptions()
										.add(new Exception("Could not print tonic document '" + doc.getId()
												+ "', printer : '" + doc.getDocumentActions().getPrinter() + "', "
												+ Utils.getStackTrace(e)));
								LogHelper.error("Could not print document '" + doc.getId() + "', printer : '"
										+ doc.getDocumentActions().getPrinter() + "', : " + Utils.getStackTrace(e));
								return;
							}
							if (result.isSuccess()) {

								JsonReader jsonReader = Json
										.createReader(new StringReader(String.valueOf(result.getResponse())));
								JsonObject json = jsonReader.readObject();
								int inputID = json.getJsonObject("entries").getInt("InputId");

								doc.getDocumentActions().addPrintId(inputID);
								doc.getDocumentActions().setPrinterStatus(Status.OK);
								LogHelper.info("Document '" + doc.getId() + "' printed to '"
										+ doc.getDocumentActions().getPrinter() + "' : " + result.getResponse());
							} else {
								// failed
								doc.getDocumentActions().setPrinterStatus(Status.KO);

								doc.getDocumentActions().getErrorMessages()
										.add("Could not print tonic document '" + doc.getId()
												+ "', path : ', printer : '" + doc.getDocumentActions().getPrinter()
												+ "', : " +new String(result.getBody()));
								doc.getDocumentActions().getExceptions()
										.add(new Exception("Could not print tonic document '" + doc.getId()
												+ "', printer : '" + doc.getDocumentActions().getPrinter() + "', : "
												+ new String(result.getBody())));
								LogHelper.error("Could not print tonic document '" + doc.getId() + "', printer : '"
										+ doc.getDocumentActions().getPrinter() + "', : " + new String(result.getBody()));
								return;
							}
						} else {// No action defined...
							doc.getDocumentActions().getExceptions().add(new Exception(
									"No action defined for '" + doc.getId() + "' type '" + doc.getName() + "'"));
							doc.getDocumentActions().getErrorMessages()
									.add("No action defined for '" + doc.getId() + "' type '" + doc.getName() + "'");

							LogHelper.error("No action defined for '" + doc.getId() + "' type '" + doc.getName() + "'");
							return;
						}

					} finally {
						this.terminated = true;
					}

				}
			};

			tasks.add(task);
			ThreadManager.geThreadExecutor(ThreadManager.DOCUMENTS_THREAD_POOL).enqueueTask(task);
		}

		Task.waitAllTerminated(tasks);

	}

	public MomsClientAPI getMomsClient() {
		if (momsClient == null) {
			try {
				momsClient = this.globalConfiguration.getMomsClient();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return momsClient;
	}

	public MtextClientAPI getMtextClient() {
		if (mtextClient == null) {
			try {
				mtextClient = this.globalConfiguration.getMtextClient();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				LogHelper.error("Cannot create MtextAPI client : " + e.getMessage(), e);
			}
		}
		return mtextClient;
	}

	public File getErrorFolder() {
		return errorFolder;
	}

	public void setErrorFolder(File errorFolder) {
		this.errorFolder = errorFolder;
	}
}
