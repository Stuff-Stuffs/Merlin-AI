package io.github.artificial_intellicrafters.merlin_ai.api.location_caching;

import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutionContext;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import net.minecraft.block.BlockState;

public interface ValidLocationClassifier<T> {
	T classify(int x, int y, int z, ShapeCache cache, AITaskExecutionContext executionContext);

	void rebuild(BlockState[] updateBlockStates, short[] updatedPositions, int updateCount, int chunkSectionX, int chunkSectionY, int chunkSectionZ, int offsetX, int offsetY, int offsetZ, RebuildConsumer<T> consumer, ShapeCache cache, AITaskExecutionContext executionContext);

	interface RebuildConsumer<T> {
		void update(T val, int x, int y, int z);
	}
}
