package io.github.artificial_intellicrafters.merlin_ai.impl.common.region;

import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegion;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.Arrays;

public class ChunkSectionBigRegionImpl<T, N extends AIPathNode<T, N>> implements AbstractChunkSectionRegionImpl<T, N> {
	private final int id;
	private final short[] positions;
	private final LongSet normalOutgoingEdges;

	public ChunkSectionBigRegionImpl(final int id, final short[] positions, final LongSet normalOutgoingEdges) {
		this.id = id;
		this.positions = positions;
		this.normalOutgoingEdges = normalOutgoingEdges;
	}

	@Override
	public int id() {
		return id;
	}

	@Override
	public boolean contains(final int x, final int y, final int z) {
		final short local = ChunkSectionRegion.packLocal(x & 15, y & 15, z & 15);
		return Arrays.binarySearch(positions, local) >= 0;
	}

	@Override
	public LongSet getOutgoingEdges() {
		return normalOutgoingEdges;
	}
}
