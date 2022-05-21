package io.github.artificial_intellicrafters.merlin_ai.api.region;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.path.NeighbourGetter;

import java.util.Set;

public interface ChunkSectionRegionType<T,N extends AIPathNode<T,N>> {
	Set<ValidLocationSetType<?>> dependencies();

	NeighbourGetter<T, N> neighbourGetter();
}
