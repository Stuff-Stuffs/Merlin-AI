package io.github.artificial_intellicrafters.merlin_ai.api.region;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.region.ChunkSectionRegionTypeRegistryImpl;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface ChunkSectionRegionTypeRegistry {
	ChunkSectionRegionTypeRegistry INSTANCE = ChunkSectionRegionTypeRegistryImpl.INSTANCE;

	void register(Set<ValidLocationSetType<?>> dependencies, ChunkSectionRegionClassifier classifier, Identifier id);

	@Nullable ChunkSectionRegionType get(Identifier id);
}
