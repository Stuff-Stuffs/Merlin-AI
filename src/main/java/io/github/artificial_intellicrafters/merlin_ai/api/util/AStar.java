package io.github.artificial_intellicrafters.merlin_ai.api.util;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.LongPredicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

public final class AStar {
	public static final int MAX_SUCCESSORS = 64;

	public static <T, C> PathInfo<T> findPath(final T start, final C context, final ToLongFunction<T> keyGetter, final NeighbourGetter<T, C> neighbourGetter, final Function<T, @Nullable T> previousNodeGetter, final ToDoubleFunction<T> cost, final ToDoubleFunction<T> heuristic, final double error, final double maxCost, final boolean partial) {
		final Object[] successors = new Object[MAX_SUCCESSORS];
		final PathingHeap<WrappedPathNode<T>> queue = new PathingHeap<>();
		final Long2ReferenceMap<WrappedPathNode<T>> visited = new Long2ReferenceOpenHashMap<>(Hash.DEFAULT_INITIAL_SIZE, 0.5F);
		double bestDist = Double.POSITIVE_INFINITY;
		WrappedPathNode<T> best = null;
		final WrappedPathNode<T> wrappedStart = wrap(start, 1, heuristic);
		wrappedStart.handle = queue.insert(wrappedStart.heuristicDistance + cost.applyAsDouble(wrappedStart.delegate), wrappedStart);
		visited.put(keyGetter.applyAsLong(start), wrappedStart);
		final CostGetter costGetter = key -> {
			final WrappedPathNode<T> node = visited.get(key);
			if (node == null) {
				return Double.POSITIVE_INFINITY;
			}
			return cost.applyAsDouble(node.delegate);
		};
		//While there is more nodes to visit
		while (!queue.isEmpty()) {
			final WrappedPathNode<T> current = queue.deleteMin().getValue();
			//Check if the node is too far away
			if (cost.applyAsDouble(current.delegate) > maxCost) {
				continue;
			}
			//Is the node the best node so far
			if (current.heuristicDistance < bestDist) {
				bestDist = current.heuristicDistance;
				best = current;
			}
			//Is the node at the goal
			if (heuristic.applyAsDouble(current.delegate) < error) {
				return createPath(visited.size(), current, previousNodeGetter);
			}
			//Get adjacent nodes, fill the array with them, return how many neighbours were found
			final int count = neighbourGetter.getNeighbours(current.delegate, context, costGetter, successors);
			//For each neighbour found
			for (int i = 0; i < count; i++) {
				final T next = (T) successors[i];
				final double nextHeuristic = heuristic.applyAsDouble(next);
				if(nextHeuristic!=nextHeuristic) {
					continue;
				}
				final long pos = keyGetter.applyAsLong(next);
				final WrappedPathNode<T> wrapped = wrap(next, current.nodeCount + 1, heuristic);
				//Will return null if this is the first time we see it
				final WrappedPathNode<T> node = visited.putIfAbsent(pos, wrapped);
				if (node == null) {
					//Enqueue node to be processed
					wrapped.handle = queue.insert(wrapped.heuristicDistance + cost.applyAsDouble(wrapped.delegate), wrapped);
				} else {
					//We check if this node faster to get to than the currently existing one,  I add a small constant because sometimes a path is every so slightly short(example 0.0001 shorter path).
					//This is not worth the computation time to consider
					final double v = cost.applyAsDouble(next);
					if (v + 0.01 + wrapped.heuristicDistance < cost.applyAsDouble(node.delegate) + node.heuristicDistance) {
						//This node is better, replace the current one at this position
						visited.put(pos, wrapped);
						if (node.handle.isValid()) {
							wrapped.handle = node.handle;
							wrapped.handle.setValue(wrapped);
							wrapped.handle.decreaseKey(v + wrapped.heuristicDistance);
						} else {
							wrapped.handle = queue.insert(v + wrapped.heuristicDistance, wrapped);
						}
					}
				}
			}
		}
		return best == null ? new PathInfo<>(visited.size(), null) : partial ? createPath(visited.size(), best, previousNodeGetter) : new PathInfo<>(visited.size(), null);
	}

