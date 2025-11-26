package fr.kw.adapter.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import fr.utils.LogHelper;
import fr.utils.Threading.Task;
import fr.utils.configuration.BaseConfiguration;
import fr.utils.configuration.ConfigurationException;

public class LocalFilesSynchronizer extends Task {

	private static Random random = new Random(System.currentTimeMillis());

	private BaseConfiguration configuration;

	public LocalFilesSynchronizer(String name, BaseConfiguration configuration) {
		super(name);
		this.configuration = configuration;
	}

	public synchronized boolean synchronize() {
		System.out.println(this);
		try {
			configuration.refresh();
		} catch (ConfigurationException e2) {
			LogHelper.warn("Could not refresh " + configuration.getPropertiesFile() + " : " + e2.getMessage(),e2);
		}
		boolean ok = true;
		if (configuration.get("SYNCHRO_SETTINGS_FOLDER", null) != null) {
			String folderPath = configuration.get("SYNCHRO_SETTINGS_FOLDER");

			LogHelper.info("Synchronizing with folder " + folderPath);

			File settingsFolder = new File(folderPath);
			if (!settingsFolder.exists())
				return false;

			Collection<File> files = FileUtils.listFiles(settingsFolder, null, true);

			for (File file : files) {
				// LogHelper.info("found '" + file.getPath() + "'");
				boolean update = false; // true pour test..//false;
				if (!file.getName().startsWith(".")) {
					Path relativePath = settingsFolder.toPath().relativize(file.toPath());
					File target = relativePath.toFile();

					try {
						byte[] b = Files.readAllBytes(file.toPath());
						String hashFrom = new String(MessageDigest.getInstance("MD5").digest(b));
						if (target.exists()) {
							b = Files.readAllBytes(file.toPath());
							String hashTarget = new String(MessageDigest.getInstance("MD5").digest(b));
							update = hashTarget.compareTo(hashFrom) != 0;

						} else {
							update = true;
						}
						// System.out.println("Hash=" + hash);
					} catch (NoSuchAlgorithmException | IOException e) {
						LogHelper.warn("Could not get hash for '" + file.getPath() + "' : " + e.getMessage(), e);
						continue;
					}

					if (update) {

						// TODO contruire le nom du fichier cible = nom du fichier à copier en enlevant
						// la partie settingsFolder

						LogHelper.info("Updating '" + file.getPath() + "' to '" + target.getPath() + "'");
						FileInputStream fis = null;
						FileOutputStream fos = null;
						File tmpTarget = null;
						try {
							tmpTarget = File.createTempFile("settings", ".tmp");
							fos = new FileOutputStream(tmpTarget);
							fis = new FileInputStream(file);
							IOUtils.copy(fis, fos);
							IOUtils.closeQuietly(fos);
							IOUtils.closeQuietly(fis);
							int nbTries = 0;
							IOException e = null;
							boolean fileOk = false;
							do {
								nbTries++;
								try {
									if (target.canWrite())
									{
										Files.move(tmpTarget.toPath(), target.toPath(),
												StandardCopyOption.REPLACE_EXISTING);
										// if (hash != null) resourcesHash.put(file.getPath(), hash);
										LogHelper.info(target.getPath() + " updated (" + nbTries + " attempt(s))");
										e = null;
										fileOk = true;
									}
									else
									{
										throw new IOException("Write access denied to " + target );
									}
								} catch (IOException e1) {
									e = e1;
									LogHelper.warn(
											"Test " + nbTries + " for copying '" + target.getPath() + "' failed.");
									try {
										Thread.sleep(random.nextInt(1000));
									} catch (InterruptedException e2) {

									}
								}

							} while (!fileOk && e != null && nbTries <= 10);

							if (!fileOk)
								throw e;

						} catch (IOException e) {
							LogHelper.error("Could not save settings file '" + file.getPath() + "' to local folder : "
									+ e.getMessage(), e);
							ok = false;
						} finally {
							if (fos != null)
								IOUtils.closeQuietly(fos);
							if (fis != null)
								IOUtils.closeQuietly(fis);
							if (tmpTarget != null && tmpTarget.exists())
								tmpTarget.delete();
						}

					} else {
						LogHelper.info("No update for '" + target.getPath() + "' (unchanged)");
					}

				}

			}

			LogHelper.info("Synchronization done");

		}
		return ok;
	}

	@Override
	public void run() {

		synchronize();
	}

}
