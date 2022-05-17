package io.github.artificial_intellicrafters.merlin_ai.api.util;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

public final class AStar {
	public static final int MAX_SUCCESSORS = 64;

	public static <T, C> PathInfo<T> findPath(final T start, final C context, final ToLongFunction<T> keyGetter, final NeighbourGetter<T, C> neighbourGetter, final ToDoubleFunction<T> cost, final ToDoubleFunction<T> heuristic, final double error, final double maxCost, final boolean partial) {
		final Object[] successors = new Object[MAX_SUCCESSORS];
		final PathingHeapQueue<WrappedPathNode<T>> queue = new PathingHeapQueue<>(Comparator.comparingDouble(i -> i.distToTarget + cost.applyAsDouble(i.delegate)));
		final Long2ReferenceMap<WrappedPathNode<T>> visited = new Long2ReferenceOpenHashMap<>();
		double bestDist = Double.POSITIVE_INFINITY;
		WrappedPathNode<T> best = null;
		final WrappedPathNode<T> wrappedStart = wrap(start, 1, heuristic, null);
		queue.enqueue(wrappedStart);
		visited.put(keyGetter.applyAsLong(start), wrappedStart);
		//While there is more nodes to visit
		while (!queue.isEmpty()) {
			final WrappedPathNode<T> current = queue.dequeue();
			//Check if the node is too far away
			if (cost.applyAsDouble(current.delegate) > maxCost) {
				continue;
			}
			//Is the node the best node so far
			if (current.distToTarget < bestDist) {
				bestDist = current.distToTarget;
				best = current;
			}
			//Is the node at the goal
			if (heuristic.applyAsDouble(current.delegate) < error) {
				return createPath(visited.size(), current);
			}
			//Get adjacent nodes, fill the array with them, return how many neighbours were found
			final int count = neighbourGetter.getNeighbours(current.delegate, context, successors);
			//The last node in the linked list formed by AIPathNode.sibling, this is the list of nodes directly after the current one
			WrappedPathNode<T> sibling = current.next;
			//If sibling is null we don't need to find the end of the linked list, as it doesn't exist
			while (sibling != null) {
				if (sibling.sibling != null) {
					sibling = sibling.sibling;
				} else {
					break;
				}
			}
			//For each neighbour found
			for (int i = 0; i < count; i++) {
				final T next = (T) successors[i];
				final long pos = keyGetter.applyAsLong(next);
				final WrappedPathNode<T> wrapped = wrap(next, current.nodeCount + 1, heuristic, current);
				//Will return null if this is the first time we see it
				final WrappedPathNode<T> node = visited.putIfAbsent(pos, wrapped);
				if (node == null) {
					if (sibling == null) {
						//If the current node has no next node, put the current neighbour as the first in the linked list
						current.next = wrapped;
					} else {
						//If the current node has a next node, put the current neighbour at the end of the linked list of next nodes
						sibling.sibling = wrapped;
					}
					//The last node in the linked list is now the current neighbour
					sibling = wrapped;
					//Enqueue node to be processed
					queue.enqueue(wrapped);
				} else {
					//We check if this node faster to get to than the currently existing one,  I add a small constant because sometimes a path is every so slightly short(example 0.0001 shorter path).
					//This is not worth the computation time to consider
					if (cost.applyAsDouble(next) + 0.1 < cost.applyAsDouble(node.delegate)) {
						//This node is better, replace the current one at this position
						visited.put(pos, wrapped);
						if (sibling == null) {
							//If the current node has no next node, put the current neighbour as the first in the linked list
							current.next = wrapped;
						} else {
							//If the current node has a next node, put the current neighbour at the end of the linked list of next nodes
							sibling.sibling = wrapped;
						}
						sibling = wrapped;
						//Find the node before the old node that we replaced
						final WrappedPathNode<T> previous = node.previous;
						//This should only be null if we are replacing the root node, which shouldn't happen.
						if (previous != null) {
							//if the node getting replaced is the head of the linked list, simply replace it
							if (previous.next == node) {
								previous.next = node.sibling;
							} else {
								//Otherwise, walk the linked list until the node getting replaced is found
								WrappedPathNode<T> cursor = previous.next;
								while (cursor.sibling != node) {
									cursor = cursor.sibling;
								}
								//Remove the node
								cursor.sibling = cursor.sibling.sibling;
							}
						}
						//Remove the old node from the heap
						queue.removeFirstReference(node);
						//Re-queue the node as its distance has changed;
						queue.enqueue(wrapped);
					}
				}
			}
		}
		return best == null ? new PathInfo<>(visited.size(), null) : partial ? createPath(visited.size(), best) : new PathInfo<>(visited.size(), null);
	}

	private static <T> PathInfo<T> createPath(final int considered, WrappedPathNode<T> node) {
		final List<T> list = new ArrayList<>(node.nodeCount);
		for (int i = 0; i < node.nodeCount; i++) {
			list.add(null);
		}
		while (node != null) {
			list.set(node.nodeCount - 1, node.delegate);
			node = node.previous;
		}
		return new PathInfo<>(considered, list);
	}

	public record PathInfo<T>(int nodesConsidered, @Nullable List<T> path) {
	}

	private static <T> WrappedPathNode<T> wrap(final T delegate, final int nodeCount, final ToDoubleFunction<T> heuristic, @Nullable final WrappedPathNode<T> previous) {
		return new WrappedPathNode<>(delegate, nodeCount, heuristic.applyAsDouble(delegate), previous);
	}

	static final class WrappedPathNode<T> {
		public final T delegate;
		public final int nodeCount;
		public final double distToTarget;
		//node before this on path from root node
		public final @Nullable WrappedPathNode<T> previous;
		//a node after this on path from root node; other nodes after this can be found be inspecting the sibling field of next.
		public WrappedPathNode<T> next;
		//linked list of nodes sharing same previous node.
		public WrappedPathNode<T> sibling;
		public int index = -1;

		public WrappedPathNode(final T delegate, final int nodeCount, final double distToTarget, final @Nullable WrappedPathNode<T> previous) {
			this.delegate = delegate;
			this.nodeCount = nodeCount;
			this.distToTarget = distToTarget;
			this.previous = previous;
		}
	}

	public interface NeighbourGetter<T, C> {
		int getNeighbours(T previous, C context, Object[] successors);
	}

	private AStar() {
	}
}
