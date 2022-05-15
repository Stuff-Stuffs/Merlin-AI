package io.github.artificial_intellicrafters.merlin_ai.api.location_caching;

import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;

public interface ValidLocationClassifier<T> {
	T validate(int x, int y, int z, ShapeCache cache);
}
