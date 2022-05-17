package io.github.artificial_intellicrafters.merlin_ai.impl.common.region;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.path.NeighbourGetter;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionTypeRegistry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ChunkSectionRegionTypeRegistryImpl implements ChunkSectionRegionTypeRegistry {
	public static final ChunkSectionRegionTypeRegistryImpl INSTANCE = new ChunkSectionRegionTypeRegistryImpl();
	private final Map<Identifier, ChunkSectionRegionType<?, ?>> registry = new HashMap<>();

	@Override
	public void register(final Set<ValidLocationSetType<?>> dependencies, final NeighbourGetter<?, ?> neighbourGetter, final Identifier id) {
		if (registry.put(id, new ChunkSectionRegionTypeImpl<>(dependencies, neighbourGetter)) != null) {
			throw new RuntimeException("Duplicate ChunkSectionRegionTypes");
		}
	}

	@Override
	public <T, N extends AIPathNode<T>> @Nullable ChunkSectionRegionType<T, N> get(final NeighbourGetter<T, N> neighbourGetter, final Identifier id) {
		final ChunkSectionRegionType<?, ?> type = registry.get(id);
		if (type == null || type.neighbourGetter() != neighbourGetter) {
			return null;
		}
		return (ChunkSectionRegionType<T, N>) type;
	}
}
