package fr.kw.api.submit;

import java.io.File;
import java.net.MalformedURLException;
import java.util.logging.Level;

import fr.kw.adapter.parser.process.ParseProcessConfiguration;
import fr.kw.api.rest.moms.MomsClientAPI;
import fr.kw.api.rest.mtext.MtextClientAPI;
import fr.utils.configuration.ConfigurationException;
import fr.utils.configuration.FileConfiguration;

public class SubmitConfiguration extends ParseProcessConfiguration {

	protected SubmitConfiguration(File fileProperties) throws ConfigurationException {
		super(fileProperties);
		// TODO Auto-generated constructor stub
	}

	public static SubmitConfiguration get(File fileProperties) throws ConfigurationException
	{
		if (!configurations.containsKey(fileProperties.getPath()))
		{
			FileConfiguration conf = new SubmitConfiguration(fileProperties);
			configurations.put(fileProperties.getPath(), conf);
		}
		return (SubmitConfiguration) configurations.get(fileProperties.getPath());
	}
	
	public Level getLogLevel() {
		return Level.parse(this.get("LOG_LEVEL", Level.INFO.getName()));
	}

	public int getDocumentThreadPoolSize(int defaultValue) {
		return getInt("DOCUMENT_THREADS", defaultValue);

	}

	public String getJobStatusMetadata() {
		return get("JOB_SUBMIT_STATUS_METADATA");
	}

	public String getDocStatusMetadata() {
		return get("DOC_SUBMIT_STATUS_METADATA");
	}

	public int getProcessorNumber() {

		return this.getInt("PROCESSOR_NUMBER", 1);
	}

	public int getBunchSize() {
		return this.getInt("BUNCH_SIZE", 1);
	}

	public boolean isParseOnly() {
		return Boolean.parseBoolean(this.get("PARSE_ONLY", "false"));
	}

	public MomsClientAPI getMomsClient() throws MalformedURLException {
		MomsClientAPI momsClient = null;

		momsClient = new MomsClientAPI(this.getUrlForRestAPI());

		return momsClient;
	}

	public MtextClientAPI getMtextClient() throws MalformedURLException {

		MtextClientAPI mtextClient = new MtextClientAPI(this.getUrlForRestAPI());

		return mtextClient;
	}

	public boolean isKeepParsed() {

		return Boolean.parseBoolean(this.get("KEEP_PARSED", "false"));
	}

}
