package io.github.artificial_intellicrafters.merlin_ai.api;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSubSectionRegions;
import org.jetbrains.annotations.Nullable;

public interface ChunkRegionGraph {
	@Nullable Entry getEntry(long subSectionPos);

	@Nullable Entry getEntry(int x, int y, int z);

	interface Entry {
		<T> @Nullable ValidLocationSet<T> getValidLocationSet(ValidLocationSetType<T> type);

		<T, N extends AIPathNode<T, N>> @Nullable ChunkSubSectionRegions<T, N> getRegions(ChunkSectionRegionType<T, N> type);
	}
}
