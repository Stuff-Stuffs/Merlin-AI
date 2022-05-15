package io.github.artificial_intellicrafters.merlin_ai.api.task;

public interface AITask {
	default int priority() {
		return 1;
	}

	boolean done();

	void runIteration();
}
