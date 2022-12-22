package io.github.artificial_intellicrafters.merlin_ai.api.location_caching;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface ValidLocationSet<T> {
	ValidLocationSetType<T> type();

	long revision();

	T get(final int x, final int y, final int z);
}
