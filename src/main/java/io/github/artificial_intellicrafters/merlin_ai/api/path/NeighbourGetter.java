package io.github.artificial_intellicrafters.merlin_ai.api.path;

import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;

public interface NeighbourGetter<T, N extends AIPathNode<T>> {
	N createStartNode(ShapeCache cache, T context);

	int getNeighbours(ShapeCache cache, N previous, Object[] successors);
}
