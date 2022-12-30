package io.github.artificial_intellicrafters.merlin_ai.api.task;

import java.util.Optional;

public interface AITaskExecutor {
	Optional<AITaskExecutionContext> submitTask(AITask task);

	Optional<AITaskExecutionContext> submitTaskBefore(AITask task, AITaskExecutionContext executionContext);

	Optional<AITaskExecutionContext> submitTaskAfter(AITask task, AITaskExecutionContext executionContext);

	void runTasks(int maxMillis);
}
