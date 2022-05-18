package io.github.artificial_intellicrafters.merlin_ai.api.path;

import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;

public abstract class AIPathNode<T, N extends AIPathNode<T, N>> {
	public final int x;
	public final int y;
	public final int z;
	public final BiPredicate<T, N> linkPredicate;
	public final double cost;
	public final @Nullable N previous;

	public AIPathNode(final int x, final int y, final int z, final double cost, @Nullable final N previous) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.cost = cost;
		this.previous = previous;
		linkPredicate = null;
	}

	public AIPathNode(final int x, final int y, final int z, final BiPredicate<T, N> linkPredicate, final double cost, @org.jetbrains.annotations.Nullable final N previous) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.cost = cost;
		this.linkPredicate = linkPredicate;
		this.previous = previous;
	}
}
