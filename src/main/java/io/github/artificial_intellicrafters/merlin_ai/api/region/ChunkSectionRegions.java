package io.github.artificial_intellicrafters.merlin_ai.api.region;

import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.NonExtendable
public interface ChunkSectionRegions<T, N extends AIPathNode<T, N>> {
	ChunkSectionRegionType<?, ?> type();

	@Nullable ChunkSectionRegion<T, N> getRegion(int x, int y, int z);
}
