package fr.kw.adapter.document;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;

import org.apache.commons.lang3.StringUtils;

import fr.freemarker.FreeMarkerHelper;
import fr.kw.adapter.document.configuration.DocumentConfiguration;
import fr.kw.api.submit.Status;

public class DocumentActions {

	/**
	 * Name without extension of a freemarker template in folder
	 * {@link FreeMarkerHelper#XF_FOLDER} If the document has to be sent directly to
	 * MOMS (no tonic template), a control job is built using this template. In case
	 * the document layout is made with a Tonic template (see
	 * {@link DocumentConfiguration#mtextSettings}, this field is ignored.
	 */
	protected String controlJob;

	/**
	 * The full path to the template. ex: /Invoices/template/invoice.template
	 */
	protected String template;

	/**
	 * The full path to the splitter configuration if a splitter must be called.
	 */
	protected String xmlSplitter;

	protected boolean splitted = false;

	/**
	 * Indicate the moms printer or null. Default moms printer name should be OMS.
	 */
	protected String printer;
	protected Status printStatus = Status.PENDING;
	protected List<Long> printIds = new ArrayList<Long>();
	/**
	 * Indicate if the update stage must be done if errors occured during the job
	 * (if a document of the job is on error, none document will be updated).
	 */
	protected boolean updateOmsParametersOnError = true;

	/**
	 * Indicates the full path where the document should be created in Tonic. No
	 * document is created if the value is not set.
	 */
	protected String tonicPath;

	protected Status tonicStatus = Status.PENDING;
	protected String tonicPathResult = null;

	protected File controlJobFile = null;

	protected List<Throwable> exceptions = new ArrayList<Throwable>(1);

	@XmlElement(name = "errorMessages")
	protected List<String> errorMessages = new ArrayList<String>(1);

	/**
	 * Indicates the full path for the response. The file extension (usually .pdf)
	 * indicates the renderer to use. This option works for single documents only,
	 * it will be ignored if a splitter is set.
	 */
	protected String returnPath;
	
	protected String mimeType = "application/pdf";

	protected Status returnStatus = Status.PENDING;

	protected boolean embedData = false;

	protected String embedDataPath = null;

	/**
	 * Metadata for the document.
	 */
	protected Map<String, String> metadata = new HashMap<String, String>();

	/**
	 * OMS parameters to update, once all the documents of the batch have been sent
	 * to OMS.
	 */
	protected LinkedHashMap<String, String> updateOmsParameters = new LinkedHashMap<String, String>();

	public DocumentActions() {
		// TODO Auto-generated constructor stub
	}

	public Status getPrinterStatus() {
		return printStatus;
	}

	public void setPrinterStatus(Status printerStatus) {
		this.printStatus = printerStatus;
	}

	public Status getTonicStatus() {
		return tonicStatus;
	}

	public void setTonicStatus(Status tonicStatus) {
		this.tonicStatus = tonicStatus;

	}

	public Status getReturnStatus() {
		return returnStatus;
	}

	public void setReturnStatus(Status returnStatus) {
		this.returnStatus = returnStatus;

	}

	public Status getGlobalStatus() {
		Status global = Status.PENDING;
		boolean hasOK = false;
		boolean hasKO = false;
		boolean hasIgnore = false;

		if (getPrinterStatus() == Status.KO)
			hasKO = true;
		else if (getPrinterStatus() == Status.OK)
			hasOK = true;
		else if (getPrinterStatus() == Status.IGNORE)
			hasIgnore = true;

		if (getReturnStatus() == Status.KO)
			hasKO = true;
		else if (getReturnStatus() == Status.OK)
			hasOK = true;
		else if (getReturnStatus() == Status.IGNORE)
			hasIgnore = true;

		if (getTonicStatus() == Status.KO)
			hasKO = true;
		else if (getTonicStatus() == Status.OK)
			hasOK = true;
		else if (getTonicStatus() == Status.IGNORE)
			hasIgnore = true;

		if (hasOK)
			global = Status.OK;
		if (hasKO)
			global = Status.KO;
		if (hasIgnore)
			global = Status.IGNORE;

		return global;
	}

	public boolean isPreformattedDocument() {
		return StringUtils.isNotBlank(this.controlJob);
	}

	public boolean isResponseOnly() {
		return (!isPreformattedDocument()) && (this.printer == null) && (this.tonicPath == null)
				&& (this.returnPath != null);
	}

	public boolean isResponse() {
		return (!isPreformattedDocument()) && (this.returnPath != null);
	}

	public boolean isPrint() {
		return StringUtils.isNotBlank(printer);
	}

	public boolean isSaveInTonic() {
		return StringUtils.isNotBlank(tonicPath);
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public LinkedHashMap<String, String> getUpdateOmsParameters() {
		return updateOmsParameters;
	}

	public String getControlJob() {
		return controlJob;
	}

	public void setControlJob(String controlJobKey) {
		this.controlJob = controlJobKey;
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

	public List<Throwable> getExceptions() {
		return exceptions;
	}

	public List<String> getErrorMessages() {
		return errorMessages;
	}

	public List<Long> getPrintIds() {
		return printIds;
	}

	public void addPrintId(long printId) {
		this.printIds.add(printId);
	}

	public String getTonicPathResult() {
		return tonicPathResult;
	}

	public void setTonicPathResult(String tonicPathResult) {
		this.tonicPathResult = tonicPathResult;
	}

	public boolean isUpdateOmsParametersOnError() {
		return updateOmsParametersOnError;
	}

	public void setUpdateOmsParametersOnError(boolean updateOmsParametersOnError) {
		this.updateOmsParametersOnError = updateOmsParametersOnError;
	}

	public boolean isSplitted() {
		return splitted;
	}

	public void setSplitted(boolean splitted) {
		this.splitted = splitted;
	}

	public File getControlJobFile() {
		return controlJobFile;
	}

	public void setControlJobFile(File controlJobFile) {
		this.controlJobFile = controlJobFile;
	}

	public boolean isEmbedData() {
		return embedData;
	}

	public void setEmbedData(boolean embedData) {
		this.embedData = embedData;
	}

	public String getEmbedDataPath() {
		return embedDataPath;
	}

	public void setEmbedDataPath(String embedDataPath) {
		this.embedDataPath = embedDataPath;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
}
