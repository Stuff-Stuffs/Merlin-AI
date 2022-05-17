package io.github.artificial_intellicrafters.merlin_ai.impl.common.region;

import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegion;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegions;
import org.jetbrains.annotations.Nullable;

public class ChunkSectionRegionsImpl implements ChunkSectionRegions {
	public static final ChunkSectionRegions EMPTY = new ChunkSectionRegionsImpl(new ChunkSectionRegion[0]);
	private final ChunkSectionRegion[] regions;

	public ChunkSectionRegionsImpl(final ChunkSectionRegion[] regions) {
		this.regions = regions;
	}

	@Override
	public @Nullable ChunkSectionRegion getRegion(final int x, final int y, final int z) {
		for (final ChunkSectionRegion region : regions) {
			if (region.contains(x, y, z)) {
				return region;
			}
		}
		return null;
	}
}
