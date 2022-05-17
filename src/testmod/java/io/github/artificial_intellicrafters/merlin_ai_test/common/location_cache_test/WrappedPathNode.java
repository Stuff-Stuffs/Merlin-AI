package io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test;

import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import org.jetbrains.annotations.Nullable;

public class WrappedPathNode<T, N extends AIPathNode<T,N>> {
	public final N delegate;
	public final int nodeCount;
	public final double distToTarget;
	//node before this on path from root node
	public final @Nullable WrappedPathNode<T, N> previous;
	//a node after this on path from root node; other nodes after this can be found be inspecting the sibling field of next.
	public WrappedPathNode<T, N> next;
	//linked list of nodes sharing same previous node.
	public WrappedPathNode<T, N> sibling;

	public WrappedPathNode(final N delegate, final int nodeCount, final double distToTarget, final @Nullable WrappedPathNode<T, N> previous) {
		this.delegate = delegate;
		this.nodeCount = nodeCount;
		this.distToTarget = distToTarget;
		this.previous = previous;
	}
}
