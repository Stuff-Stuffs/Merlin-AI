package io.github.artificial_intellicrafters.merlin_ai.api.region;

import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import it.unimi.dsi.fastutil.longs.LongIterator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.NonExtendable
public interface ChunkSectionRegions<T, N extends AIPathNode<T, N>> {
	ChunkSectionRegionType<?, ?> type();

	@Nullable ChunkSectionRegion<T, N> getRegion(int x, int y, int z);

	@Nullable ChunkSectionRegion<T, N> getRegionById(long id);

	LongIterator getRegionIds();
}
