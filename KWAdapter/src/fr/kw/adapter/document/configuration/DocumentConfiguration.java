package fr.kw.adapter.document.configuration;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import javax.xml.bind.JAXB;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import fr.freemarker.FreeMarkerException;
import fr.freemarker.FreeMarkerHelper;
import fr.kw.adapter.document.DatasourceType;
import fr.kw.adapter.document.Document;
import fr.kw.adapter.document.DocumentActions;
import fr.utils.Values;
import fr.utils.configuration.BaseConfiguration;
import fr.utils.configuration.FileConfiguration;

public class DocumentConfiguration {

	private DocumentConfiguration() {
		super();
	}

	/**
	 * Indicate the document type (ex: Invoice). This name is used to retrieve the
	 * settings related to this document type in the configuration files. ex:
	 * documentName.printer=OMS documentName.project=Invoices
	 * documentName.template=templates\invoice.template etc...
	 */
	protected String documentName;

	/**
	 * Indicate the environment (DEV/PROD/QUAL/etc...) where the document was
	 * produced
	 */
	protected String environment = "";

	/**
	 * Indicates files to load extra documents for additionnal informations. The
	 * files will be parsed and the result can be used as : - additional data
	 * sources (ex :
	 * Invoice_ADDITIONAL_DATASOURES=extraDocName:extraDatasource;etc... -
	 * additional contexts to create Control Job File. The context object will be
	 * the resulting xml named with the extra document name.
	 * 
	 */
	protected List<String> additionalDocumentFiles;

	/**
	 * Path to a freemarker template file. If the document has to be sent directly
	 * to MOMS (no tonic template), a control job is built using this template. In
	 * case the document layout is made with a Tonic template (see
	 * {@link DocumentConfiguration#mtextSettings}, this field is ignored.
	 */
	protected String controlJobTemplate;

	/**
	 * The full path to the template. ex: /Invoices/template/invoice.template
	 */
	protected String template;

	/**
	 * The full path to the splitter configuration if a splitter must be called.
	 */
	protected String xmlSplitter;

	/**
	 * Indicates a filter to apply on document data. If the value terminates with
	 * xslt, a xslt transform is applied. Otherwise the value is supposed to be a
	 * command working in stdin/stdout mode.
	 * 
	 */
	protected String filter;

	/**
	 * Indicate the moms printer or null. Default moms printer name should be OMS.
	 */
	protected String printer;

	/**
	 * Indicates the full path where the document should be created in Tonic. No
	 * document is created if the value is not set.
	 */
	protected String tonicPath;

	/**
	 * Indicates the full path for the response. The file extension (usually .pdf)
	 * indicates the renderer to use. This option works for single documents only,
	 * it will be ignored if a splitter is set.
	 */
	protected String returnPath;
	
	protected String mimeType;
	

	/**
	 * 
	 */
	protected DatasourceConfiguration mainDatasource;

	/**
	 * Additional datasources. The following syntax is used : -
	 * "documentName:datasourceName" with "documentName" = a document type declared
	 * in the settings, "datasourceName" = the datasource declared in the Tonic
	 * template. - "datasourecName" : if no document name is present (no ":"
	 * delimiter), the main data of the main datasoure will be duplicated and sent
	 * again with the name indicated. The document used to produce the datasource
	 * can come from a document previously parsed in the data file, or from an extra
	 * document set with a parameter
	 * "documentName_ADDITIONAL_DOCUMENT_FILE=../extraFile/${origin).txt". ${origin}
	 * will be replaced by the original name of the input stream, its file name if
	 * the input data stream was initiated from a file.
	 */
	protected List<DatasourceConfiguration> additionalDatasources = new ArrayList<DatasourceConfiguration>(0);

	/**
	 * Metadata for the document.
	 */
	protected Map<String, String> metadata = new HashMap<String, String>();

	/**
	 * OMS parameters to set if the document is sent (printed) to OMS.
	 */
	protected LinkedHashMap<String, String> omsParameters = new LinkedHashMap<String, String>();

	/**
	 * OMS parameters to update, once all the documents of the batch have been sent
	 * to OMS.
	 */
	protected LinkedHashMap<String, String> updateOmsParameters = new LinkedHashMap<String, String>();

