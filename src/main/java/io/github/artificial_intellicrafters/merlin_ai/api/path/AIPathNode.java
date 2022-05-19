package io.github.artificial_intellicrafters.merlin_ai.api.path;

import org.jetbrains.annotations.Nullable;

public abstract class AIPathNode<T, N extends AIPathNode<T, N>> {
	public final int x;
	public final int y;
	public final int z;
	public double cost;
	public @Nullable N previous;

	public AIPathNode(final int x, final int y, final int z, final double cost, @Nullable final N previous) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.cost = cost;
		this.previous = previous;
	}
}
