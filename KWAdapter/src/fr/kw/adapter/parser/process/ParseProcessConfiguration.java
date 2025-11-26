package fr.kw.adapter.parser.process;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import fr.freemarker.FreeMarkerHelper;
import fr.kw.adapter.document.configuration.DocumentConfiguration;
import fr.kw.adapter.document.configuration.DocumentConfigurationException;
import fr.utils.Utils;
import fr.utils.configuration.BaseConfiguration;
import fr.utils.configuration.ConfigurationException;
import fr.utils.configuration.FileConfiguration;

public class ParseProcessConfiguration extends BaseConfiguration {

	protected Map<String, DocumentConfiguration> documentConfigurations = Collections
			.synchronizedMap(new HashMap<String, DocumentConfiguration>());

	protected ParseProcessConfiguration(File fileProperties) throws ConfigurationException {
		super(fileProperties);
		// TODO Auto-generated constructor stub
	}
	
	public static ParseProcessConfiguration get(File fileProperties) throws ConfigurationException
	{
		if (!configurations.containsKey(fileProperties.getPath()))
		{
			FileConfiguration conf = new ParseProcessConfiguration(fileProperties);
			configurations.put(fileProperties.getPath(), conf);
		}
		return (ParseProcessConfiguration) configurations.get(fileProperties.getPath());
	}

	public String getFilterType() {
		return this.get("FILTER_TYPE");
	}

	public List<File> getParsers() {
		List<File> parsers = new ArrayList<File>();

		List<String> parserNames = this.getList("PARSERS");
		if (parserNames == null)
			parserNames = Collections.EMPTY_LIST;

		for (String parserName : parserNames) {
			// if (Utils.isAbsolute(parserName))
			// {
			File f = new File(parserName);
			parsers.add(f);
			// }
			// else
			// {
			// path relative to the properties file folder
			// File f = new File(getPropertiesFile().getParentFile(), parserName);
			// parsers.add(f);
			// }
		}
		return parsers;
	}

	public String getEncoding() {
		return this.get("ENCODING", StandardCharsets.UTF_8.name());
	}

	public DocumentConfiguration getDocumentConfiguration(String documentName)
			throws ConfigurationException, DocumentConfigurationException {
		DocumentConfiguration docConfig = this.documentConfigurations.get(documentName);
		if (docConfig == null) {

			docConfig = DocumentConfiguration.load(this, documentName);
			String configFileName = this.get(documentName);
			if (StringUtils.isNotBlank(configFileName)) {
				File configFile = null;
				if (Utils.isAbsolute(configFileName)) {
					configFile = new File(configFileName);
				} else {
					configFile = new File(this.getPropertiesFile().getParent(), configFileName);
				}
				if (!configFile.exists()) {
					throw new DocumentConfigurationException(
							"Document configuration file '" + configFileName + "' not found");
				}
				FileConfiguration docConfigProps;
				try {
					docConfigProps = FileConfiguration.get(configFile);
				} catch (ConfigurationException e) {
					throw new ConfigurationException(e.getMessage(), e);
				}
				docConfig.overloadConfiguration(docConfigProps);
			}
			this.documentConfigurations.put(documentName, docConfig);
		}
		return docConfig;
	}

	public void cleanDocumentConfigurations() {// TODO : should take care of synchronization in case of concurrent
												// access...
		if (this.documentConfigurations != null)
			this.documentConfigurations.clear();
	}

	public File getEventStructure(String eventName) {
		File eventStructureFile = new File("dictionaries", eventName + ".event");

		return eventStructureFile;
	}

	@Override
	protected void load() throws ConfigurationException {
		// TODO Auto-generated method stub
		super.load();
		this.cleanDocumentConfigurations();
		FreeMarkerHelper.clearCache();
	}

}
