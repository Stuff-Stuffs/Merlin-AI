package io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test;

import io.github.artificial_intellicrafters.merlin_ai.api.AIWorld;
import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.path.NeighbourGetter;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegion;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.graph.ChunkRegionGraph;
import io.github.artificial_intellicrafters.merlin_ai.api.util.AStar;
import io.github.artificial_intellicrafters.merlin_ai.api.util.PathingHeapQueue;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.MerlinAI;
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class AIPather<T, N extends AIPathNode<T, N>> {
	private final World world;
	private final NeighbourGetter<T, N> neighbourGetter;
	private final Function<T, BlockPos> startingPositionRetriever;
	private final ChunkSectionRegionType<T, N> type;
	private final Object[] successors;

	public AIPather(final World world, final NeighbourGetter<T, N> neighbourGetter, final Function<T, BlockPos> startingPositionRetriever, final ChunkSectionRegionType<T, N> type) {
		this.world = world;
		this.neighbourGetter = neighbourGetter;
		this.startingPositionRetriever = startingPositionRetriever;
		this.type = type;
		successors = new Object[AStar.MAX_SUCCESSORS];
	}

	public @Nullable AIPath<T, N> calculatePath(final PathTarget pathTarget, final double max, final boolean partial, final T context) {
		if (MerlinAI.DEBUG) {
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
		final BlockPos startingPos = startingPositionRetriever.apply(context);
		final ShapeCache cache = ShapeCache.create(world, startingPos.add(-256, -256, -256), startingPos.add(256, 256, 256));
		final N startingNode = neighbourGetter.createStartNode(cache, startingPos.getX(), startingPos.getY(), startingPos.getZ());
		if (startingNode == null) {
			return new PathInfo<>(0, null);
		}
		final ChunkSectionRegion<T, N> region = cache.getRegion(startingNode.x, startingNode.y, startingNode.z, type);
		if (region == null) {
			return new PathInfo<>(0, null);
		}
		final PathingHeapQueue<AStar.WrappedPathNode<Node<T, N>>> queue = new PathingHeapQueue<>(Comparator.comparingDouble(i -> i.heuristicDistance + i.delegate.cost));
		final Object2ReferenceMap<RegionKey, AStar.WrappedPathNode<Node<T, N>>> visited = new Object2ReferenceOpenHashMap<>();

		final AStar.WrappedPathNode<Node<T, N>> wrappedStart = AStar.wrap(new Node<>(new RegionKey(ChunkSectionPos.asLong(startingNode.x >> 4, startingNode.y >> 4, startingNode.z >> 4), cache.getRegion(startingNode.x, startingNode.y, startingNode.z, type).id()), 0, pathTarget.heuristic(startingNode.x, startingNode.y, startingNode.z), region, startingNode, null), 0, n -> n.heuristic);
		queue.enqueue(wrappedStart);
		visited.put(wrappedStart.delegate.key, wrappedStart);

		double bestDist = Double.POSITIVE_INFINITY;
		AStar.WrappedPathNode<Node<T, N>> best = null;

		final Node<T, N>[] succ = new Node[AStar.MAX_SUCCESSORS];
		final long[] edges = new long[AStar.MAX_SUCCESSORS * 8];

		while (!queue.isEmpty()) {
			final AStar.WrappedPathNode<Node<T, N>> node = queue.dequeue();
			if (node.delegate.cost > max) {
				continue;
			}
			if (node.heuristicDistance < pathTarget.getRadius()) {
				return createPath(node.delegate.gate);
			}
			if (node.heuristicDistance < bestDist) {
				bestDist = node.heuristicDistance;
				best = node;
			}
			final ChunkRegionGraph.Entry entry = ((AIWorld) world).merlin_ai$getChunkGraph().getEntry(node.delegate.gate.x, node.delegate.gate.y, node.delegate.gate.z);
			if (entry == null) {
				continue;
			}
			final int i = entry.getAdjacentRegions(type, node.delegate.region.id(), edges);
			final int neighbours = fillNeighbours(node.delegate, edges, i, pathTarget, cache, succ);
			for (int j = 0; j < neighbours; j++) {
				final Node<T, N> neighbour = succ[j];
				final AStar.WrappedPathNode<Node<T, N>> wrapped = AStar.wrap(neighbour, node.nodeCount, p -> p.heuristic);
				final AStar.WrappedPathNode<Node<T, N>> n = visited.putIfAbsent(neighbour.key, wrapped);
				if (n == null) {
					//Enqueue node to be processed
					queue.enqueue(wrapped);
				} else {
					if (n.delegate.cost + 0.1 < n.delegate.cost) {
						//This node is better, replace the current one at this position
						visited.put(wrapped.delegate.key, wrapped);

						//Remove the old node from the heap
						queue.removeFirstReference(n);

						//Re-queue the node as its distance has changed;
						//Instead of removing the old reference and adding the new one, this usually talked about in terms of just updating the h score and gscore of the node, or sometimes as using the decrease key function on a heap.
						//Sadly we do not have access to a heap with efficient decrease key, so i just remove the old node and add the new node instead
						queue.enqueue(wrapped);
					}
				}
			}
		}
		return partial && best != null ? createPath(best.delegate.gate) : new PathInfo<>(0, null);
	}

	private int fillNeighbours(final Node<T, N> node, final long[] edges, final int edgeCount, final PathTarget target, final ShapeCache cache, final Node<T, N>[] succ) {
		final PriorityQueue<N> queue = new ObjectArrayFIFOQueue<>();
		final Long2ReferenceMap<N> visited = new Long2ReferenceOpenHashMap<>();
		final Object2ReferenceMap<RegionKey, N> visitedRegions = new Object2ReferenceOpenHashMap<>();

		final N gate = node.gate;
		final ChunkSectionRegion<T, N> region = node.region;

		queue.enqueue(gate);
		visited.put(BlockPos.asLong(node.gate.x, node.gate.y, node.gate.z), gate);
		final AStar.CostGetter costGetter = key -> {
			final N n = visited.get(key);
			if (n != null) {
				return n.cost;
			}
			final int x = BlockPos.unpackLongX(key);
			final int y = BlockPos.unpackLongY(key);
			final int z = BlockPos.unpackLongZ(key);
			if (Arrays.binarySearch(edges, 0, edgeCount, key) >= 0) {
				return Double.POSITIVE_INFINITY;
			}
			if (x >> 4 == node.gate.x >> 4 && y >> 4 == node.gate.y >> 4 && z >> 4 == node.gate.z >> 4 && region.contains(x, y, z)) {
				return Double.POSITIVE_INFINITY;
			}
			return Double.NEGATIVE_INFINITY;
		};
		while (!queue.isEmpty()) {
			final N current = queue.dequeue();
			final int neighbours = neighbourGetter.getNeighbours(cache, current, costGetter, successors);
			for (int i = 0; i < neighbours; i++) {
				final N neighbour = (N) successors[i];
				final long k = BlockPos.asLong(neighbour.x, neighbour.y, neighbour.z);
				final double l = costGetter.cost(k);
				if (l == Double.NEGATIVE_INFINITY) {
					continue;
				}
				final N n = visited.putIfAbsent(k, neighbour);
				boolean update = false;
				if (n == null) {
					queue.enqueue(neighbour);
					update = true;
				} else {
					if (neighbour.cost + 0.1 < n.cost) {
						n.cost = neighbour.cost;
						n.previous = neighbour.previous;
						update = true;
					}
				}
				if (update) {
					final ChunkSectionRegion<T, N> cacheRegion = cache.getRegion(neighbour.x, neighbour.y, neighbour.z, type);
					if (cacheRegion != null) {
						final RegionKey key = new RegionKey(ChunkSectionPos.asLong(neighbour.x >> 4, neighbour.y >> 4, neighbour.z >> 4), cacheRegion.id());
						final N old = visitedRegions.putIfAbsent(key, neighbour);
						if (old != null) {
							if (neighbour.cost < old.cost) {
								visitedRegions.put(key, neighbour);
							}
						}
					}
				}
			}
		}
		int i = 0;
		for (int j = 0; j < edgeCount; j++) {
			final long key = edges[j];
			final int x = BlockPos.unpackLongX(key);
			final int y = BlockPos.unpackLongY(key);
			final int z = BlockPos.unpackLongZ(key);
			final RegionKey k = new RegionKey(ChunkSectionPos.asLong(x >> 4, y >> 4, z >> 4), cache.getRegion(x, y, z, type).id());
			final N n = visitedRegions.get(k);
			if (n != null) {
				succ[i++] = new Node<>(k, n.cost, target.heuristic(n.x, n.y, n.z), cache.getRegion(n.x, n.y, n.z, type), n, node);
			}
		}
		return i;
	}

	private PathInfo<T, N> createPath(N path) {
		final List<N> nodes = new ArrayList<>();
		do {
			nodes.add(0, path);
			path = path.previous;
		} while (path != null);
		return new PathInfo<>(0, new AIPath<>(nodes));
	}

	private record PathInfo<T, N extends AIPathNode<T, N>>(int nodesConsidered, @Nullable AIPath<T, N> path) {
	}

	private record RegionKey(long chunkSectionPos, long id) {
	}

	private static final class Node<T, N extends AIPathNode<T, N>> {
		private final RegionKey key;
		private final double cost;
		private final double heuristic;
		private final ChunkSectionRegion<T, N> region;
		private final N gate;
		private final @Nullable Node<T, N> previous;

		private Node(final RegionKey key, final double cost, final double heuristic, final ChunkSectionRegion<T, N> region, final N gate, @Nullable final Node<T, N> previous) {
			this.key = key;
			this.cost = cost;
			this.heuristic = heuristic;
			this.region = region;
			this.gate = gate;
			this.previous = previous;
		}
	}
}
