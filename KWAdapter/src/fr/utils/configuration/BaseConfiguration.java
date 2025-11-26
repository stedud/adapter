package fr.utils.configuration;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.coreoz.wisp.schedule.FixedDelaySchedule;
import com.coreoz.wisp.schedule.Schedule;

import de.kwsoft.mtext.util.misc.PasswordEncryptor;
import fr.kw.adapter.engine.purge.PurgeSettings;
import fr.utils.LogHelper;
import fr.utils.Utils;

public class BaseConfiguration extends FileConfiguration {

	public BaseConfiguration(File fileProperties) throws ConfigurationException {
		super(fileProperties);
		// TODO Auto-generated constructor stub
	}

	public String getKWUser() {
		return get("KW_USER");
	}

	public String getKWPlainPassword() {
		String pwd = get("KW_PASSWORD_PLAIN");
		if (StringUtils.isBlank(pwd)) {

			pwd = getKWCryptedPassword();
			if (StringUtils.isNotBlank(pwd)) {
				pwd = PasswordEncryptor.decode(pwd);
			}
		}

		return pwd;
	}

	public String getKWCryptedPassword() {
		return get("KW_PASSWORD_CRYPTED");
	}

	public String getServer() {
		return get("KW_SERVER");
	}

	public String getPort() {
		return get("KW_SERVER_PORT");
	}

	public boolean isSSLEnabled() {
		return Boolean.parseBoolean(this.get("SSL_ENABLED", "false"));
	}

	public String[] getUrlForJavaAPI() {
		String url = (isSSLEnabled() ? "https-remoting" : "http-remoting") + "://" + getServer()
				+ (StringUtils.isNotBlank(getPort()) ? ":" + getPort() : "");
		return new String[] { url };
	}

	public String[] getUrlForRestAPI() {
		String url = (isSSLEnabled() ? "https" : "http") + "://" + getServer()
				+ (StringUtils.isNotBlank(getPort()) ? ":" + getPort() : "");
		return new String[] { url };
	}

	public BaseConfiguration getMainConfiguration() {
		FileConfiguration root = this;

		while (StringUtils.isBlank(root.getLocalValue("KW_SERVER"))) {
			root = root.parent;
			if (root == null)
				return null;
		}
		if (root instanceof BaseConfiguration)
			return (BaseConfiguration) root;
		else
			try {
				return loadAsBaseConfiguration(root);
			} catch (ConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
	}

	public List<PurgeSettings> getPurgeSettings() {
		String[] purgeNames = getAll("PURGE");

		List<String> purgeNameList = new ArrayList();
		for (String purgeName : purgeNames) {
			String[] names = purgeName.split(",");
			for (String name : names) {
				if (!purgeNameList.contains(name))
					purgeNameList.add(name);
			}
		}

		List<PurgeSettings> settings = new ArrayList<PurgeSettings>();

		for (String purge : purgeNameList) {
			String folderName = get("PURGE_" + purge + "_FOLDER");
			if (folderName == null) {
				LogHelper.warn("PURGE_" + purge + "_FOLDER not set for purge " + purge);
				continue;
			}

			File folder = new File(folderName);
			String filters = get("PURGE_" + purge + "_FILTERS");
			if (filters == null) {
				LogHelper.warn("PURGE_" + purge + "_FILTERS not set for purge " + purge + ", using default *.tmp");
				filters = "*.tmp";
			}
			String[] filtersArr = filters.split(",");

			List<String> filtersList = new ArrayList<String>(filtersArr.length);
			for (String filter : filtersArr) {
				if (StringUtils.isNotBlank(filter)) {
					filtersList.add(filter);
				}
			}
			if (filtersList.size() == 0) {
				LogHelper.warn("PURGE_" + purge + "_FILTERS not set for purge " + purge + ", using default *.tmp");
				filtersList.add("*.tmp");
			}

			int days = Utils.getInt(get("PURGE_" + purge + "_DAYS"));

			if (days == 0) {
				days = 60;
			}
			Duration duration = Duration.ofDays(days);
			

			String lifeDurationStr = get("PURGE_" + purge + "_AGE","");

			if (StringUtils.isNotBlank(lifeDurationStr)) {
				try {
					duration = Utils.parseDuration(lifeDurationStr);
					
				} catch (Exception e) {
					LogHelper.warn("PURGE_" + purge + "_AGE='" + lifeDurationStr + "' for " + purge + " could not be parsed as Duration : " + e.getMessage(),e);
				}
				
			}
			
			LogHelper.info("Purge " + purge + " Lifetime=" + duration.toString());
			
			
			int hours = Utils.getInt(get("PURGE_" + purge + "_PERIOD_HOURS"));

			if (hours == 0) {
				hours = 12;
			}
			Duration durationSchedule = Duration.ofHours(hours);
			Schedule purgeSchedule = new FixedDelaySchedule(durationSchedule);
			
			String durationScheduleStr = get("PURGE_" + purge + "_SCHEDULE","");

			if (StringUtils.isNotBlank(durationScheduleStr)) {
				try {
					LogHelper.info("Purge " + purge + " Schedule=" + durationScheduleStr);		
					purgeSchedule = Utils.parseSchedule(durationScheduleStr);
					
				} catch (Exception e) {
					LogHelper.warn("PURGE_" + purge + "_PERIOD='" + durationScheduleStr + "' for " + purge + " could not be parsed as Duration : " + e.getMessage(),e);
				}
				
			}
			else
			{
				LogHelper.info("Purge " + purge + " Schedule=" + hours + "h");
			}
			
			
			

			boolean recursive = Boolean.parseBoolean(get("PURGE_" + purge + "_RECURSIVE"));

			PurgeSettings purgeSettings = new PurgeSettings(purge, folder);
			purgeSettings.setDuration(duration);
			purgeSettings.setFilters(filtersList);
			purgeSettings.setRecursive(recursive);
			purgeSettings.setScheduleDelay(purgeSchedule);

			settings.add(purgeSettings);
		}

		return settings;
	}

	public static BaseConfiguration loadAsBaseConfiguration(FileConfiguration config) throws ConfigurationException {
		return new BaseConfiguration(config.getPropertiesFile());
	}

}
