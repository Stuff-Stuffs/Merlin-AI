package io.github.artificial_intellicrafters.merlin_ai_test.common;

import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test.BasicLocationType;

import java.util.function.Predicate;

public class BasicAIPathNode<T> extends AIPathNode<T> {
	public final BasicLocationType type;

	public BasicAIPathNode(final int x, final int y, final int z, final double cost, final BasicLocationType type) {
		super(x, y, z, cost);
		this.type = type;
	}

	public BasicAIPathNode(final int x, final int y, final int z, final Predicate<T> linkPredicate, final double cost, final BasicLocationType type) {
		super(x, y, z, linkPredicate, cost);
		this.type = type;
	}
}
