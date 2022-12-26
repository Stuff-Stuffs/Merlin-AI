package io.github.artificial_intellicrafters.merlin_ai.impl.common.hierarchy;

import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegion;
import it.unimi.dsi.fastutil.shorts.ShortArrays;

public class ChunkSectionRegionImpl implements ChunkSectionRegion {
	private final long id;
	private final short[] set;

	public ChunkSectionRegionImpl(final long id, final short[] set) {
		this.id = id;
		this.set = set;
	}

	@Override
	public long id() {
		return id;
	}

	@Override
	public boolean contains(final short s) {
		return ShortArrays.binarySearch(set, s) >= 0;
	}
}
