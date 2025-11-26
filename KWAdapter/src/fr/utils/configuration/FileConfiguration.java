package fr.utils.configuration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;

import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.lang3.StringUtils;

import fr.utils.LogHelper;
import fr.utils.Values;

public class FileConfiguration extends Observable {

	protected FileBasedConfigurationBuilder<FileBasedConfiguration> builder;
	protected FileBasedConfiguration configuration;
	protected File propertiesFile;
	protected long timstamp = 0;
	protected static Map<String, FileConfiguration> configurations = new HashMap<String, FileConfiguration>();

	protected FileConfiguration parent;

	/**
	 * Load a properties file as a {@link FileBasedConfiguration}. Adds convenient
	 * methods to : - get a list of values from a key (separator ';') - indicate a
	 * default String value
	 * 
	 * @param fileProperties
	 * @throws ConfigurationException
	 */
	protected FileConfiguration(File fileProperties) throws ConfigurationException {
		if (fileProperties == null)
			throw new ConfigurationException("Properties File cannot be null");
		this.propertiesFile = fileProperties;
		this.load();

	}
	
	
	public static FileConfiguration get(File fileProperties) throws ConfigurationException
	{
		if (! configurations.containsKey(fileProperties.getPath()))
		{
			FileConfiguration conf = new FileConfiguration(fileProperties);
			configurations.put(fileProperties.getPath(), conf);
		}
		return configurations.get(fileProperties.getPath());
	}

