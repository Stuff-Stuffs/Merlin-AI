package io.github.artificial_intellicrafters.merlin_ai.impl.common.region;

import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegion;
import it.unimi.dsi.fastutil.longs.LongSet;

public interface AbstractChunkSectionRegionImpl<T, N extends AIPathNode<T, N>> extends ChunkSectionRegion<T, N> {
	LongSet getOutgoingEdges();
}
