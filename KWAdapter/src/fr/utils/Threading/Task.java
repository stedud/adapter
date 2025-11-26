package fr.utils.Threading;

import java.util.List;

public abstract class Task implements Runnable {

	protected boolean started = false;
	protected boolean terminated = false;
	protected String name;

	public Task(String name) {
		this.name = name;

	}

	public boolean isStarted() {
		return started;
	}

	public boolean isTerminated() {
		return terminated;
	}

	public static boolean allTerminated(List<Task> tasks) {
		if (tasks == null)
			return true;

		for (Task task : tasks) {
			if (!task.terminated)
				return false;
		}

		return true;
	}

	public static void waitAllTerminated(List<Task> tasks) {
		while (!allTerminated(tasks)) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public String getName() {
		return name;
	}

}
