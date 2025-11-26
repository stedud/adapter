package fr.kw.adapter.engine.scanner;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

import com.coreoz.wisp.schedule.Schedule;

import de.kwsoft.mtext.util.misc.PasswordEncryptor;
import fr.kw.api.submit.SubmitConfiguration;
import fr.utils.LogHelper;
import fr.utils.Utils;
import fr.utils.configuration.ConfigurationException;
import fr.utils.configuration.FileConfiguration;

public class ScannerConfiguration extends SubmitConfiguration {

	protected boolean stopping = false;

	protected ScannerConfiguration(File fileProperties) throws ConfigurationException {
		super(fileProperties);
		// TODO Auto-generated constructor stub
	}

	public static ScannerConfiguration get(File fileProperties) throws ConfigurationException
	{
		if (!configurations.containsKey(fileProperties.getPath()))
		{
			FileConfiguration conf = new ScannerConfiguration(fileProperties);
			configurations.put(fileProperties.getPath(), conf);
		}
		return (ScannerConfiguration) configurations.get(fileProperties.getPath());
	}
	
	public String getFilter() {
		return this.get("FILTER");
	}

	public boolean backupInputFile() {
		return Boolean.parseBoolean(this.get("BACKUP", "false"));
	}

	public String getScanFolder() {
		return this.get("FOLDER");
	}

	public Schedule getSchedule() {
		try {
			return Utils.parseSchedule(this.get("SCHEDULE"));
		} catch (Exception e) {
			LogHelper.error(e.getMessage(), e);
		}
		return null;
	}

	public String getSystemUser() {
		return get("SYSTEM_USER");
	}

	public String getSystemPlainPassword() {
		String pwd = get("SYSTEM_USER_PASSWORD_PLAIN");
		if (StringUtils.isBlank(pwd)) {

			pwd = getKWCryptedPassword();
			if (StringUtils.isNotBlank(pwd)) {
				pwd = PasswordEncryptor.decode(pwd);
			}
		}

		return pwd;
	}

	public String getKWCryptedPassword() {
		return get("SYSTEM_USER_PASSWORD_CRYPTED");
	}

	public boolean isStopping() {
		return stopping;
	}

	public void setStopping(boolean stopping) {
		this.stopping = stopping;
	}

	public int getScannerThreadPoolSize(int defaultValue) {
		return this.getInt("FILE_THREADS", defaultValue);
	}

}
