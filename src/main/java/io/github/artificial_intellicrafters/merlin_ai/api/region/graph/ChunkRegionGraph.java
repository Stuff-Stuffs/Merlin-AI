package io.github.artificial_intellicrafters.merlin_ai.api.region.graph;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegions;
import net.minecraft.util.math.ChunkSectionPos;
import org.jetbrains.annotations.Nullable;

public interface ChunkRegionGraph {
	@Nullable Entry getEntry(ChunkSectionPos pos);

	@Nullable Entry getEntry(int x, int y, int z);

	interface Entry {
		long getId();

		/**
		 * @return A valid location set if it exists, this operation is slow, you should cache it
		 */
		<T> @Nullable ValidLocationSet<T> getValidLocationSet(ValidLocationSetType<T> type);

		/**
		 * @return A region set if it exists, this operation is slow, you should cache it
		 */
		<T, N extends AIPathNode<T, N>> @Nullable ChunkSectionRegions<T, N> getChunkSectionRegions(ChunkSectionRegionType<T, N> type);
	}
}
