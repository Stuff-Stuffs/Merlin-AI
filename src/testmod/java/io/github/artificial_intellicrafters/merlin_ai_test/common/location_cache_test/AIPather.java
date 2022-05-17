package io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test;

import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.path.NeighbourGetter;
import io.github.artificial_intellicrafters.merlin_ai.api.util.PathingHeapQueue;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.QuiltLoader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class AIPather<T, N extends AIPathNode<T, N>> {
	private static final boolean DEBUG = QuiltLoader.isDevelopmentEnvironment();
	private final Object[] successors = new Object[64];
	private final World world;
	private final NeighbourGetter<T, N> neighbourGetter;
	private final Function<T, BlockPos> startingPositionRetriever;

	public AIPather(final World world, final NeighbourGetter<T, N> neighbourGetter, final Function<T, BlockPos> startingPositionRetriever) {
		this.world = world;
		this.neighbourGetter = neighbourGetter;
		this.startingPositionRetriever = startingPositionRetriever;
	}

	public @Nullable AIPath<T, N> calculatePath(final PathTarget pathTarget, final double max, final boolean partial, final T context) {
		if (DEBUG) {
			final StopWatch stopWatch = StopWatch.createStarted();
			final PathInfo<T, N> info = find(pathTarget, max, partial, context);
			stopWatch.stop();
			final double v = stopWatch.getTime(TimeUnit.NANOSECONDS) / 1_000_000D;
			System.out.println("Time: " + v);
			System.out.println("Nodes considered: " + info.nodesConsidered());
			System.out.println("Nodes/Second: " + (info.nodesConsidered() / (v / 1000)));
			return info.path;
		} else {
			return find(pathTarget, max, partial, context).path();
		}
	}

	private PathInfo<T, N> find(final PathTarget pathTarget, final double max, final boolean partial, final T context) {
		final BlockPos startingPosition = startingPositionRetriever.apply(context);
		final ShapeCache cache = ShapeCache.create(world, startingPosition.add(-256, -256, -256), startingPosition.add(256, 256, 256));
		final N startNode = neighbourGetter.createStartNode(cache, startingPosition.getX(), startingPosition.getY(), startingPosition.getZ());
		if (startNode == null) {
			return new PathInfo<>(0, null);
		}
		final WrappedPathNode<T, N> start = wrap(startNode, 1, pathTarget, null);
		//Heuristic must be below this value to be considered the end
		final double err = pathTarget.getRadius();
		//We need a specialized heap implementation so that we can remove object in the heap, not just the top
		final PathingHeapQueue<WrappedPathNode<T, N>> queue = new PathingHeapQueue<>(Comparator.comparingDouble(i -> i.distToTarget + i.delegate.cost));
		final Long2ReferenceMap<WrappedPathNode<T, N>> visited = new Long2ReferenceOpenHashMap<>();
		double bestDist = Double.POSITIVE_INFINITY;
		WrappedPathNode<T, N> best = null;
		queue.enqueue(start);
		visited.put(BlockPos.asLong(start.delegate.x, start.delegate.y, start.delegate.z), start);
		//While there is more nodes to visit
		while (!queue.isEmpty()) {
			final WrappedPathNode<T, N> current = queue.dequeue();
			//Check if the node is too far away
			if (current.delegate.cost > max) {
				continue;
			}
			//Is the node the best node so far
			if (current.distToTarget < bestDist) {
				bestDist = current.distToTarget;
				best = current;
			}
			//Is the node at the goal
			if (pathTarget.heuristic(current.delegate.x, current.delegate.y, current.delegate.z) < err) {
				return new PathInfo<>(visited.size(), toPath(current));
			}
			//Get adjacent nodes, fill the array with them, return how many neighbours were found
			final int count = neighbourGetter.getNeighbours(cache, current.delegate, successors);
			//The last node in the linked list formed by AIPathNode.sibling, this is the list of nodes directly after the current one
			WrappedPathNode<T, N> sibling = current.next;
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
				final N next = (N) successors[i];
				final WrappedPathNode<T, N> wrapped = wrap(next, current.nodeCount + 1, pathTarget, current);
				final long pos = BlockPos.asLong(next.x, next.y, next.z);
				//Will return null if this is the first time we see it
				final WrappedPathNode<T, N> node = visited.putIfAbsent(pos, wrapped);
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
					if (next.cost + 0.1 < node.delegate.cost) {
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
						final WrappedPathNode<T, N> previous = node.previous;
						//This should only be null if we are replacing the root node, which shouldn't happen.
						if (previous != null) {
							//if the node getting replaced is the head of the linked list, simply replace it
							if (previous.next == node) {
								previous.next = node.sibling;
							} else {
								//Otherwise, walk the linked list until the node getting replaced is found
								WrappedPathNode<T, N> cursor = previous.next;
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
		return new PathInfo<>(visited.size(), partial && best != null ? toPath(best) : null);
	}

	private WrappedPathNode<T, N> wrap(final N delegate, final int nodeCount, final PathTarget target, @Nullable final WrappedPathNode<T, N> previous) {
		return new WrappedPathNode<>(delegate, nodeCount, target.heuristic(delegate.x, delegate.y, delegate.z), previous);
	}

	private AIPath<T, N> toPath(WrappedPathNode<T, N> node) {
		final List<N> nodes = new ArrayList<>(node.nodeCount);
		for (int i = 0; i < node.nodeCount; i++) {
			nodes.add(null);
		}
		int i = node.nodeCount - 1;
		while (node != null) {
			nodes.set(i--, node.delegate);
			node = node.previous;
		}
		return new AIPath<>(nodes);
	}

	private record PathInfo<T, N extends AIPathNode<T, N>>(int nodesConsidered, @Nullable AIPath<T, N> path) {
	}
}
