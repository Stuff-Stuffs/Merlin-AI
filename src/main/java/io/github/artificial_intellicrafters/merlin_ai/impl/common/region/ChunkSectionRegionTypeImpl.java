package io.github.artificial_intellicrafters.merlin_ai.impl.common.region;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.path.NeighbourGetter;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;

import java.util.Set;

public final class ChunkSectionRegionTypeImpl<T, N extends AIPathNode<T, N>> implements ChunkSectionRegionType<T, N> {
	private final Set<ValidLocationSetType<?>> dependencies;
	private final NeighbourGetter<T, N> neighbourGetter;

	public ChunkSectionRegionTypeImpl(final Set<ValidLocationSetType<?>> dependencies, final NeighbourGetter<T, N> neighbourGetter) {
		this.dependencies = dependencies;
		this.neighbourGetter = neighbourGetter;
	}

	@Override
	public Set<ValidLocationSetType<?>> dependencies() {
		return dependencies;
	}

	@Override
	public NeighbourGetter<T, N> neighbourGetter() {
		return neighbourGetter;
	}
}
