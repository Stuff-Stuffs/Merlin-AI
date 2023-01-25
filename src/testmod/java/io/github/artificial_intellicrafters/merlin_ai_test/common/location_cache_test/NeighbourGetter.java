package io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test;

import io.github.artificial_intellicrafters.merlin_ai.api.util.AStar;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import org.jetbrains.annotations.Nullable;

public interface NeighbourGetter<T, N extends AIPathNode<T, N>> {

	@Nullable N createStartNode(ShapeCache cache, int x, int y, int z);

	int getNeighbours(ShapeCache cache, N previous, T context, AStar.CostGetter costGetter, Object[] successors);
}
