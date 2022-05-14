package io.github.artificial_intellicrafters.merlin_ai.impl.common.location_caching;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.UniverseInfo;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationClassifier;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;

public final class ValidLocationSetTypeImpl<T> implements ValidLocationSetType<T> {
	private final UniverseInfo<T> universeInfo;
	private final ValidLocationClassifier<T> classifier;
	private final Class<T> typeClass;

	public ValidLocationSetTypeImpl(final UniverseInfo<T> universeInfo, final ValidLocationClassifier<T> classifier, final Class<T> typeClass) {
		this.universeInfo = universeInfo;
		this.classifier = classifier;
		this.typeClass = typeClass;
	}

	@Override
	public UniverseInfo<T> universeInfo() {
		return universeInfo;
	}

	@Override
	public ValidLocationClassifier<T> classifier() {
		return classifier;
	}

	@Override
	public Class<T> typeClass() {
		return typeClass;
	}
}
