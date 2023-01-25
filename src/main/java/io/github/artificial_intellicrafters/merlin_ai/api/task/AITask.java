package io.github.artificial_intellicrafters.merlin_ai.api.task;

import org.slf4j.Logger;

public interface AITask {
	boolean done();

	/**
	 * Calling this function should return fast, if needed only performing part of a computation. May be run in parallel.
	 */
	void runIteration();

	/**
	 * Calling this function should flush changes to the world. Should only be called once done() is returns true. Guaranteed to be run on the main thread in serial.
	 */
	void runFinish();

	void cancel();

	Logger logger();
}
