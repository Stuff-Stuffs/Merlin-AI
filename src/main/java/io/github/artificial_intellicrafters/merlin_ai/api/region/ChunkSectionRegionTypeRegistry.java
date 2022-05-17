package io.github.artificial_intellicrafters.merlin_ai.api.region;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.path.NeighbourGetter;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.region.ChunkSectionRegionTypeRegistryImpl;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface ChunkSectionRegionTypeRegistry {
	ChunkSectionRegionTypeRegistry INSTANCE = ChunkSectionRegionTypeRegistryImpl.INSTANCE;

	void register(Set<ValidLocationSetType<?>> dependencies, NeighbourGetter<?, ?> neighbourGetter, Identifier id);

	<T, N extends AIPathNode<T>> @Nullable ChunkSectionRegionType<T, N> get(NeighbourGetter<T, N> neighbourGetter, Identifier id);
}
