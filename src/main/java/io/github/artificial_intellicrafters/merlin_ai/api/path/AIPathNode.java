package io.github.artificial_intellicrafters.merlin_ai.api.path;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public abstract class AIPathNode<T, N extends AIPathNode<T,N>> {
	public final int x;
	public final int y;
	public final int z;
	public final BiPredicate<T, List<N>> linkPredicate;
	public final  double cost;

	public AIPathNode(final int x, final int y, final int z, double cost) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.cost = cost;
		linkPredicate = null;
	}

	public AIPathNode(final int x, final int y, final int z, BiPredicate<T, List<N>> linkPredicate, double cost) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.cost = cost;
		this.linkPredicate = linkPredicate;
	}
}
