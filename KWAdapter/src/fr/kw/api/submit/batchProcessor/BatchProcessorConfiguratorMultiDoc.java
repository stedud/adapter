package fr.kw.api.submit.batchProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.python.jline.internal.InputStreamReader;

import de.kwsoft.mtext.api.databinding.CSVDataSource;
import de.kwsoft.mtext.api.databinding.DataSource;
import de.kwsoft.mtext.api.databinding.JSONDataSource;
import de.kwsoft.mtext.api.databinding.XMLDataSource;
import de.kwsoft.mtext.tools.batchprocessor.api.AdditionalSource;
import de.kwsoft.mtext.tools.batchprocessor.api.BatchProcessorConfigurationException;
import de.kwsoft.mtext.tools.batchprocessor.api.DocumentExceptionInfo;
import de.kwsoft.mtext.tools.batchprocessor.api.DocumentProcessingConfiguration;
import de.kwsoft.mtext.tools.batchprocessor.api.DocumentProcessingException;
import de.kwsoft.mtext.tools.batchprocessor.api.InputSourceProcessingConfiguration;
import de.kwsoft.mtext.tools.batchprocessor.api.InputSourceProcessingException;
import de.kwsoft.mtext.tools.batchprocessor.api.MappingParameter;
import de.kwsoft.mtext.tools.batchprocessor.api.ProcessedDocumentInformation;
import fr.kw.adapter.document.DataSourceException;
import fr.kw.adapter.document.Datasource;
import fr.kw.adapter.document.DatasourceType;
import fr.kw.adapter.document.Document;
import fr.kw.api.submit.Status;
import fr.kw.api.submit.SubmitConfiguration;
import fr.utils.LogHelper;
import fr.utils.Utils;

