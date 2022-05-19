package io.github.artificial_intellicrafters.merlin_ai_test.common;

import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test.BasicLocationType;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class BasicAIPathNode extends AIPathNode<Entity, BasicAIPathNode> {
	public final BasicLocationType type;

	public BasicAIPathNode(final int x, final int y, final int z, final double cost, final BasicLocationType type, final @Nullable BasicAIPathNode previous) {
		super(x, y, z, cost, previous);
		this.type = type;
	}
}
