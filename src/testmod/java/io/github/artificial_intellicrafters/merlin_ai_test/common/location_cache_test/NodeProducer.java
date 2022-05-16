package io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test;

import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;

public interface NodeProducer {
	AIPathNode getStart(ShapeCache cache);

	int getNeighbours(AIPathNode previous, AIPathNode[] successors);
}
