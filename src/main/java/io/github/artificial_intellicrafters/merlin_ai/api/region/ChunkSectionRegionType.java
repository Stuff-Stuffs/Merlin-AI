package io.github.artificial_intellicrafters.merlin_ai.api.region;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.path.NeighbourGetter;
import org.jetbrains.annotations.ApiStatus;

import java.util.Set;

@ApiStatus.NonExtendable
public interface ChunkSectionRegionType<T, N extends AIPathNode<T>> {
	Set<ValidLocationSetType<?>> dependencies();

	NeighbourGetter<T, N> neighbourGetter();
}
