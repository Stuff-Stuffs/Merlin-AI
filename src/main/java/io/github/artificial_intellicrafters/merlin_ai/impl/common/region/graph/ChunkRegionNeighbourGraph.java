package io.github.artificial_intellicrafters.merlin_ai.impl.common.region.graph;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;

public class ChunkRegionNeighbourGraph {
	private final Long2ReferenceMap<long[]> entries;

	public ChunkRegionNeighbourGraph(final Long2ReferenceMap<long[]> entries) {
		this.entries = entries;
	}

	public long[] getEntries(final long id) {
		return entries.get(id);
	}
}
