package io.github.artificial_intellicrafters.merlin_ai.impl.common.region;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionClassifier;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;

import java.util.Set;

public final class ChunkSectionRegionTypeImpl implements ChunkSectionRegionType {
	private final Set<ValidLocationSetType<?>> dependencies;
	private final ChunkSectionRegionClassifier classifier;

	public ChunkSectionRegionTypeImpl(final Set<ValidLocationSetType<?>> dependencies, final ChunkSectionRegionClassifier classifier) {
		this.dependencies = dependencies;
		this.classifier = classifier;
	}

	@Override
	public Set<ValidLocationSetType<?>> dependencies() {
		return dependencies;
	}

	@Override
	public ChunkSectionRegionClassifier classifier() {
		return classifier;
	}
}
