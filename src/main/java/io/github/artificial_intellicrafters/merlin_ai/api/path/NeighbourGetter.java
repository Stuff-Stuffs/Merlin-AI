package io.github.artificial_intellicrafters.merlin_ai.api.path;

import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import org.jetbrains.annotations.Nullable;

public interface NeighbourGetter<T, N extends AIPathNode<T>> {
	int MAX_SUCCESSORS = 64;

	@Nullable N createStartNode(ShapeCache cache, int x, int y, int z);

	int getNeighbours(ShapeCache cache, N previous, Object[] successors);
}
