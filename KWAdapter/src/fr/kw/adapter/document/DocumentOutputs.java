package fr.kw.adapter.document;

import java.io.File;

import fr.kw.api.submit.Status;

public class DocumentOutputs {

	protected File controlJob;

	/**
	 * The full path to the template. ex: /Invoices/template/invoice.template
	 */
	protected String template;

	/**
	 * The full path to the splitter configuration if a splitter must be called.
	 */
	protected String xmlSplitter;

	/**
	 * Indicate the moms printer or null. Default moms printer name should be OMS.
	 */
	protected String printer;

	protected Status printerStatus = Status.PENDING;

	/**
	 * Indicates the full path where the document should be created in Tonic. No
	 * document is created if the value is not set.
	 */
	protected String tonicPath;

	protected Status tonicPathStatus = Status.PENDING;

	/**
	 * Indicates the full path for the response. The file extension (usually .pdf)
	 * indicates the renderer to use. This option works for single documents only,
	 * it will be ignored if a splitter is set.
	 */
	protected String returnPath;

	protected Status returnPathStatus = Status.PENDING;

	public DocumentOutputs() {
		// TODO Auto-generated constructor stub
	}

}
