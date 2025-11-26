package fr.kw.adapter.engine.purge;

import java.io.File;
import java.io.FileFilter;

import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import fr.utils.LogHelper;
import fr.utils.LogPurgeHelper;

public class PurgeTask implements Runnable {

	private PurgeSettings settings;

	public PurgeTask(PurgeSettings settings) {
		this.settings = settings;
		LogPurgeHelper.info("Creating PURGE " + settings.getName());
	}

	@Override
	public void run() {
		LogPurgeHelper.info("Purge task " + settings.getName() + " fetch");
		cleanFolder(settings.folderToPurge);
		LogPurgeHelper.info("Purge task " + settings.getName() + " finished");
	}

	public void cleanFolder(File folder) {
		if (folder.getParent() == null) {
			LogPurgeHelper.warn("Cannot purge a root directory : " + folder);
			return;
		}
		File[] files = this.list(folder);
		if (files != null)
		for (File f : files) {
			if (f.isFile()) {
				LogPurgeHelper.info("Purge task " + settings.getName() + ", deleting " + f);
				f.delete();
			} else if (settings.recursive) {
				cleanFolder(f);
				String[] contents = f.list();
				
				if (contents != null && contents.length == 0 && f.compareTo(settings.folderToPurge) != 0) {
					LogPurgeHelper.info("Purge task " + settings.getName() + ", deleting folder " + f);
					f.delete();
				}
			}
		}

	}

	public File[] list(File folder) {
		WildcardFileFilter wildCardFileFilter = new WildcardFileFilter(settings.filters);
		AgeFileFilter ageFileFilter = new AgeFileFilter(System.currentTimeMillis() - settings.duration.toMillis());
		IOFileFilter dirFilter = FileFilterUtils.directoryFileFilter();

		AndFileFilter andfileFilter = new AndFileFilter();
		andfileFilter.addFileFilter(wildCardFileFilter);
		andfileFilter.addFileFilter(ageFileFilter);

		OrFileFilter fileFilter = new OrFileFilter();
		fileFilter.addFileFilter(andfileFilter);
		fileFilter.addFileFilter(dirFilter);

		return folder.listFiles((FileFilter) fileFilter);
	}

	public PurgeSettings getSettings() {
		return settings;
	}

	public void setSettings(PurgeSettings settings) {
		this.settings = settings;
	}

}
