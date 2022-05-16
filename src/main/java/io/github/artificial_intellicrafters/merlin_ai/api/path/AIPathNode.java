package io.github.artificial_intellicrafters.merlin_ai.api.path;

import java.util.function.Predicate;

public abstract class AIPathNode<T> {
	public static final Predicate<Object> TRUE_PREDICATE = o -> true;
	public static final Predicate<Object> FALSE_PREDICATE = o -> false;
	public final int x;
	public final int y;
	public final int z;
	public final boolean contextSensitive;
	public final Predicate<? super T> linkPredicate;
	public final  double cost;

	public AIPathNode(final int x, final int y, final int z, double cost) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.cost = cost;
		contextSensitive = false;
		linkPredicate = TRUE_PREDICATE;
	}

	public AIPathNode(final int x, final int y, final int z, final Predicate<? super T> linkPredicate, double cost) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.cost = cost;
		contextSensitive = true;
		this.linkPredicate = linkPredicate;
	}
}
