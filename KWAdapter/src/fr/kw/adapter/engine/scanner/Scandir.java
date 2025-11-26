package fr.kw.adapter.engine.scanner;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;

import fr.kw.adapter.document.Document;
import fr.kw.adapter.engine.ProcessFile;
import fr.kw.adapter.engine.ThreadManager;
import fr.kw.api.submit.Status;
import fr.kw.api.submit.Submit;
import fr.utils.LogHelper;
import fr.utils.Utils;
import fr.utils.Threading.ExecutorException;
import fr.utils.Threading.Task;
import fr.utils.configuration.BaseConfiguration;
import fr.utils.configuration.ConfigurationException;

public class Scandir implements Runnable {
	protected File errorFolder = new File(Submit.BAK_FOLDER, Submit.ERRORS_FOLDER);
	protected File bakFolder = new File(Submit.BAK_FOLDER);
	// protected File folder;
	protected File folderProcessing;
	protected File folderProcessed;
	protected String filter;
	protected boolean deleteFiles = true;
	protected String name;
	private ScannerConfiguration configuration;
	protected BaseConfiguration rootConfiguration;

	private boolean called = false;
	private File folder;
	// private FileSystemManager fsManager;
	private String scanFolderName;

	public Scandir(ScannerConfiguration configuration, BaseConfiguration rootConfiguration)
			throws ConfigurationException {
		if (configuration == null)
			throw new ConfigurationException("Configuration for ScanDir cannot be null");

		this.rootConfiguration = rootConfiguration;

		this.configuration = configuration;
		this.name = FilenameUtils.getBaseName(configuration.getPropertiesFile().getName());
		this.init();

	}

	protected void init() throws ConfigurationException {
		try {

			if (!StringUtils.isBlank(configuration.get("BAK_FOLDER"))) {
				bakFolder = new File(configuration.get("BAK_FOLDER"));
				if (!StringUtils.isBlank(configuration.get("ERROR_FOLDER"))) {
					errorFolder = new File(configuration.get("ERROR_FOLDER"));
				} else {
					errorFolder = new File(bakFolder, "errors");
				}
			} else {
				if (!StringUtils.isBlank(configuration.get("ERROR_FOLDER"))) {
					errorFolder = new File(configuration.get("ERROR_FOLDER"));
				}
			}
			deleteFiles = Boolean.parseBoolean(configuration.get("DELETE_FILES", "true"));

			scanFolderName = this.configuration.getScanFolder();

			LogHelper.info("Creating scan folder object from '" + scanFolderName + "'");
			folder = new File(scanFolderName);

			if (!folder.exists())
				folder.mkdirs();

			if (StringUtils.isBlank(configuration.get("PROCESSING_FOLDER"))) {
				this.folderProcessing = new File("processing");
				this.folderProcessing = new File(this.folderProcessing, this.name);
			} else {
				this.folderProcessing = new File(configuration.get("PROCESSING_FOLDER"));
			}

			this.folderProcessing.mkdirs();

			this.filter = this.configuration.getFilter();

		} finally {

		}
	}

