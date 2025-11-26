package fr.kw.adapter.document;

import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlElement;

import org.apache.commons.io.IOUtils;

import de.kwsoft.mtext.mffmfd.PageInfo;
import de.kwsoft.mtext.mffmfd.embed.MfdForEmbeddedDocument;
import fr.kw.adapter.parser.type.ParserType;
import fr.utils.Utils;

public class Document {

	/**
	 * Indicate the document type (ex: Invoice). This name is used to retrieve the
	 * settings related to this document type in the configuration files. ex:
	 * documentName.printer=OMS documentName.project=Invoices
	 * documentName.template=templates\invoice.template etc...
	 */
	protected String name;

	private static long serial = 0;

	/**
	 * Indicate the environment (DEV/PROD/QUAL/etc...) where the document was
	 * produced
	 */
	protected String environment = "";
	/**
	 * The origin of the input data that was at the origin of this document. ex :
	 * the input file name
	 */
	protected String origin;
	/**
	 * The type origin of the input data that was at the origin of this document. ex
	 * : xml, csv, pdf, otf, rdi, etc...
	 */

	protected ParserType originType;

	/**
	 * Document number in the original file. Does not apply to documents from XML
	 * splitted.
	 */
	protected long num = 1;

	/**
	 * An id to identify the document.
	 */
	private String id;

	protected DocumentActions documentActions = null;

	/**
	 * The other data sources of this document.
	 * 
	 */
	@XmlElement(name = "additionalDatasources")
	protected Map<String, Datasource> additionalDatasources = new HashMap<String, Datasource>();

	/**
	 * Additional XML contexts of this document.
	 * 
	 */
	@XmlElement(name = "additionalContexts")
	protected Map<String, Datasource> additionalContexts = new HashMap<String, Datasource>();

	/**
	 * Document metadata. In pdf/otf context, these metadata are injected in the
	 * freemarker context to build the control job file.
	 *
	 */
	@XmlElement(name = "metadata")
	protected Map<String, String> metadata = new HashMap<String, String>();

	/**
	 * The main datasource for this document. In case of preformated document (pdf,
	 * otf), this will contain MFD format. Otherwise it should be XML data.
	 */

	protected Datasource mainDatasource;

	/**
	 * The pageInfo list is returned
	 * {@link MfdForEmbeddedDocument#createMfdForEmbeddedDocument(java.io.File, String, java.io.File)}
	 * for PDFs or it is created "manually" for OTFs. It's a list of page
	 * information necessary to create the Control Job file when we process
	 * preformatted documents such as PDF or OTF. This list is null by default and
	 * must be set for preformatted documents sent to MOMS.
	 * 
	 */
	protected List<PageInfo> pageInfos = null;

	private String date;

	private String time;

	public Document() {
		// TODO Auto-generated constructor stub
		Date d = new Date();
		this.date = Utils.formatDate(d);
		this.time = Utils.formatTime(d);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public ParserType getOriginType() {
		return originType;
	}

	public void setOriginType(ParserType originType) {
		this.originType = originType;
	}

	public String getId() {
		if (id == null)
			generateId();

		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Map<String, Datasource> getAdditionalDatasources() {
		return additionalDatasources;
	}

	public Datasource getMainDatasource() {
		return mainDatasource;
	}

	public void setMainDatasource(Datasource mainDatasource) {
		this.mainDatasource = mainDatasource;
	}

	public List<PageInfo> getPageInfos() {
		return pageInfos;
	}

	public void setPageInfos(List<PageInfo> pageInfos) {
		this.pageInfos = pageInfos;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public void generateId() {
		this.id = this.getOrigin() + "_" + this.getName();// + "_" + String.format("%1$010d", nextId());
	}

	protected synchronized long nextId() {
		serial++;
		return serial;
	}

	public DocumentActions getDocumentActions() {
		return documentActions;
	}

	public void setDocumentActions(DocumentActions documentActions) {
		this.documentActions = documentActions;
	}

	public long getNum() {
		return num;
	}

	public void setNum(long num) {
		this.num = num;
	}

	public Map<String, Datasource> getAdditionalContexts() {
		return additionalContexts;
	}

	@Override
	public String toString() {
		StringWriter sw = new StringWriter();
		JAXB.marshal(this, sw);
		IOUtils.closeQuietly(sw);
		return sw.toString();
	}

	public String getDate() {
		return date;
	}

	public String getTime() {
		return time;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public void clean() {
		if (this.mainDatasource != null)
			this.mainDatasource.clean();
		if (this.additionalDatasources != null) {
			for (Datasource ds : this.additionalDatasources.values()) {
				ds.clean();
			}
		}
	}

}