public class BatchProcessorConfiguratorMultiDoc
		implements de.kwsoft.mtext.tools.batchprocessor.api.BatchProcessorConfigurator {

	/**
	 * Store the document configuration for each input data that must be processed.
	 * This map must be filled before calling the batch processor so that it will
	 * find the specific configuration of each document to process.
	 */
	protected LinkedHashMap<String, Document> documentConfigurations = new LinkedHashMap<String, Document>();
	private SubmitConfiguration globalConfiguration;
	public static String DELIMITER = "\\|/";
	protected static final Pattern pattern = Pattern.compile(DELIMITER);

	public BatchProcessorConfiguratorMultiDoc(SubmitConfiguration configuration) {
		super();
		this.globalConfiguration = configuration;
	}

	public void addDocumentConfiguration(String docIdentifier, Document docConfig) {
		this.documentConfigurations.put(docIdentifier, docConfig);
	}

	public Document[] getConfiguredDocuments() {
		return documentConfigurations.values().toArray(new Document[] {});
	}

	@Override
	public void configure(String sourceId, DataSource dataSource, DocumentProcessingConfiguration configuration) {
		// super.configure(sourceId,dataSource,configuration);
		LogHelper.debug("Document configure(" + sourceId + ", " + dataSource + ", " + configuration + ")");

		String suffix = StringUtils.substringAfterLast(sourceId, ".");

		String originalId = suffix != null ? StringUtils.removeEnd(sourceId, "." + suffix) : sourceId;

		Document doc = documentConfigurations.get(originalId);

		if (doc != null) {
			try {
				List<AdditionalSource> additionalSources = new ArrayList<AdditionalSource>(
						doc.getAdditionalDatasources().size());
				for (Entry<String, Datasource> ds : doc.getAdditionalDatasources().entrySet()) {
					AdditionalSource source = new AdditionalSource() {

						@Override
						public DataSource getSource() throws IOException {
							DataSource datasourceResult = null;
							InputStream is = null;

							try {
								is = ds.getValue().getDataInputStreamToClose();
								if (ds.getValue().getType() == DatasourceType.XML) {
									datasourceResult = new XMLDataSource(is);
								} else if (ds.getValue().getType() == DatasourceType.JSON) {

									datasourceResult = new JSONDataSource(IOUtils.toString(is,
											BatchProcessorConfiguratorMultiDoc.this.globalConfiguration.getEncoding()));
								} else if (ds.getValue().getType() == DatasourceType.CSV) {
									Reader reader = new InputStreamReader(is,
											BatchProcessorConfiguratorMultiDoc.this.globalConfiguration.getEncoding());

									datasourceResult = new CSVDataSource(ds.getValue().getName(), reader);
								}
							} catch (DataSourceException e) {
								throw new IOException("Could not create Datasource : " + e.getMessage(), e);
							} finally {

								IOUtils.closeQuietly(is);
							}

							return datasourceResult;
						}

						@Override
						public String getName() {
							return ds.getValue().getName();
						}
					};
					additionalSources.add(source);

				}
				configuration.setAdditionalSources(additionalSources.toArray(new AdditionalSource[] {}));

				configuration.setDataSourceName(doc.getMainDatasource().getName());
				configuration.setExecuteModelsEnabled(true);
				if (StringUtils.isNotBlank(doc.getDocumentActions().getPrinter())) {
					configuration.setPrintEnabled(true);
					configuration.setPrinterNames(new String[] { doc.getDocumentActions().getPrinter() });
				}

				String template = doc.getDocumentActions().getTemplate();

				configuration.setDataBindingName(template);
				if (StringUtils.isNotBlank(doc.getDocumentActions().getTonicPath())) {
					configuration.setTargetPath(doc.getDocumentActions().getTonicPath());
					configuration.setStoreEnabled(true);
				}

				List<MappingParameter> parameters = new ArrayList<>();
				for (Entry<String, String> param : doc.getDocumentActions().getMetadata().entrySet()) {
					MappingParameter mappingParam = new MappingParameter(param.getKey(), param.getValue());
					parameters.add(mappingParam);
				}
				configuration.setMappingParameters(parameters.toArray(new MappingParameter[] {}));

				if (StringUtils.isNotBlank(doc.getDocumentActions().getXmlSplitter())) {
					configuration.setSplittingName(doc.getDocumentActions().getXmlSplitter());
				}

				configuration.setExecuteModelsEnabled(true);

			} catch (Throwable t) {
				if (doc != null) {
					doc.getDocumentActions().getErrorMessages().add("Document '" + sourceId
							+ "', could not configure document settings for batch processor :" + t.getMessage());
					doc.getDocumentActions().getExceptions().add(t);
				}

			}
		} else {
			LogHelper.info("Document " + sourceId + " (" + originalId + ") not found in configuration list");
		}
		LogHelper.info("Document " + sourceId + " : " + configuration);

	}

	@Override
	public void configure(String sourceId, InputSourceProcessingConfiguration configuration) {
		// super.configure(sourceId,configuration);
		LogHelper.debug("Global configure(" + sourceId + ", " + configuration + ")");

		String suffix = StringUtils.substringAfterLast(sourceId, ".");

		String originalId = suffix != null ? StringUtils.removeEnd(sourceId, "." + suffix) : sourceId;
		Document doc = documentConfigurations.get(originalId);
		if (doc != null) {
			configuration.setMTextUser(this.globalConfiguration.getKWUser());
			configuration.setMTextPassword(this.globalConfiguration.getKWPlainPassword());
			configuration.setMTextProviderURLs(
					BatchProcessorConfiguratorMultiDoc.this.globalConfiguration.getUrlForJavaAPI());
			configuration.setDumpResultEnabled(false);
			configuration.setDumpSourceEnabled(false);
			configuration.setMTextBunchSize(this.globalConfiguration.getBunchSize());
			configuration.setProcessorNumber(this.globalConfiguration.getProcessorNumber());
		}
	}

	@Override
	public void handleConfigurationException(String sourceId, BatchProcessorConfigurationException exception) {
		// super.handleConfigurationException(sourceId, exception);
		LogHelper.info("handleConfigurationException:" + sourceId + ", " + exception.getMessage());
		LogHelper.info(Utils.getStackTrace(exception));

		String suffix = StringUtils.substringAfterLast(sourceId, ".");

		String originalId = suffix != null ? StringUtils.removeEnd(sourceId, "." + suffix) : sourceId;

		Document doc = documentConfigurations.get(originalId);
		if (doc != null) {
			LogHelper.error("Set document status to KO");

			doc.getDocumentActions().getExceptions().add(exception);
			doc.getDocumentActions().getErrorMessages()
					.add("Error occured during BatchProcessing : " + exception.getMessage());
			if (doc.getDocumentActions().isPrint())
				doc.getDocumentActions().setPrinterStatus(Status.KO);
			if (doc.getDocumentActions().isResponse())
				doc.getDocumentActions().setReturnStatus(Status.KO);
			if (doc.getDocumentActions().isSaveInTonic())
				doc.getDocumentActions().setTonicStatus(Status.KO);

		} else if ("DocumentProcessor".compareTo(sourceId) == 0) {// Global error
			for (Document aDoc : documentConfigurations.values()) {
				LogHelper.error("Set all documents status to KO");

				aDoc.getDocumentActions().getExceptions().add(exception);
				aDoc.getDocumentActions().getErrorMessages()
						.add("Error occured during BatchProcessing : " + exception.getMessage());
				if (doc.getDocumentActions().isPrint())
					doc.getDocumentActions().setPrinterStatus(Status.KO);
				if (doc.getDocumentActions().isResponse())
					doc.getDocumentActions().setReturnStatus(Status.KO);
				if (doc.getDocumentActions().isSaveInTonic())
					doc.getDocumentActions().setTonicStatus(Status.KO);
			}

		}
	}

	@Override
	public void handleDocumentException(String documentName, DocumentExceptionInfo errorInfo,
			DocumentProcessingException exception) {
		// super.handleDocumentException(documentName, errorInfo, exception);
		LogHelper.debug("handleDocumentException:" + documentName + ", " + errorInfo);
		// TODO : extract sourceID from documentName
		String suffix = StringUtils.substringAfterLast(documentName, ".");
		String error = Utils.getStackTrace(exception);
		String originalId = suffix != null ? StringUtils.removeEnd(documentName, "." + suffix) : documentName;
		Document doc = documentConfigurations.get(originalId);
		if (doc != null) {

			LogHelper.error("Set document status to KO");

			doc.getDocumentActions().getExceptions().add(exception);
			doc.getDocumentActions().getErrorMessages().add("Error occured during BatchProcessing : " + error);
			if (doc.getDocumentActions().isPrint())
				doc.getDocumentActions().setPrinterStatus(Status.KO);
			if (doc.getDocumentActions().isResponse())
				doc.getDocumentActions().setReturnStatus(Status.KO);
			if (doc.getDocumentActions().isSaveInTonic())
				doc.getDocumentActions().setTonicStatus(Status.KO);
		} else {// Global error
			for (Document aDoc : documentConfigurations.values()) {
				LogHelper.error("Set all documents status to KO");

				aDoc.getDocumentActions().getExceptions().add(exception);
				aDoc.getDocumentActions().getErrorMessages().add("Error occured during BatchProcessing : " + error);
				if (doc.getDocumentActions().isPrint())
					doc.getDocumentActions().setPrinterStatus(Status.KO);
				if (doc.getDocumentActions().isResponse())
					doc.getDocumentActions().setReturnStatus(Status.KO);
				if (doc.getDocumentActions().isSaveInTonic())
					doc.getDocumentActions().setTonicStatus(Status.KO);
			}

		}
	}

	@Override
	public void handleDocumentProcessed(String documentName, ProcessedDocumentInformation info) {
		// super.handleDocumentProcessed(documentName, info);
		// TODO Auto-generated method stub
		LogHelper.debug("handleDocumentProcessed:" + documentName + ", " + info);

		String suffix = StringUtils.substringAfterLast(documentName, ".");

		String originalId = suffix != null ? StringUtils.removeEnd(documentName, "." + suffix) : documentName;
		Document doc = documentConfigurations.get(originalId);
		if (doc != null) {
			LogHelper.info("Document '" + documentName + "' processed successfully by BatchProcessor");

			if (StringUtils.isNotBlank(doc.getDocumentActions().getPrinter())) {
				doc.getDocumentActions().setPrinterStatus(Status.OK);
				try {
					doc.getDocumentActions()
							.addPrintId(Long.parseLong(String.valueOf(info.getPrintResult().get("InputId"))));

				} catch (NumberFormatException e) {
					LogHelper.warn("Could not parse OMS print id in " + info.getPrintResult());
				}
			}
			if (StringUtils.isNotBlank(doc.getDocumentActions().getTonicPath())) {
				doc.getDocumentActions().setTonicStatus(Status.OK);
				doc.getDocumentActions().setTonicPathResult(info.getPersistedDocumentName());
			}

		}

	}

	@Override
	public void handleInputProcessingException(String sourceId, InputSourceProcessingException exception) {
		// super.handleInputProcessingException(sourceId, exception);
		LogHelper.debug("handleInputProcessingException:" + sourceId + ", " + exception.getMessage());
		String suffix = StringUtils.substringAfterLast(sourceId, ".");
		String error = Utils.getStackTrace(exception);
		String originalId = suffix != null ? StringUtils.removeEnd(sourceId, "." + suffix) : sourceId;
		Document doc = documentConfigurations.get(originalId);
		if (doc != null) {
			LogHelper.error("Set document status to KO");

			doc.getDocumentActions().getExceptions().add(exception);
			doc.getDocumentActions().getErrorMessages().add("Error occured during BatchProcessing : " + error);
			if (doc.getDocumentActions().isPrint())
				doc.getDocumentActions().setPrinterStatus(Status.KO);
			if (doc.getDocumentActions().isResponse())
				doc.getDocumentActions().setReturnStatus(Status.KO);
			if (doc.getDocumentActions().isSaveInTonic())
				doc.getDocumentActions().setTonicStatus(Status.KO);
		} else {// Global error
			for (Document aDoc : documentConfigurations.values()) {

				aDoc.getDocumentActions().getExceptions().add(exception);
				aDoc.getDocumentActions().getErrorMessages().add("Error occured during BatchProcessing : " + error);
				if (doc.getDocumentActions().isPrint())
					doc.getDocumentActions().setPrinterStatus(Status.KO);
				if (doc.getDocumentActions().isResponse())
					doc.getDocumentActions().setReturnStatus(Status.KO);
				if (doc.getDocumentActions().isSaveInTonic())
					doc.getDocumentActions().setTonicStatus(Status.KO);
			}

		}
	}

}