	protected void load() throws ConfigurationException {
		boolean errors = false;
		Parameters params = new Parameters();

		builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
				.configure(params.fileBased().setEncoding(StandardCharsets.UTF_8.name()).setFile(this.propertiesFile)
						.setListDelimiterHandler(new DefaultListDelimiterHandler(';')));
		try {
			configuration = builder.getConfiguration();

			String parentConfigFile = this.get("PARENT");
			if (StringUtils.isNotBlank(parentConfigFile)) {
				File parentConfig = new File(parentConfigFile);
				if (parentConfig.exists()) {
					this.parent = FileConfiguration.get(parentConfig);
				} else {

					//errors = true;
					LogHelper.error("Missing parent configuration file '" + this.get("PARENT") + "'");
					// throw new ConfigurationException("Missing parent configuration file '" +
					// this.get("PARENT") + "'");
				}
			}
			
			if (StringUtils.isNotBlank(this.get("LOG_LEVEL", "")))
			{
				try {
					LogHelper.setLevel(Level.parse(this.get("LOG_LEVEL")));
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} catch (org.apache.commons.configuration2.ex.ConfigurationException e) {
			errors = true;
			LogHelper.error(e.getMessage(), e);

		}
		if (!errors)
		{
			long newTimestamp = this.propertiesFile.lastModified();
			LogHelper.debug(this.propertiesFile + " configuration file, time stamp set to " + newTimestamp + " (previous was " + this.timstamp + ")");
			this.timstamp = newTimestamp;
		}
	}

	public boolean refresh() throws ConfigurationException {
		return this.refresh(false);
	}

	public boolean refresh(boolean forceRefresh) throws ConfigurationException {

		boolean changed = false;
		if (this.hasChanged() || forceRefresh) {

			this.load();
			
			LogHelper.info(this.propertiesFile.getPath() + " (re)loaded, lastModified timestamp=" + this.timstamp);
			changed = true;
		}
		if (parent != null && parent.refresh(forceRefresh)) {
//			changed = true;
		}
		
		if (changed)
			{
				this.setChanged();
				this.notifyObservers();
				
				LogHelper.info("Configuration " + this.propertiesFile + " has changed, Observers notified");
			}

		//
		return changed;
	}

	public boolean hasChanged() {
		if (!this.propertiesFile.exists())
			return false;
		return this.propertiesFile.lastModified() != this.timstamp;
	}

	/**
	 * Returns a list of String value. In the properties file, values are delimited
	 * with ";". If the entry is not found, an empty list is returned.
	 * 
	 * @param key
	 * @return
	 */
	public List<String> getList(String key) {
		if (configuration == null)
			return Collections.emptyList();

		List<String> values = configuration.getList(String.class, key);
		if ((values == null || values.size() == 0) && parent != null) {
			values = parent.getList(key);
		}
		if (values == null)
			values = Collections.emptyList();

		return values;
	}

	public FileBasedConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Returns a String value for the entered key. Will NOT search parent configs is
	 * no value was found. If the key is not found, a null value is returned.
	 * 
	 * @param key
	 * @return
	 */
	public String getLocalValue(String key) {

		return Values.getValue(this.getConfiguration().getString(key));
	}

	/**
	 * Returns a String value for the entered key. Will search parent configs is no
	 * value was found. If the key is not found, a null value is returned.
	 * 
	 * @param key
	 * @return
	 */
	public String get(String key) {

		return Values.getValue(this.get(key, null));
	}

	/**
	 * Returns a String value for the entered key. Will search parent configs is no
	 * value was found. If the key is not found, the indicated default value is
	 * returned.
	 * 
	 * @param key
	 * @return
	 */

	public String get(String key, String defaultValue) {
		if (configuration == null)
			return defaultValue;
		String localValue = this.getConfiguration().getString(key);
		if (StringUtils.isBlank(localValue)) {
			if (parent != null)
				return parent.get(key, defaultValue);
			else
				return defaultValue;
		} else
			return localValue;
	}

	/**
	 * Returns a String value for the entered key. Will search parent configs is no
	 * value was found. If the key is not found, the indicated default value is
	 * returned.
	 * 
	 * @param key
	 * @return
	 */

	public String[] getAll(String key) {

		if (configuration == null)
			return new String[] {};

		List<String> allValues = new ArrayList<String>();

		String localValue = Values.getValue(this.getConfiguration().getString(key));
		if (!StringUtils.isBlank(localValue)) {
			allValues.add(localValue);
		}

		FileConfiguration currentParent = parent;

		while (currentParent != null) {
			String val = currentParent.getLocalValue(key);
			if (!StringUtils.isBlank(val)) {
				if (!allValues.contains(val)) {
					allValues.add(val);
				}
			}
			currentParent = currentParent.getParent();

		}

		return allValues.toArray(new String[] {});
	}

	/**
	 * Returns the original properties file.
	 * 
	 * @return
	 */
	public File getPropertiesFile() {
		return propertiesFile;
	}

	public List<String> getKeys() {
		if (configuration == null)
			return Collections.EMPTY_LIST;
		Map<String, String> keysMap = new HashMap<String, String>();
		List<String> keys = new ArrayList<String>();
		Iterator<String> keysIter = this.configuration.getKeys();
		while (keysIter.hasNext()) {
			String key = keysIter.next();
			keysMap.put(key, key);
		}
		if (parent != null) {
			List<String> parentKeys = parent.getKeys();

			for (String key : parentKeys)
				keysMap.put(key, key);
		}

		keys.addAll(keysMap.values());
		return keys;
	}

	public int getInt(String key, int defaultValue) {
		if (configuration == null)
			return defaultValue;

		String valStr = this.get(key, String.valueOf(defaultValue));
		int val = defaultValue;

		try {
			val = Integer.parseInt(valStr);

			return val;
		} catch (NumberFormatException e) {
			LogHelper.warn("Could not parse " + key + " value : " + e.getMessage());
			return defaultValue;
		}
	}

	@Override
	public synchronized void addObserver(Observer o) {
		// TODO Auto-generated method stub
		super.addObserver(o);
		if (this.parent != null)
			this.parent.addObserver(o);
	}

	@Override
	public synchronized void deleteObserver(Observer o) {
		// TODO Auto-generated method stub
		super.deleteObserver(o);
		if (this.parent != null)
			this.parent.deleteObserver(o);
	}

	@Override
	public synchronized void deleteObservers() {
		// TODO Auto-generated method stub
		super.deleteObservers();
		if (this.parent != null)
			this.parent.deleteObservers();
	}

	public FileConfiguration getParent() {
		return parent;
	}

	@Override
	public String toString() {
		return "FileConfiguration [propertiesFile=" + propertiesFile + ", parent=" + parent + "]";
	}


	
}
