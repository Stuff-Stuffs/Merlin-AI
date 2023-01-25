package io.github.artificial_intellicrafters.merlin_ai.api;

import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutor;

public interface AIWorld {
	AITaskExecutor merlin_ai$getTaskExecutor();

	ChunkPathingInfo merlin_ai$getChunkGraph();
}
