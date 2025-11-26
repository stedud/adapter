package fr.utils.Threading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import fr.utils.LogHelper;

public class RunnableExecutor {

	protected String name;
	protected int nbThreads = 1;
	protected int queueSize = Integer.MAX_VALUE;
	private LinkedBlockingQueue<Runnable> queue;
	private ExecutorService executor;

	private boolean stopping = false;

	public RunnableExecutor(String name, int nbThreads, int maxQueueSize) throws ExecutorException {
		this.name = name;
		this.nbThreads = nbThreads;
		this.queueSize = maxQueueSize;
		init();

	}

	protected void init() throws ExecutorException {
		queue = new LinkedBlockingQueue<Runnable>();
		start();

	}

	public synchronized boolean canAcceptNewTasks() {
		return queue.size() < queueSize && !stopping;
	}

	public synchronized boolean hasPendingTasks() {
		return queue.size() > 0;
	}

	public synchronized boolean enqueueTask(Task task) {
		if (executor == null)
			try {
				start();
			} catch (ExecutorException e1) {
				LogHelper.error("Could not start Thread pool '" + name + "' : " + e1.getMessage(), e1);
				return false;
			}

		while (!canAcceptNewTasks()) {
			if (stopping)
				return false;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			LogHelper.debug("ThreadExecutor '" + name + "' waiting for avalaible thread...");
		}
		executor.execute(task);
		return true;
	}

	protected synchronized void start() throws ExecutorException {
		if (stopping)
			throw new ExecutorException("Thread executor '" + name + "' is stopping and cannot be started any more");
		LogHelper.info("Starting thread pool '" + name + "'");
		executor = new ThreadPoolExecutor(nbThreads, nbThreads, 10L, TimeUnit.SECONDS, queue);
	}

	public synchronized void stop() {
		this.stopping = true;
		if (executor != null) {
			LogHelper.info("Stopping thread pool '" + name + "', will wait up to 10 minutes");
			try {
				executor.shutdown();
				executor.awaitTermination(10, TimeUnit.MINUTES);
				LogHelper.info("Thread pool '" + name + "' stopped");
			} catch (InterruptedException e) {
				LogHelper.warn("Thread pool '" + name + "', did not terminate within 10 minutes");
			}
		}
	}

	public int getNbThreads() {
		return nbThreads;
	}

}
