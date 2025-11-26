package fr.kw.adapter.engine.scanner;

import java.io.File;
import java.io.FilenameFilter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.lang3.StringUtils;

import com.coreoz.wisp.Job;
import com.coreoz.wisp.JobStatus;
import com.coreoz.wisp.Scheduler;
import com.coreoz.wisp.SchedulerConfig;
import com.coreoz.wisp.SchedulerConfig.SchedulerConfigBuilder;
import com.coreoz.wisp.schedule.FixedDelaySchedule;
import com.coreoz.wisp.schedule.Schedule;
import com.coreoz.wisp.schedule.Schedules;

import fr.kw.adapter.engine.KWFilesSynchronizer;
import fr.kw.adapter.engine.LocalFilesSynchronizer;
import fr.kw.adapter.engine.ProcessFile;
import fr.kw.adapter.engine.ThreadManager;
import fr.kw.adapter.engine.purge.PurgeSettings;
import fr.kw.adapter.engine.purge.PurgeTask;
import fr.kw.api.submit.SubmitConfiguration;
import fr.utils.LogHelper;
import fr.utils.Utils;
import fr.utils.Threading.ExecutorException;
import fr.utils.configuration.ConfigurationException;
import fr.utils.configuration.FileConfiguration;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "start")
public class StartScanners implements Callable<String>, Observer {

	protected static int defaultThreads = 20;
	protected List<ScannerConfiguration> scannerConfigurations = new ArrayList<ScannerConfiguration>();
	public static final File SCANNERS_FOLDER = new File("scanners");
	@Option(names = "-documentThreads", required = false)
	protected int nbDocumentThreads = 1;

	@Option(names = "-fileThreads", required = false)
	protected int nbFilesThreads = 1;

	@Option(names = "-config", required = false)
	protected File configurationFile = null;

	protected SubmitConfiguration initialConfiguration = null;
	private Scheduler scheduler;
	private KWFilesSynchronizer kwSettingsSynchronizer;
	private Schedule synchronizerSchedule;

	private String kwSettingsProject;
	private String kwSettingsScheduleStr;
	private Job jobKWSettingsSynchronizer;
	private String localFolderSettingsScheduleStr;
	private Schedule localFolderSynchronizerSchedule;
	private LocalFilesSynchronizer localFolderSettingsSynchronizer;
	private Job jobLocalFolderSettingsSynchronizer;
	private String localFolderSettingsProject;
	static StartScanners startScanner;
	private Map<String,Job> purgeJobs = new HashMap<String, Job>();
	private Map<String,Job> scanJobs = new HashMap<String, Job>();

	public static void main(String[] args) {
		boolean stop = false;
		boolean start = false;
		startScanner = new StartScanners();
		ArrayList<String> argsList = new ArrayList<String>(args.length);

		for (String arg : args) {
			if (arg.compareToIgnoreCase("stop") == 0)
				stop = true;
			else if (arg.compareToIgnoreCase("start") == 0)
				start = true;
			else {
				argsList.add(arg);
			}
		}

		if (stop) {
			stopService(null);
		} else {
			CommandLine cmdLine = new CommandLine(startScanner);
			cmdLine.execute(argsList.toArray(new String[] {}));
		}

	}

	public StartScanners() {
		ProcessFile.setDaemon(true);
		String tmp = System.getenv("java.io.tmpdir");
		if (StringUtils.isNotBlank(tmp)) {
			File tmpFolder = new File(tmp);
			tmpFolder.mkdirs();
		}

	}

