package io.github.artificial_intellicrafters.merlin_ai.api.location_caching;

import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.Optional;

public interface ValidLocationClassifier<T> {
	default Optional<T> fill(final ChunkSectionPos pos, final ShapeCache cache) {
		return Optional.empty();
	}

	default T postProcess(final T defaultVal, final int x, final int y, final int z, final ShapeCache cache) {
		return defaultVal;
	}

	T classify(int x, int y, int z, ShapeCache cache);

	void rebuild(BlockState[] updateBlockStates, short[] updatedPositions, int updateCount, int chunkSectionX, int chunkSectionY, int chunkSectionZ, int offsetX, int offsetY, int offsetZ, RebuildConsumer<T> consumer, ShapeCache cache);

	interface RebuildConsumer<T> {
		void update(T val, int x, int y, int z);
	}
}
