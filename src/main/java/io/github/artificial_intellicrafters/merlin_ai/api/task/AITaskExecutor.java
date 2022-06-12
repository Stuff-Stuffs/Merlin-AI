package io.github.artificial_intellicrafters.merlin_ai.api.task;

public interface AITaskExecutor {
	boolean submitTask(AITask task);

	void runTasks(int maxMillis);
}
