package io.github.artificial_intellicrafters.merlin_ai_test.common;

import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test.BasicLocationType;
import net.minecraft.entity.Entity;

import java.util.List;
import java.util.function.BiPredicate;

public class BasicAIPathNode extends AIPathNode<Entity, BasicAIPathNode> {
	public final BasicLocationType type;

	public BasicAIPathNode(final int x, final int y, final int z, final double cost, final BasicLocationType type) {
		super(x, y, z, cost);
		this.type = type;
	}

	public BasicAIPathNode(final int x, final int y, final int z, final BiPredicate<Entity, List<BasicAIPathNode>> linkPredicate, final double cost, final BasicLocationType type) {
		super(x, y, z, linkPredicate, cost);
		this.type = type;
	}
}
