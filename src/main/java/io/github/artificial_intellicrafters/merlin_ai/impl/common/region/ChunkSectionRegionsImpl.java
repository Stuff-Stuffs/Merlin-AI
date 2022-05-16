package io.github.artificial_intellicrafters.merlin_ai.impl.common.region;

import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegion;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegions;
import org.jetbrains.annotations.Nullable;

public class ChunkSectionRegionsImpl implements ChunkSectionRegions {
	private final ChunkSectionRegionImpl[] regions;

	public ChunkSectionRegionsImpl(ChunkSectionRegionImpl[] regions) {
		this.regions = regions;
	}

	@Override
	public @Nullable ChunkSectionRegion getRegion(int x, int y, int z) {
		for (ChunkSectionRegionImpl region : regions) {
			if(region.contains(x,y,z)) {
				return region;
			}
		}
		return null;
	}
}
