package io.github.artificial_intellicrafters.merlin_ai.api.location_caching;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface ValidLocationSetType<T> {
	UniverseInfo<T> universeInfo();

	ValidLocationClassifier<T> classifier();

	Class<T> typeClass();

	boolean columnar();
}
