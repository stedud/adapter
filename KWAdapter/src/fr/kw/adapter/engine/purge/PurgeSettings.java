package fr.kw.adapter.engine.purge;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.coreoz.wisp.schedule.FixedDelaySchedule;
import com.coreoz.wisp.schedule.Schedule;

public class PurgeSettings {
	protected String name;
	protected File folderToPurge;
	protected Duration duration = Duration.ofDays(30);// Par défaut
	protected Schedule scheduleDelay = new FixedDelaySchedule(Duration.ofHours(1));// Par défaut
	protected List<String> filters = new ArrayList<String>();
	protected boolean recursive = false;

	public PurgeSettings(String name, File folder) {
		this.name = name;
		this.folderToPurge = folder;
	}

	public Duration getDuration() {
		return duration;
	}

	public void setDuration(Duration duration) {
		this.duration = duration;
	}

	public List<String> getFilters() {
		return filters;
	}

	public void setFilters(List<String> filters) {
		this.filters = filters;
	}

	public boolean isRecursive() {
		return recursive;
	}

	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}

	public File getFolderToPurge() {
		return folderToPurge;
	}

	public String getName() {
		return name;
	}

	public Schedule getScheduleDelay() {
		return scheduleDelay;
	}

	public void setScheduleDelay(Schedule scheduleDelay) {
		this.scheduleDelay = scheduleDelay;
	}

}