	public static <T, C> Set<T> restrictedDjikstra(final T start, final C context, final ToLongFunction<T> keyGetter, final NeighbourGetter<T, C> neighbourGetter, final ToDoubleFunction<T> cost, final LongPredicate allowed, final LongPredicate tracked) {
		final Object[] successors = new Object[MAX_SUCCESSORS];
		final PriorityQueue<T> queue = new ObjectArrayFIFOQueue<>();
		final Long2ReferenceMap<T> visited = new Long2ReferenceOpenHashMap<>(Hash.DEFAULT_INITIAL_SIZE, 0.5F);
		final Set<T> nodes = new ReferenceOpenHashSet<>();
		visited.put(keyGetter.applyAsLong(start), start);
		queue.enqueue(start);
		final CostGetter costGetter = key -> {
			if (!allowed.test(key)) {
				return Double.NEGATIVE_INFINITY;
			}
			final T node = visited.get(key);
			if (node == null) {
				return Double.POSITIVE_INFINITY;
			}
			return cost.applyAsDouble(node);
		};
		ReferenceSet<T> removed = new ReferenceOpenHashSet<>();
		//While there is more nodes to visit
		while (!queue.isEmpty()) {
			final T current = queue.dequeue();
			if(removed.contains(current)) {
				continue;
			}
			//Get adjacent nodes, fill the array with them, return how many neighbours were found
			final int count = neighbourGetter.getNeighbours(current, context, costGetter, successors);
			//For each neighbour found
			for (int i = 0; i < count; i++) {
				final T next = (T) successors[i];
				if (!allowed.test(keyGetter.applyAsLong(next))) {
					continue;
				}
				final long pos = keyGetter.applyAsLong(next);
				//Will return null if this is the first time we see it
				final T node = visited.putIfAbsent(pos, next);
				if (node == null) {
					//Enqueue node to be processed
					queue.enqueue(next);
					if (tracked.test(keyGetter.applyAsLong(next))) {
						nodes.add(next);
					}
				} else {
					//We check if this node faster to get to than the currently existing one,  I add a small constant because sometimes a path is every so slightly short(example 0.0001 shorter path).
					//This is not worth the computation time to consider
					final double v = cost.applyAsDouble(next);
					if (v + 0.01 < cost.applyAsDouble(node)) {
						visited.put(pos, next);
						queue.enqueue(next);
						nodes.remove(node);
						removed.add(node);
						if (tracked.test(keyGetter.applyAsLong(next))) {
							nodes.add(next);
						}
					}
				}
			}
		}
		return nodes;
	}

	private static <T> PathInfo<T> createPath(final int considered, final WrappedPathNode<T> wrapped, final Function<T, @Nullable T> previousNodeGetter) {
		final List<T> list = new ArrayList<>(wrapped.nodeCount);
		for (int i = 0; i < wrapped.nodeCount; i++) {
			list.add(null);
		}
		T node = wrapped.delegate;
		int i = wrapped.nodeCount - 1;
		while (node != null && i >= 0) {
			list.set(i, node);
			i--;
			node = previousNodeGetter.apply(node);
		}
		if (i + 1 != 0) {
			throw new RuntimeException("Invalid previousNodeGetter!");
		}
		return new PathInfo<>(considered, list);
	}

	public record PathInfo<T>(int nodesConsidered, @Nullable List<T> path) {
	}

	private static <T> WrappedPathNode<T> wrap(final T delegate, final int nodeCount, final ToDoubleFunction<T> heuristic) {
		return new WrappedPathNode<>(delegate, nodeCount, heuristic.applyAsDouble(delegate));
	}

	static final class WrappedPathNode<T> {
		public final T delegate;
		public final int nodeCount;
		public final double heuristicDistance;
		public PathingHeap.Node<WrappedPathNode<T>> handle;

		public WrappedPathNode(final T delegate, final int nodeCount, final double heuristicDistance) {
			this.delegate = delegate;
			this.nodeCount = nodeCount;
			this.heuristicDistance = heuristicDistance;
		}
	}

	public interface NeighbourGetter<T, C> {
		int getNeighbours(T previous, C context, CostGetter costGetter, Object[] successors);
	}

	public interface CostGetter {
		double cost(long key);
	}

	private AStar() {
	}
}