	/**
	 * Indicate if the update stage must be done if errors occured during the job
	 * (if a document of the job is on error, none document will be updated).
	 */
	protected boolean updateOmsParametersOnError = true;

	protected File propertiesFile = null;

	public String getDocumentName() {
		return documentName;
	}

	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}

	public String getControlJobTemplate() {
		return controlJobTemplate;
	}

	public void setControlJobTemplate(String controlJobTemplate) {
		this.controlJobTemplate = controlJobTemplate;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public String getXmlSplitter() {
		return xmlSplitter;
	}

	public void setXmlSplitter(String xmlSplitter) {
		this.xmlSplitter = xmlSplitter;
	}

	public String getPrinter() {
		return printer;
	}

	public void setPrinter(String printer) {
		this.printer = printer;
	}

	public String getTonicPath() {
		return tonicPath;
	}

	public void setTonicPath(String tonicPath) {
		this.tonicPath = tonicPath;
	}

	public String getReturnPath() {
		return returnPath;
	}

	public void setReturnPath(String returnPath) {
		this.returnPath = returnPath;
	}

	public Map<String, String> getOmsParameters() {
		return omsParameters;
	}

	public Map<String, String> getUpdateOmsParameters() {
		return updateOmsParameters;
	}

	public DatasourceConfiguration getMainDatasource() {
		return mainDatasource;
	}

	public void setMainDatasource(DatasourceConfiguration mainDatasource) {
		this.mainDatasource = mainDatasource;
	}

	public List<DatasourceConfiguration> getAdditionalDatasources() {
		return additionalDatasources;
	}

	public boolean isAdditionalDatasource(String documentName) {
		for (DatasourceConfiguration dsConf : this.getAdditionalDatasources()) {
			if (StringUtils.compare(dsConf.getDocumentName(), documentName) == 0)
				return true;
		}
		return false;
	}

	public boolean isAdditionalDocumentConfig() {
		return ((getTemplate() == null && !isPreformattedDocument()) || getTemplate().compareTo("NONE") == 0);
	}

	/**
	 * Read the properties configuration and update the current configuration when a
	 * value is set. Parameters not present in the properties won;t be deleted from
	 * the current configuration.
	 * 
	 * @param docConfigProps
	 * @throws DocumentConfigurationException
	 */
	public void overloadConfiguration(FileConfiguration docConfigProps) throws DocumentConfigurationException {
		this.setPropertiesFile(docConfigProps.getPropertiesFile());
		List<String> additionalDocs = docConfigProps.getList(documentName + "_ADDITIONAL_FILES");
		if (additionalDocs == null)
			additionalDocs = Collections.EMPTY_LIST;
		this.setAdditionalDocumentFiles(additionalDocs);

		String controlJobTemplate = docConfigProps.get(documentName + "_OMS_CONTROL_JOB_TEMPLATE");
		if (!StringUtils.isBlank(controlJobTemplate)) {
			this.setControlJobTemplate(controlJobTemplate);
		}

		String env = docConfigProps.get("ENVIRONMENT", "");
		if (StringUtils.isNotBlank(env))
			this.setEnvironment(env);

		String template = docConfigProps.get(documentName + "_TEMPLATE");
		if (StringUtils.isNotBlank(template))
			this.setTemplate(template);
		String tonicPath = docConfigProps.get(documentName + "_TONIC_PATH");
		if (StringUtils.isNotBlank(tonicPath))
			this.setTonicPath(tonicPath);
		String printer = docConfigProps.get(documentName + "_PRINTER");
		if (StringUtils.isNotBlank(printer))
			this.setPrinter(printer);
		String responsePath = docConfigProps.get(documentName + "_RESPONSE_PATH");
		if (StringUtils.isNotBlank(responsePath))
			this.setReturnPath(responsePath);
		String mimeType = docConfigProps.get(documentName + "_RESPONSE_TYPE");
		if (StringUtils.isNotBlank(mimeType))
			this.setMimeType(mimeType);
		
		
		
		String xmlSplitter = docConfigProps.get(documentName + "_XML_SPLITTER");
		if (StringUtils.isNotBlank(xmlSplitter))
			this.setXmlSplitter(xmlSplitter);
		String filter = docConfigProps.get(documentName + "_FILTER");
		if (StringUtils.isNotBlank(filter))
			this.setFilter(filter);

		String mainDataSource = docConfigProps.get(documentName + "_DATASOURCE");
		if (StringUtils.isNotBlank(mainDataSource))
			this.setMainDatasource(new DatasourceConfiguration(mainDataSource));

		List<String> additionalDatasources = docConfigProps.getList(documentName + "_ADDITIONAL_DATASOURCES");
		if (additionalDatasources == null)
			additionalDatasources = Collections.EMPTY_LIST;
		if (additionalDatasources.size() > 0)
			this.getAdditionalDatasources().clear();
		for (String token : additionalDatasources) {
			StringTokenizer parameterValueTokenizer = new StringTokenizer(token, ":");
			if (parameterValueTokenizer.countTokens() == 0 || parameterValueTokenizer.countTokens() >= 3) {
				throw new DocumentConfigurationException("Additional datasource '" + token
						+ "' must contain a key and a value as following 'document:datasource' or just the datasource name. Datasources are separated by ';'. ex : name1:value1;name2;name3:value3;etc...");
			}
			String name = null;
			String docName = null;
			if (parameterValueTokenizer.countTokens() == 1) {
				name = parameterValueTokenizer.nextToken();
			} else if (parameterValueTokenizer.countTokens() == 2) {
				docName = parameterValueTokenizer.nextToken();
				name = parameterValueTokenizer.nextToken();
			}

			this.getAdditionalDatasources().add(new DatasourceConfiguration(name, documentName));
		}

		List<String> OMSParameters = null;

		OMSParameters = docConfigProps.getList(documentName + "_UPDATE_OMS_PARAMETERS");
		if (OMSParameters == null)
			OMSParameters = Collections.EMPTY_LIST;
		if (OMSParameters.size() > 0)
			this.getUpdateOmsParameters().clear();
		for (String token : OMSParameters) {
			StringTokenizer parameterValueTokenizer = new StringTokenizer(token, ":");
			if (parameterValueTokenizer.countTokens() != 2) {
				throw new DocumentConfigurationException("UPDATE OMS Parameter '" + token
						+ "' must contain a key and a value as following 'name:value'. Parameters are separated by ';'. ex : name1:value1;name2:value2;name3:value3;etc...");
			}
			this.getUpdateOmsParameters().put(parameterValueTokenizer.nextToken(), parameterValueTokenizer.nextToken());
		}

		String updateMomsOnError = docConfigProps.get(documentName + "_UPDATE_OMS_ON_ERROR");
		if (StringUtils.isNotBlank(updateMomsOnError)) {
			this.setUpdateOmsParametersOnError(Boolean.parseBoolean(updateMomsOnError));
		}

		if (this.isPreformattedDocument() && this.isResponse())
			throw new DocumentConfigurationException("Document '" + this.getDocumentName()
					+ "', preformatted document cannot return formatted response.");
	}

	public static DocumentConfiguration load(BaseConfiguration properties, String documentName)
			throws DocumentConfigurationException {

		DocumentConfiguration docConfig = new DocumentConfiguration();

		docConfig.setPropertiesFile(properties.getPropertiesFile());

		String controlJobTemplate = properties.get(documentName + "_OMS_CONTROL_JOB_TEMPLATE");
		if (!StringUtils.isBlank(controlJobTemplate)) {
			docConfig.setControlJobTemplate(controlJobTemplate);
		}
		docConfig.setEnvironment(properties.get("ENVIRONMENT", ""));

		docConfig.setTemplate(properties.get(documentName + "_TEMPLATE"));
		docConfig.setTonicPath(properties.get(documentName + "_TONIC_PATH"));
		docConfig.setPrinter(properties.get(documentName + "_PRINTER"));
		docConfig.setReturnPath(properties.get(documentName + "_RESPONSE_PATH"));
		docConfig.setMimeType(properties.get(documentName + "_RESPONSE_TYPE"));
		docConfig.setXmlSplitter(properties.get(documentName + "_XML_SPLITTER"));
		docConfig.setFilter(properties.get(documentName + "_FILTER"));

		List<String> additionnalFiles = properties.getList(documentName + "_ADDITIONAL_FILES");
		if (additionnalFiles == null)
			additionnalFiles = Collections.EMPTY_LIST;
		docConfig.additionalDocumentFiles = additionnalFiles;

		docConfig.setMainDatasource(new DatasourceConfiguration(properties.get(documentName + "_DATASOURCE")));

		List<String> additionalDatasources = properties.getList(documentName + "_ADDITIONAL_DATASOURCES");
		if (additionalDatasources == null)
			additionalDatasources = Collections.EMPTY_LIST;
		for (String token : additionalDatasources) {
			StringTokenizer parameterValueTokenizer = new StringTokenizer(token, ":");
			if (parameterValueTokenizer.countTokens() == 0 || parameterValueTokenizer.countTokens() >= 3) {
				throw new DocumentConfigurationException("Additional datasource '" + token
						+ "' must contain a key and a value as following 'document:datasource' or just the datasource name. Datasources are separated by ';'. ex : name1:value1;name2;name3:value3;etc...");
			}
			String name = null;
			String docName = null;
			if (parameterValueTokenizer.countTokens() == 1) {
				name = Values.getValue(parameterValueTokenizer.nextToken());
				docName = Values.getValue(documentName);
			} else if (parameterValueTokenizer.countTokens() == 2) {
				name = Values.getValue(parameterValueTokenizer.nextToken());
				docName = Values.getValue(parameterValueTokenizer.nextToken());
			}

			docConfig.getAdditionalDatasources().add(new DatasourceConfiguration(name, docName));
		}

		List<String> OMSParameters = null;

		OMSParameters = properties.getList(documentName + "_UPDATE_OMS_PARAMETERS");
		if (OMSParameters == null)
			OMSParameters = Collections.EMPTY_LIST;
		for (String token : OMSParameters) {
			StringTokenizer parameterValueTokenizer = new StringTokenizer(token, ":");
			if (parameterValueTokenizer.countTokens() != 2) {
				throw new DocumentConfigurationException("UPDATE OMS Parameter '" + token
						+ "' must contain a key and a value as following 'name:value'. Parameters are separated by ';'. ex : name1:value1;name2:value2;name3:value3;etc...");
			}
			docConfig.getUpdateOmsParameters().put(Values.getValue(parameterValueTokenizer.nextToken()),
					Values.getValue(parameterValueTokenizer.nextToken()));
		}

		List<String> metadataList = properties.getList(documentName + "_METADATA");
		if (metadataList == null)
			metadataList = Collections.EMPTY_LIST;
		for (String token : metadataList) {

			StringTokenizer parameterValueTokenizer = new StringTokenizer(token, ":");
			if (parameterValueTokenizer.countTokens() != 2) {
				throw new DocumentConfigurationException("Metadata '" + token
						+ "' must contain a key and a value as following 'name:value'. Metadata are separated by ';'. ex : name1:value1;name2:value2;name3:value3;etc...");
			}
			docConfig.getMetadata().put(Values.getValue(parameterValueTokenizer.nextToken()),
					Values.getValue(parameterValueTokenizer.nextToken()));
		}

		String updateMomsOnError = properties.get(documentName + "_UPDATE_OMS_ON_ERROR");
		if (StringUtils.isNotBlank(updateMomsOnError)) {
			docConfig.setUpdateOmsParametersOnError(Boolean.parseBoolean(updateMomsOnError));
		}

		if (docConfig.isPreformattedDocument() && docConfig.isResponse())
			throw new DocumentConfigurationException(
					"Document '" + documentName + "', preformatted document cannot return formatted response.");
		return docConfig;
	}

	public boolean isPreformattedDocument() {
		return (this.controlJobTemplate != null);
	}

	public boolean isResponse() {
		return (!isPreformattedDocument()) && (this.returnPath != null);
	}

	/**
	 * This method will generate the list of actions to do with a document,
	 * depending on the current configuration. Most parameters values (template
	 * name, tonic path, return path, metadata values, etc...) are considererd as
	 * freemarker templates and parsed with a document dependant context. For
	 * instance, the template can contain a value such as
	 * /Invoices/template/${name}.template In the case, the ${name} parameter is the
	 * name of the document in the settings, ie "GazInvoice".
	 * 
	 * @param doc
	 * @return
	 * @throws FreeMarkerException
	 */
	public DocumentActions createDocumentActions(Document doc) throws FreeMarkerException {

		DocumentActions docActions = new DocumentActions();
		// TODO : fill docActions object
		Map<String, Object> context = FreeMarkerHelper.getContextFromDocument(doc);

		context.put("docNum", doc.getNum());
		context.put("time", System.currentTimeMillis());

		String template = this.getTemplate() != null
				? FreeMarkerHelper.parseExpression(this.getTemplate(), "template_" + doc.getName(), context)
				: null;
		context.put("template", template);
		docActions.setTemplate(template);

		String controlJobName = this.getControlJobTemplate() != null
				? FreeMarkerHelper.parseExpression(this.getControlJobTemplate(), "controlJob_" + doc.getName(), context)
				: null;
		context.put("controlJob", controlJobName);
		if (StringUtils.isNotBlank(controlJobName)) {
			docActions.setControlJob(controlJobName);
		} else if (doc.getMainDatasource().getType() == DatasourceType.MFD
				|| doc.getMainDatasource().getType() == DatasourceType.PDF) {
			docActions.setControlJob(doc.getName() + ".ftl");
		}

		String printer = this.getPrinter() != null
				? FreeMarkerHelper.parseExpression(this.getPrinter(), "printer_" + doc.getName(), context)
				: null;
		context.put("printer", printer);
		if (StringUtils.isBlank(printer) && docActions.isPreformattedDocument())
			printer = "OMS";// par défaut si document préformatté
		docActions.setPrinter(printer);

		String returnPath = this.getReturnPath() != null
				? FreeMarkerHelper.parseExpression(this.getReturnPath(), "returnPath_" + doc.getName(), context)
				: null;
		context.put("returnPath", returnPath);
		docActions.setReturnPath(returnPath);
		
		
		String mimeType = this.getMimeType() != null
				? FreeMarkerHelper.parseExpression(this.getMimeType(), "mimeType_" + doc.getName(), context)
				: null;
		context.put("mimeType", mimeType);
		docActions.setMimeType(mimeType);
		

		String tonicPath = this.getTonicPath() != null
				? FreeMarkerHelper.parseExpression(this.getTonicPath(), "tonicPath_" + doc.getName(), context)
				: null;
		context.put("tonicPath", tonicPath);
		docActions.setTonicPath(tonicPath);

		String xmlSplitter = this.xmlSplitter != null
				? FreeMarkerHelper.parseExpression(this.getXmlSplitter(), "xmlSplitter_" + doc.getName(), context)
				: null;
		context.put("xmlSplitter", xmlSplitter);
		docActions.setXmlSplitter(xmlSplitter);

		for (Entry<String, String> metadata : this.metadata.entrySet()) {
			String value = FreeMarkerHelper.parseExpression(metadata.getValue(),
					metadata.getKey() + "+" + doc.getName(), context);
			docActions.getMetadata().put(metadata.getKey(), value);
			context.put(metadata.getKey(), value);
		}

		Map<String, String> omsUpdateParameters = new HashMap<String, String>();
		context.put("omsUpdate", omsUpdateParameters);
		for (Entry<String, String> metadata : this.updateOmsParameters.entrySet()) {
			String value = FreeMarkerHelper.parseExpression(metadata.getValue(),
					"omsUpdate_" + metadata.getKey() + "+" + doc.getName(), context);
			docActions.getUpdateOmsParameters().put(metadata.getKey(), value);
			omsUpdateParameters.put(metadata.getKey(), value);
		}

		docActions.setUpdateOmsParametersOnError(isUpdateOmsParametersOnError());

		return docActions;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public File getPropertiesFile() {
		return propertiesFile;
	}

	public void setPropertiesFile(File propertiesFile) {
		this.propertiesFile = propertiesFile;
	}

	public List<String> getAdditionalDocumentFiles() {
		return additionalDocumentFiles;
	}

	public void setAdditionalDocumentFiles(List<String> additionalDocumentFiles) {
		this.additionalDocumentFiles = additionalDocumentFiles;
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	@Override
	public String toString() {
		StringWriter sw = new StringWriter();
		JAXB.marshal(this, sw);
		IOUtils.closeQuietly(sw);
		return sw.toString();
	}

	public boolean isUpdateOmsParametersOnError() {
		return updateOmsParametersOnError;
	}

	public void setUpdateOmsParametersOnError(boolean updateOmsParametersOnError) {
		this.updateOmsParametersOnError = updateOmsParametersOnError;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

}
