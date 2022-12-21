package io.github.artificial_intellicrafters.merlin_ai.api.location_caching;

import java.util.function.ToIntFunction;

public interface UniverseInfo<T> {
	T getDefaultValue();

	int toInt(T value);

	T fromInt(int value);

	int getUniverseSize();

	static <T extends Enum<T>> UniverseInfo<T> ofEnum(final T defaultValue, final ToIntFunction<T> indexer) {
		final T[] constants = (T[]) defaultValue.getClass().getEnumConstants();
		return new UniverseInfo<>() {
			@Override
			public T getDefaultValue() {
				return defaultValue;
			}

			@Override
			public int toInt(final T value) {
				return indexer.applyAsInt(value);
			}

			@Override
			public T fromInt(final int value) {
				return constants[value];
			}

			@Override
			public int getUniverseSize() {
				return constants.length;
			}
		};
	}
}