	@Override
	public void run() {

		if (configuration.isStopping())
			return;

		try {
		//	LogHelper.info("Fetch " + this.name);
//			if (configuration.refresh()) {
//				LogHelper.info("Configuration " + configuration.getPropertiesFile() + " changed.");
//				return;
//				this.init();
//				ThreadManager.init(ThreadManager.DOCUMENTS_THREAD_POOL, configuration.getDocumentThreadPoolSize(1));
//				ThreadManager.init(ThreadManager.FILES_THREAD_POOL, configuration.getScannerThreadPoolSize(1));
//				LogHelper.info("Configuration files reloaded");
//			}

			Thread t = new Thread(new Runnable() {
				
				@Override
				public void run() {
					try {
						configuration.refresh();
					} catch (ConfigurationException e) {
						LogHelper.warn("Could not check refresh for " + configuration.getPropertiesFile());
					}
					
				}
			});
			t.start();
			
			if (!called) {
				called = true;
				LogHelper.info("Scanner '" + configuration.getPropertiesFile().getPath() + "' scanning '"
						+ this.configuration.getScanFolder() + "'");
			}

			WildcardFileFilter wildCardFileFilter = new WildcardFileFilter(this.filter);
			File[] files = this.folder.listFiles((FileFilter) wildCardFileFilter);

			if (files == null) {
				// LogHelper.warn("Could not scan '" + folder + "'");
				return;
			}

			Map<File, Long> fileSizes = new HashMap<>(files.length);
			
			for (File f : files) {
				if (f.isFile())
					

				if (!f.isFile() || !f.canRead() || !f.canWrite())
					continue;

		

				long fileSize = f.length();
				if (fileSize <= 0)
				{
					LogHelper.info("Scanner '" + this.configuration.getPropertiesFile().getPath() + ", file " + f.getPath() + " is 0kb : ignored");
					continue;
				}
				fileSizes.put(f, f.length());
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e3) {
				// TODO Auto-generated catch block
				e3.printStackTrace();
			}

			for (File f : files)
			{
			
				long newFileSize = f.length();

				
				if (fileSizes.get(f) != newFileSize) {
					LogHelper.info("Scanner '" + this.configuration.getPropertiesFile().getPath() + ", file " + f.getPath() + " has changed : ignored");
					continue;
				}
				LogHelper.info("Scanner '" + this.configuration.getPropertiesFile().getPath() + "' found '"
						+ f.getPath() + "' for processing");
				File fProcessing = new File(f.getPath() + ".processing");
				
				if (! f.renameTo(fProcessing))
				{
					LogHelper.info("Scanner '" + this.configuration.getPropertiesFile().getPath() + "' can't rename '"
							+ f.getPath() + "'. File will be retried at next loop.");
					continue;
				}
				else
				{
					LogHelper.info("Scanner '" + this.configuration.getPropertiesFile().getPath() + "', '"
							+ f.getPath() + "' renamed to '"  + fProcessing.getPath() + "'.");
					
				}
				
				LogHelper.info("Scanner '" + this.configuration.getPropertiesFile().getPath() + "' will process '"
						+ f.getPath() + "'");

				if (!folderProcessing.exists())
					folderProcessing.mkdirs();
				File processingFile = new File(folderProcessing, f.getName());
				if (processingFile.exists())
					processingFile.delete();

				/*
				 * int i=1; while (processingFile.exists()) { processingFile = new
				 * File(folderProcessing, FilenameUtils.getBaseName(getFileName(f)) + "." + i +
				 * "." + FilenameUtils.getExtension(getFileName(f)) ); i++; }
				 */

				File finalProcessingFile = processingFile;
				File backupFile = null;
				try {
					if (configuration.backupInputFile()) {
						try {
						bakFolder.mkdirs();
						File backupFolder = new File(bakFolder, this.name);
						backupFolder.mkdirs();
						backupFile = new File(backupFolder, f.getName());
						if (backupFile.exists())
							backupFile.delete();
						LogHelper.info("Scanner " + this.name + " saving " + f.getName() + " to " + backupFile.getPath());
						FileUtils.copyFile(fProcessing, backupFile, false);
						}
						catch (Exception e)
						{
							LogHelper.error("Could not backup unput file " + f.getPath() + " to " + backupFile.getPath() + " : " + e.getMessage());
						}
					}
					FileOutputStream fos = new FileOutputStream(finalProcessingFile);
					FileInputStream fis = new FileInputStream(fProcessing);

					IOUtils.copy(fis, fos);
					fos.flush();
					IOUtils.closeQuietly(fis);
					IOUtils.closeQuietly(fos);


					if (this.deleteFiles) {
						if (fProcessing.exists())
						{
							LogHelper.info("Deleting '" + fProcessing.getName() + "'");
							if (!fProcessing.delete()) {
								finalProcessingFile.delete();
								// if (backupFile != null) backupFile.delete();
								// Fichier non terminé ?
								LogHelper.info(Scandir.class.getSimpleName() + ", Could not delete '" + fProcessing.getName() + "'");
								continue;
	
							}
						}
					}
				} catch (IOException e2) {
					LogHelper.warn(
							"Could not move '" + fProcessing + "' to '" + folderProcessing.getPath() + "' : " + e2.getMessage(),
							e2);

					finalProcessingFile.delete();
					// if (backupFile != null) backupFile.delete();
					// Fichier non terminé ?

					continue;
				}

				Task runnable = new Task(f.getPath()) {

					@Override
					public void run() {
						this.started = true;

						ProcessFile processFile = new ProcessFile();
						processFile.setErrorFolder(errorFolder);
						processFile.setRootConfiguration(rootConfiguration);
						processFile.setConfigurationFile(configuration.getPropertiesFile());
						processFile.setInputFile(finalProcessingFile);
						try {
							LogHelper.info("processing " + getName());
							List<Document> documents = processFile.call();
							if (hasError(documents)) {
								throw new Exception("Error(s) detected during documents processing : "
										+ getErrorMessages(documents));
							} else {
								LogHelper.info(getName() + " processed.");
							}
						} catch (Throwable e) {
							LogHelper.error("Error when processing " + getName() + " : " + e.getMessage(), e);

							boolean useFilePath = Boolean.parseBoolean(configuration.get("ERRORS_WITH_RELATIVE_PATH", "false"));
							File errorFile = useFilePath ? new File(errorFolder, getName()) : new File(errorFolder, FilenameUtils.getName(getName()));
							File logFile = new File(errorFolder, (useFilePath ? getName():FilenameUtils.getName(getName())) + ".log");
							errorFolder.mkdirs();
							try {
								if (finalProcessingFile.exists())
								{
									LogHelper.info("Saving file processed with errors to " + errorFile.getPath());
									FileUtils.copyFile(finalProcessingFile, errorFile);
								}
								else
								{
									LogHelper.info("Processing final deleted.");
								}
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
								LogHelper.info("Could not create file : " + errorFile.getPath() + " : " + e1.getMessage());
							}
							String errorText = Utils.getStackTrace(e);
							try {
								LogHelper.info("Writing error log file : " + logFile.getPath());
								
								FileUtils.writeStringToFile(logFile, errorText);
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
								LogHelper.info("Could not write log file : " + logFile.getPath() + " : " + e.getMessage());
							}
						} finally {
							if (finalProcessingFile.exists())
							{
								if (!finalProcessingFile.delete()) {
									LogHelper.warn(Scandir.class.getSimpleName() +", Task " + this.getName() + ", Could not delete " + finalProcessingFile.getPath());
								}
							}
							processFile.getInputFile().delete();
						}

						this.terminated = true;
					}
				};
				LogHelper.info("Enqueueing " + runnable.getName() + " for processing");
				ThreadManager.geThreadExecutor(ThreadManager.FILES_THREAD_POOL).enqueueTask(runnable);
				

			}

		} catch (Throwable e4) {
			// TODO Auto-generated catch block
			e4.printStackTrace();
			LogHelper.error("Error during scanner loop '" + name + "' : " + e4.getMessage(), e4);
		} finally {

		}

	}

	@Override
	public String toString() {
		return "Scandir [folder=" + folder + ", filter=" + filter + "]";
	}

	protected boolean hasError(List<Document> docs) {
		boolean errors = false;
		for (Document doc : docs) {
			if (doc.getDocumentActions().getGlobalStatus() == Status.KO) {
				errors = true;
				break;
			}
		}
		return errors;
	}

	protected String getErrorMessages(List<Document> docs) {
		StringBuffer sb = new StringBuffer();

		for (Document doc : docs) {
			if (doc.getDocumentActions().getGlobalStatus() == Status.KO) {
				sb.append("DOCUMENT ");
				sb.append(doc.getId());
				sb.append(" : \n");
				for (String msg : doc.getDocumentActions().getErrorMessages()) {
					sb.append(StringUtils.abbreviate(msg, 256));
					sb.append("\n");
				}

			}
		}
		return StringUtils.abbreviate(sb.toString(), 256 * 10);
	}

	protected static String getFileName(FileObject file) {
		FileName name = file.getName();

		return name.getBaseName();// + (StringUtils.isNotBlank(name.getExtension()) ? "." + name.getExtension() :
									// "");
	}
}
