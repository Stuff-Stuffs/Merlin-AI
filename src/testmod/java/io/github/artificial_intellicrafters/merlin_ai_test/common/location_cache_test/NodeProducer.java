package io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test;

import io.github.artificial_intellicrafters.merlin_ai.api.util.WorldCache;

public interface NodeProducer {
	AIPathNode getStart(WorldCache cache);

	int getNeighbours(AIPathNode root, AIPathNode[] successors);

	AIPathNode get(int x, int y, int z);
}
