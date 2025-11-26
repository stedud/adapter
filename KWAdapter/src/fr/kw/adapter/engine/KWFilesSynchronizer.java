package fr.kw.adapter.engine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import de.kwsoft.mtext.api.MTextException;
import de.kwsoft.mtext.api.Resource;
import de.kwsoft.mtext.api.ResourceInformation;
import fr.kw.api.resources.KWResources;
import fr.strs.utils.Util;
import fr.utils.LogHelper;
import fr.utils.Threading.Task;
import fr.utils.configuration.BaseConfiguration;
import fr.utils.configuration.ConfigurationException;

/**
 * This class must update local files with files from KW server. The project is
 * specified in the configuration file with the key KW_SETTINGS_PROJECT.
 * 
 * @author stedu
 *
 */
public class KWFilesSynchronizer extends Task {

	private BaseConfiguration configuration;
	protected Map<String, String> resourcesHash = Collections.synchronizedMap(new HashMap<String, String>());

	public KWFilesSynchronizer(String name, BaseConfiguration configuration) {
		super(name);
		this.configuration = configuration;
	}

	public synchronized void kwSynchronize() {
		try {
			configuration.refresh();
		} catch (ConfigurationException e2) {
			LogHelper.warn("Could not refresh " + configuration.getPropertiesFile() + " : " + e2.getMessage(), e2);
		}
		if (configuration.get("KW_SYNCHRO_SETTINGS_PROJECT", null) != null) {
			String project = configuration.get("KW_SYNCHRO_SETTINGS_PROJECT");

			LogHelper.info("Synchronizing with KW server, project " + project);

			KWResources kwResources;
			Collection<ResourceInformation> resourceInfoList = Collections.EMPTY_LIST;

			try {
				kwResources = new KWResources(configuration);
				LogHelper.info("Synchronizing with KW server, project, connected to server");
				LogHelper.info("Synchronizing with KW server, project, listing project content...");
				resourceInfoList = kwResources.listProjectResources(project);
				LogHelper.info("Synchronizing with KW server, project, content returned by server");
			} catch (Exception e1) {
				LogHelper.error("Could not list KW files from project " + project + " : " + e1.getMessage(), e1);
				return;
			}
			for (ResourceInformation resourceInfo : resourceInfoList) {
				File file = new File(resourceInfo.getProjectRelativeName().replace('\\', File.separatorChar));
				if (!file.getName().startsWith(".")) {
					String hash = kwResources.getResourceHash(resourceInfo);
					boolean download = false;

					if (hash != null) {

						if (StringUtils.compare(hash, resourcesHash.get(resourceInfo.getFullName())) != 0) {
							download = true;
						}
					} else {
						download = true;
					}

					if (download) {
						Resource resource;
						LogHelper.info( resourceInfo.getFullName() + " : modified");
						try {
							resource = kwResources.getResource(resourceInfo);
						} catch (MTextException e1) {
							LogHelper.error("Could not get file for KW resource '" + resourceInfo.getFullName() + "' : "
									+ e1.getMessage(), e1);
							continue;
						}

						if (file.getParent() != null)
							file.getParentFile().mkdirs();
						FileOutputStream fos = null;
						InputStream is = null;
						File tmp = null;
						try {
							fr.utils.Utils.getTmpFolder().mkdirs();
							tmp = File.createTempFile("settings", ".tmp");
							LogHelper.info("Downloading " + resourceInfo.getFullName() + " to " + tmp.getPath());
							fos = new FileOutputStream(tmp);
							is = resource.getStream();
							IOUtils.copy(is, fos);
							IOUtils.closeQuietly(fos);
							IOUtils.closeQuietly(is);
							LogHelper.info("Updating " + file.getPath() + " with " + tmp.getPath());
							

							boolean error = false;
							int attempts = 0;
							IOException lastError = null;
							do {
								try {
									attempts++;
									Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
								} catch (IOException e) {
									lastError = e;
									error = true;
									LogHelper.warn("Attempt " + attempts + " : Could not update " + file.getPath()
											+ " : " + e.getMessage());
									try {
										Thread.sleep(500);
									} catch (InterruptedException e1) {

									}
								}
							} while (error && attempts <= 5);

							if (lastError != null) {
								throw lastError;
							} else if (hash != null)
								resourcesHash.put(resourceInfo.getFullName(), hash);
							LogHelper.info(file.getPath() + " updated");

						} catch (IOException e) {
							LogHelper.error("Could not save kw file '" + file.getPath() + "' to local folder : "
									+ e.getMessage(), e);
						} finally {
							if (fos != null)
								IOUtils.closeQuietly(fos);
							if (is != null)
								IOUtils.closeQuietly(is);
							if (tmp != null && tmp.exists())
								tmp.delete();
						}

					}
				}
			}

			LogHelper.info("Synchronization done");
		}
	}

	@Override
	public void run() {

		kwSynchronize();
	}

}
