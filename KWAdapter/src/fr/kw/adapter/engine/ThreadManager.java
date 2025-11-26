package fr.kw.adapter.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import fr.utils.LogHelper;
import fr.utils.Threading.ExecutorException;
import fr.utils.Threading.RunnableExecutor;

public class ThreadManager {

	public static final String FILES_THREAD_POOL = "filesThreadPool";
	public static final String DOCUMENTS_THREAD_POOL = "documentsThreadPool";

	private static Map<String, RunnableExecutor> executors = Collections
			.synchronizedMap(new HashMap<String, RunnableExecutor>());
	private static boolean threadsLocked = true;

	public static synchronized RunnableExecutor init(String name, int nbThreads) throws ExecutorException {
		LogHelper.info("Initiating ThreadManager " + name + " with " + nbThreads + " thread(s)");
		RunnableExecutor result = null;
		RunnableExecutor previous = executors.get(name);
		boolean previousRemoved = false;
		if (previous != null && previous.getNbThreads() != nbThreads && !threadsLocked) {
			executors.remove(name);
			previousRemoved = true;
		}
		if (previous == null || previousRemoved) {
			result = new RunnableExecutor(name, nbThreads, 1000 * nbThreads);
			executors.put(name, result);
		} else {
			result = previous;
		}

		if (previousRemoved) {
			previous.stop();
		}

		return result;
	}

	public String[] getExecutorNames() {
		return executors.entrySet().toArray(new String[] {});
	}

	public static RunnableExecutor geThreadExecutor(String name) {
		return executors.get(name);
	}

	public static boolean exists(String name) {
		return executors.containsKey(name);
	}

	public static void stopAll() {
		for (String threadName : executors.keySet()) {
			stop(threadName);
		}
	}

	public static void stop(String name) {

		RunnableExecutor executor = executors.get(name);
		if (executor != null) {
			executors.remove(name);
			Runnable stopTask = new Runnable() {
				
				@Override
				public void run() {
					executor.stop();
					
				}
			};
			Thread stopThread = new Thread(stopTask);
			stopThread.start();

		}

	}

	public static boolean isThreadsLocked() {
		return threadsLocked;
	}

	public static void setThreadsLocked(boolean threadsLocked) {
		ThreadManager.threadsLocked = threadsLocked;
	}

}
