package io.github.artificial_intellicrafters.merlin_ai.api.region;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import org.jetbrains.annotations.ApiStatus;

import java.util.Set;

@ApiStatus.NonExtendable
public interface ChunkSectionRegionType {
	Set<ValidLocationSetType<?>> dependencies();

	ChunkSectionRegionClassifier classifier();
}
