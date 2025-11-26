package fr.kw.adapter.engine.scanner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;

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
import jcifs.CIFSContext;
import jcifs.CIFSException;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import net.idauto.oss.jcifsng.vfs2.provider.SmbFileSystemConfigBuilder;

public class ScandirVFS implements Runnable {
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
	private FileObject folder;
	private FileSystemManager fsManager;
	private String scanFolderName;

	public ScandirVFS(ScannerConfiguration configuration, BaseConfiguration rootConfiguration)
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
			if (!Utils.isAbsolute(scanFolderName)) {
				File scanFolderFile = new File(scanFolderName);
				scanFolderName = scanFolderFile.toURI().toString();
			}

			String user = configuration.getSystemUser();
			String password = configuration.getSystemPlainPassword();
			String domain = configuration.get("SYSTEM_USER_DOMAIN");

			StaticUserAuthenticator auth = null;
			if (StringUtils.isNotBlank(user)) {
				auth = new StaticUserAuthenticator(domain, user, password);

			}

			// jcifs configuration
			Properties jcifsProperties = new Properties();

			// these settings are needed for 2.0.x to use anything but SMB1, 2.1.x enables
			// by default and will ignore
			jcifsProperties.setProperty("jcifs.smb.client.enableSMB2", "true");
			jcifsProperties.setProperty("jcifs.smb.client.useSMB2Negotiation", "true");

			CIFSContext jcifsContext = new BaseContext(new PropertyConfiguration(jcifsProperties));

			// pass in both to VFS
			FileSystemOptions options = new FileSystemOptions();
			if (auth != null)
				DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(options, auth);
			SmbFileSystemConfigBuilder.getInstance().setCIFSContext(options, jcifsContext);

			fsManager = VFS.getManager();
			if (!fsManager.hasProvider("smb"))
				throw new RuntimeException("Provide missing Samba");

			LogHelper.info("Creating scan folder object from '" + scanFolderName + "'");
			folder = fsManager.resolveFile(scanFolderName, options);

			if (!folder.exists())
				folder.createFolder();

			if (StringUtils.isBlank(configuration.get("PROCESSING_FOLDER"))) {
				this.folderProcessing = new File("processing");
				this.folderProcessing = new File(this.folderProcessing, this.name);
			} else {
				this.folderProcessing = new File(configuration.get("PROCESSING_FOLDER"));
			}

			this.folderProcessing.mkdirs();

			this.filter = this.configuration.getFilter();

		} catch (CIFSException | FileSystemException e) {
			throw new ConfigurationException(
					"Error during initialization for scan '" + this.configuration.getPropertiesFile().getPath()
							+ "' scanning in '" + this.configuration.getScanFolder() + "'");
		} finally {

		}
	}

	@Override
	public void run() {

		if (configuration.isStopping())
			return;

		try {

			if (configuration.refresh()) {
				return;//let the StartScanners reload and relaunch the scanner
//				this.init();
//				ThreadManager.init(ThreadManager.DOCUMENTS_THREAD_POOL, configuration.getDocumentThreadPoolSize(1));
//				ThreadManager.init(ThreadManager.FILES_THREAD_POOL, configuration.getScannerThreadPoolSize(1));
//				LogHelper.info("Configuration files reloaded");
			}

			if (!called) {
				called = true;
				LogHelper.info("Scanner '" + configuration.getPropertiesFile().getPath() + "' scanning '"
						+ this.configuration.getScanFolder() + "'");
			}

			this.folder.refresh();
			FileObject[] files = this.folder.findFiles(new FileSelector() {

				@Override
				public boolean traverseDescendents(FileSelectInfo fileInfo) throws Exception {

					return fileInfo.getFile().compareTo(folder) == 0;
				}

				@Override
				public boolean includeFile(FileSelectInfo fileInfo) throws Exception {
					return FilenameUtils.wildcardMatchOnSystem(getFileName(fileInfo.getFile()), filter);

				}
			});

			/*
			 * File[] files = folder.listFiles(new FilenameFilter() {
			 * 
			 * @Override public boolean accept(File arg0, String arg1) { return
			 * FilenameUtils.wildcardMatchOnSystem(arg1, filter); } });
			 */
			if (files == null) {
				LogHelper.warn("Could not scan '" + folder + "'");
				return;
			}

			List<Task> tasks = new ArrayList<Task>();
			for (FileObject f : files) {
				if (!f.isFile() || !f.isReadable() || !f.isWriteable())
					continue;

				long fileSize = Utils.getFileSize(f);
				if (fileSize < 0)
					continue;

				try {
					Thread.sleep(250);
				} catch (InterruptedException e3) {
					// TODO Auto-generated catch block
					e3.printStackTrace();
				}

				long newFileSize = Utils.getFileSize(f);

				if (fileSize != newFileSize) {
					continue;
				}

				LogHelper.info("Scanner '" + this.configuration.getPropertiesFile().getPath() + "' found '"
						+ getFileName(f) + "'");

				if (!folderProcessing.exists())
					folderProcessing.mkdirs();
				File processingFile = new File(folderProcessing, getFileName(f));
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

					FileOutputStream fos = new FileOutputStream(finalProcessingFile);
					FileContent content = f.getContent();
					IOUtils.copy(f.getContent().getInputStream(), fos);
					content.close();
					fos.close();

					if (configuration.backupInputFile()) {
						bakFolder.mkdirs();
						File backupFolder = new File(bakFolder, this.name);
						backupFolder.mkdirs();
						backupFile = new File(backupFolder, f.getName().getBaseName());
						if (backupFile.exists())
							backupFile.delete();
						FileUtils.copyFile(finalProcessingFile, backupFile, false);
					}
					if (this.deleteFiles) {
						if (f.exists())
						{
							LogHelper.info("Deleting '" + f.getName() + "'");
							if (!f.delete()) {
								finalProcessingFile.delete();
								// if (backupFile != null) backupFile.delete();
								// Fichier non terminé ?
								LogHelper.info(this.getClass().getSimpleName() + ", Could not delete '" + f.getName() + "'");
								continue;
	
							}
						}
					}
				} catch (IOException e2) {
					LogHelper.warn("Could not move '" + getFileName(f) + "' to '" + folderProcessing.getPath() + "' : "
							+ e2.getMessage(), e2);

					finalProcessingFile.delete();
					// if (backupFile != null) backupFile.delete();
					// Fichier non terminé ?

					continue;
				}

				Task runnable = new Task(getFileName(f)) {

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

							File errorFile = new File(errorFolder, getName());
							File logFile = new File(errorFolder, getName() + ".log");
							errorFolder.mkdirs();
							try {
								FileUtils.copyFile(finalProcessingFile, errorFile);
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							String errorText = Utils.getStackTrace(e);
							try {
								FileUtils.writeStringToFile(logFile, errorText);
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						} finally {
							if (finalProcessingFile.exists())
							{
								if (!finalProcessingFile.delete()) {
									LogHelper.warn(ScandirVFS.class.getSimpleName() + ", Task " + this.getName() + ", Could not delete " + finalProcessingFile.getPath());
								}
							}
						}

						this.terminated = true;
					}
				};

				tasks.add(runnable);
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
