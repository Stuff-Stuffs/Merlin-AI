package io.github.artificial_intellicrafters.merlin_ai.impl.common.region;

import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegion;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegions;
import org.jetbrains.annotations.Nullable;

public class ChunkSectionRegionsImpl<T, N extends AIPathNode<T, N>> implements ChunkSectionRegions<T, N> {
	private final ChunkSectionRegionType<?, ?> type;
	private final ChunkSectionRegion<T, N>[] regions;

	public ChunkSectionRegionsImpl(final ChunkSectionRegionType<?, ?> type, final ChunkSectionRegion<T, N>[] regions) {
		this.type = type;
		this.regions = regions;
	}

	@Override
	public ChunkSectionRegionType<?, ?> type() {
		return type;
	}

	@Override
	public @Nullable ChunkSectionRegion<T, N> getRegion(final int x, final int y, final int z) {
		for (final ChunkSectionRegion<T, N> region : regions) {
			if (region.contains(x, y, z)) {
				return region;
			}
		}
		return null;
	}
}
