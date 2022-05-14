package io.github.artificial_intellicrafters.merlin_ai.api.location_caching;

public interface UniverseInfo<T> {
	T getDefaultValue();

	int toInt(T value);

	T fromInt(int value);

	int getUniverseSize();
}