	@Override
	public String call() throws Exception {
		
		int nbThreads  = defaultThreads;
		
		//scheduler = new Scheduler();
		if (configurationFile != null) {
			initialConfiguration = SubmitConfiguration.get(configurationFile);
			
			nbThreads  = initialConfiguration.getInt("BACKGROUND_TASKS_MAX_THREADS", defaultThreads);
			SchedulerConfig schedConf = SchedulerConfig.builder().maxThreads(nbThreads).build();
			scheduler = new Scheduler(schedConf);
			
			
			FileConfiguration conf = initialConfiguration;
			
			while (conf != null)
			{
				conf.addObserver(this);
				LogHelper.info(conf.getPropertiesFile() + " added StartScanners as Observer");
				conf = conf.getParent();
			}
			
				
				
				
			
			if (StringUtils.isNotBlank(initialConfiguration.get("KW_SYNCHRO_SETTINGS_PROJECT"))) {
				initKWSynchronization(initialConfiguration);
				initialConfiguration.refresh();
			}
			if (StringUtils.isNotBlank(initialConfiguration.get("SYNCHRO_SETTINGS_FOLDER"))) {
				initFolderSynchronization(initialConfiguration);
				initialConfiguration.refresh();
			}
			initPurge(initialConfiguration, true);
		} else {
			LogHelper.error("No configuration file set for StartScanners");
			SchedulerConfig schedConf = SchedulerConfig.builder().maxThreads(nbThreads).build();
			scheduler = new Scheduler(schedConf);
		}
		initScanners();
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

			@Override
			public void run() {
				LogHelper.warn("Shutting down, waiting for all schedulers to terminate.");
				for (ScannerConfiguration config : scannerConfigurations) {
					config.setStopping(true);
				}
				scheduler.gracefullyShutdown(Duration.ofMinutes(10));

			}
		}));

		return null;
	}

	/**
	 * @throws ConfigurationException
	 * @throws ExecutorException
	 */
	protected void initScanners() throws ConfigurationException, ExecutorException {
		if (!SCANNERS_FOLDER.exists())
			SCANNERS_FOLDER.mkdirs();

		File[] scanConfigs = SCANNERS_FOLDER.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return FilenameUtils.wildcardMatch(name, "*.properties", IOCase.INSENSITIVE);
			}
		});
		
		
		Map<String, Job> updated = new HashMap<String, Job>();
		
		for (File scanConfigFile : scanConfigs) {
			ScannerConfiguration scanConfig = new ScannerConfiguration(scanConfigFile);
			scanConfig.addObserver(this);
			LogHelper.info(scanConfig.getPropertiesFile() + " added StartScanners as Observer");
			scannerConfigurations.add(scanConfig);
			nbDocumentThreads = scanConfig.getDocumentThreadPoolSize(nbDocumentThreads);
			nbFilesThreads = scanConfig.getScannerThreadPoolSize(nbFilesThreads);
			
			if (!ThreadManager.exists(ThreadManager.DOCUMENTS_THREAD_POOL)) {
				ThreadManager.init(ThreadManager.DOCUMENTS_THREAD_POOL,
						scanConfig.getDocumentThreadPoolSize(nbDocumentThreads));
			}
			if (!ThreadManager.exists(ThreadManager.FILES_THREAD_POOL)) {
				ThreadManager.init(ThreadManager.FILES_THREAD_POOL,
						scanConfig.getScannerThreadPoolSize(nbFilesThreads));
			}

			Schedule schedule = scanConfig.getSchedule();

			if (schedule != null) {
				LogHelper.info("Starting scandir " + scanConfigFile.getPath());
				Scandir scandir = new Scandir(scanConfig, initialConfiguration);
				Optional<Job> previousJob = scheduler.findJob(scanConfigFile.getPath());
				boolean start = true;
				long initialDelay = 0;
				if (previousJob != null && previousJob.isPresent())
				{
					start = false;
					LogHelper.info("Stopping previous scanner " + previousJob.get().name());
					long currentDelay = previousJob.get().lastExecutionEndedTimeInMillis() - System.currentTimeMillis();//temps depuis la dernière exécution
					//temps théorique jusqu'à la prochaine execution
					long newDelay = schedule.nextExecutionInMillis(System.currentTimeMillis(), 0, previousJob.get().nextExecutionTimeInMillis()) - System.currentTimeMillis();
					
					//Temps jusquà la prochaine exécution
					initialDelay = Math.max(0, newDelay-currentDelay);	
					CompletionStage<Job> result = scheduler.cancel(previousJob.get().name());
					
					LogHelper.info(previousJob.get().name() + " status after cancel : " + previousJob.get().status());
					int nbTries = 0;
					while (previousJob.get().status() == JobStatus.RUNNING && nbTries < 120)
					{
						LogHelper.warn("Scheduler " + previousJob.get().name() + " did not stop. Retrying...");
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						scheduler.cancel(previousJob.get().name());
						nbTries++;
					}
					if (previousJob.get().status() == JobStatus.RUNNING)
					{
						LogHelper.warn("Scheduler " + previousJob.get().name() + " did not stop. Aborting refresh.");
					}
					else
					{
						start = true;
						LogHelper.info("Scheduler " + previousJob.get().name() + " stopped.");
					}
				}
				
				if (start)
				{
					schedule = Schedules.afterInitialDelay(schedule, Duration.ofMillis(initialDelay));
					
					Job scannerJob = scheduler.schedule(scanConfigFile.getPath(), scandir, schedule);
					scanJobs.put(scanConfigFile.getPath(), scannerJob);
					updated.put(scanConfigFile.getPath(), scannerJob);
					LogHelper.info("Scheduler " + scanConfigFile.getPath() + " started.");
				}
				else
				{
					LogHelper.warn("Scheduler " + scanConfigFile.getPath() + " could not be started.");
				}
			}

		}
		List<String> toRemove = new ArrayList<String>();
		for (Entry<String, Job> scanJob : scanJobs.entrySet())
		{
			if (! updated.containsKey(scanJob.getKey()))
			{
				scheduler.cancel(scanJob.getKey());
				toRemove.add(scanJob.getKey());
			}
		}
		
		for (String key : toRemove)
		{
			scanJobs.remove(key);
		}
	}

	private void initFolderSynchronization(SubmitConfiguration configuration) {
		String taskName = "localFolderSettingsSynchronizer";

		localFolderSettingsProject = configuration.get("SYNCHRO_SETTINGS_FOLDER");
		if (StringUtils.isNotBlank(localFolderSettingsProject)) {
			localFolderSettingsScheduleStr = configuration.get("SYNCHRO_SETTINGS_FOLDER_SCHEDULE", "10mn");
			localFolderSynchronizerSchedule = null;
			try {
				localFolderSynchronizerSchedule = Utils.parseSchedule(localFolderSettingsScheduleStr);
			} catch (Exception e) {
				LogHelper.warn("Unable to parse synchronization settings schedule '" + localFolderSettingsScheduleStr
						+ "' : " + e.getMessage(), e);
				localFolderSynchronizerSchedule = new FixedDelaySchedule(Duration.ofMinutes(10));
			}

			localFolderSettingsSynchronizer = new LocalFilesSynchronizer(taskName, configuration);
			localFolderSettingsSynchronizer.synchronize();

			jobLocalFolderSettingsSynchronizer = scheduler.schedule(localFolderSettingsSynchronizer.getName(),
					localFolderSettingsSynchronizer, localFolderSynchronizerSchedule);

		}

	}

	protected synchronized void initKWSynchronization(SubmitConfiguration configuration) {

		String taskName = "settingsSynchronizer";

		kwSettingsProject = configuration.get("KW_SYNCHRO_SETTINGS_PROJECT");
		if (StringUtils.isNotBlank(kwSettingsProject)) {
			kwSettingsScheduleStr = configuration.get("KW_SYNCHRO_SETTINGS_SCHEDULE", "10mn");
			synchronizerSchedule = null;
			try {
				synchronizerSchedule = Utils.parseSchedule(kwSettingsScheduleStr);
			} catch (Exception e) {
				LogHelper.warn("Unable to parse synchronization settings schedule '" + kwSettingsScheduleStr + "' : "
						+ e.getMessage(), e);
				synchronizerSchedule = new FixedDelaySchedule(Duration.ofMinutes(10));
			}

			kwSettingsSynchronizer = new KWFilesSynchronizer(taskName, configuration);
			kwSettingsSynchronizer.kwSynchronize();

			jobKWSettingsSynchronizer = scheduler.schedule(kwSettingsSynchronizer.getName(), kwSettingsSynchronizer,
					synchronizerSchedule);

		}

	}

	protected synchronized void initPurge(SubmitConfiguration configuration, boolean immediatePurge) {

		
		Map<String,Job> updated = new HashMap<String, Job>();
		
		List<PurgeSettings> purgeSettings = configuration.getPurgeSettings();
		{
			for (PurgeSettings settings : purgeSettings) {
				
				PurgeTask purgeTask = new PurgeTask(settings);
				if (immediatePurge) {
					Thread t = new Thread(purgeTask);
					t.run();
				}
				Schedule schedule = settings.getScheduleDelay();
				Optional<Job> previousJob = scheduler.findJob(settings.getName());
				
				long initialDelay = 0;
				if (previousJob != null && previousJob.isPresent())
				{
					long currentDelay = previousJob.get().lastExecutionEndedTimeInMillis() - System.currentTimeMillis();//temps depuis la dernière exécution
					//temps théorique jusqu'à la prochaine execution
					long newDelay = schedule.nextExecutionInMillis(System.currentTimeMillis(), 0, previousJob.get().nextExecutionTimeInMillis()) - System.currentTimeMillis();
					
					//Temps jusquà la prochaine exécution
					initialDelay = Math.max(0, newDelay-currentDelay);	
					scheduler.cancel(previousJob.get().name());
				}
				
				schedule = Schedules.afterInitialDelay(schedule, Duration.ofMillis(initialDelay));
				
				
				
				
				Job purgeJob = scheduler.schedule(settings.getName(), purgeTask, schedule);
				purgeJobs.put(purgeTask.getSettings().getName(), purgeJob);
				updated.put(purgeTask.getSettings().getName(), purgeJob);

			}

		}
		
		List<String> toRemove = new ArrayList<String>();
		for (Entry<String, Job> job : purgeJobs.entrySet())
		{
			if (! updated.containsKey(job.getKey()))
			{
				scheduler.cancel(job.getKey());
			}
		}
		
		for (String key : toRemove)
		{
			purgeJobs.remove(key);
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		//

		LogHelper.info("StartSCanners notified for changes in " + o);
		try {
			if (o != initialConfiguration) this.initialConfiguration.refresh();
		} catch (ConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		if (o instanceof SubmitConfiguration) {

			
			if (jobKWSettingsSynchronizer != null) {
				SubmitConfiguration configuration = (SubmitConfiguration) o;
				String project = configuration.get("KW_SYNCHRO_SETTINGS_PROJECT");
				String scheduleStr = configuration.get("KW_SYNCHRO_SETTINGS_SCHEDULE", "10mn");
				if (StringUtils.compare(kwSettingsScheduleStr, scheduleStr) != 0
						|| StringUtils.compare(kwSettingsProject, project) != 0) {
					scheduler.cancel(jobKWSettingsSynchronizer.name());
					initKWSynchronization(configuration);

				}

			}
			if (jobLocalFolderSettingsSynchronizer != null) {
				SubmitConfiguration configuration = (SubmitConfiguration) o;
				String localFolder = configuration.get("SYNCHRO_SETTINGS_FOLDER");
				String scheduleStr = configuration.get("SYNCHRO_SETTINGS_FOLDER_SCHEDULE", "10mn");
				if (StringUtils.compare(localFolderSettingsScheduleStr, scheduleStr) != 0
						|| StringUtils.compare(localFolderSettingsProject, localFolder) != 0) {
					scheduler.cancel(jobKWSettingsSynchronizer.name());
					initKWSynchronization(configuration);

				}

			}
		


			initPurge((SubmitConfiguration) o, false);
			
			
				try {
					initScanners();
				} catch (ConfigurationException e) {
					LogHelper.error("Could not refresh scanners : " + e.getMessage(),e);
				} catch (ExecutorException e) {
					LogHelper.error("Could not refresh scanners : " + e.getMessage(),e);
				}
			
			
			
		}

	}

	public static void stopService(String[] args) {
		try {
			ThreadManager.stopAll();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Runtime.getRuntime().exit(0);

	}
}
