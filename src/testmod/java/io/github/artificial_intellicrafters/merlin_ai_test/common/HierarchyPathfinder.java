package io.github.artificial_intellicrafters.merlin_ai_test.common;

import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSubSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.util.AStar;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.api.util.SubChunkSectionUtil;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.MerlinAI;
import io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test.AIPath;
import io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test.AIPather;
import io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test.PathTarget;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public class HierarchyPathfinder<T, N extends AIPathNode<T, N>> {
	private final World world;
	private final ChunkSectionRegionType<T, N> type;
	private final Function<T, BlockPos> startingPositionRetriever;

	public HierarchyPathfinder(final World world, final ChunkSectionRegionType<T, N> type, final Function<T, BlockPos> startingPositionRetriever) {
		this.world = world;
		this.type = type;
		this.startingPositionRetriever = startingPositionRetriever;
	}

	public @Nullable AIPath<T, N> calculatePath(final PathTarget pathTarget, final double max, final boolean partial, final T context) {
		if (MerlinAI.DEBUG) {
			final StopWatch stopWatch = StopWatch.createStarted();
			final AIPather.PathInfo<T, N> info = find(pathTarget, max, partial, context);
			stopWatch.stop();
			final double v = stopWatch.getTime(TimeUnit.NANOSECONDS) / 1_000_000D;
			System.out.println("Time: " + v);
			System.out.println("Nodes considered: " + info.nodesConsidered());
			System.out.println("Nodes/Second: " + (info.nodesConsidered() / (v / 1000)));
			return info.path();
		} else {
			return find(pathTarget, max, partial, context).path();
		}
	}

	private AIPather.PathInfo<T, N> find(final PathTarget pathTarget, final double max, final boolean partial, final T context) {
		final BlockPos startingPos = startingPositionRetriever.apply(context);
		final ShapeCache cache = ShapeCache.create(world, startingPos.add(-256, -256, -256), startingPos.add(256, 256, 256));
		final N startingNode = type.neighbourGetter().createStartNode(cache, startingPos.getX(), startingPos.getY(), startingPos.getZ());
		if (startingNode == null) {
			return new AIPather.PathInfo<>(0, null);
		}
		final ChunkSubSectionRegions<T, N> regions = cache.getRegions(startingNode.x, startingNode.y, startingNode.z, type);
		if (regions == null) {
			return new AIPather.PathInfo<>(0, null);
		}
		final int containingRegion = regions.getContainingRegion(startingNode.x, startingNode.y, startingNode.z);
		if (!regions.isValidRegion(containingRegion)) {
			return new AIPather.PathInfo<>(0, null);
		}
		final long key = SubChunkSectionUtil.pack(SubChunkSectionUtil.blockToSubSection(startingNode.x), SubChunkSectionUtil.blockToSubSection(startingNode.y), SubChunkSectionUtil.blockToSubSection(startingNode.z), containingRegion);
		final SubNode<T, N> startingSubNode = new SubNode<>(null, type, key, regions, cache, pathTarget.heuristic(startingNode.x, startingNode.y, startingNode.z), 0);
		//Find a path through the upper level
		final AStar.PathInfo<SubNode<T, N>> path = AStar.findPath(startingSubNode, context, node -> node.regionInfo, (previous, context1, costGetter, successors) -> previous.getNeighbours(context1, costGetter, pathTarget, successors), node -> node.previous, node -> node.cost, node -> node.heuristic, pathTarget.getRadius(), max, partial);
		final List<SubNode<T, N>> subNodes = path.path();
		if (subNodes != null) {
			final Long2ReferenceMap<SubNode<T, N>> map = new Long2ReferenceOpenHashMap<>();
			for (final SubNode<T, N> subNode : subNodes) {
				map.put(subNode.regionInfo, subNode);
			}
			final AStar.PathInfo<N> info = AStar.findPath(startingNode, context, n -> BlockPos.asLong(n.x, n.y, n.z), (previous, context1, costGetter, successors) -> type.neighbourGetter().getNeighbours(cache, previous, false, costGetter, successors), n -> n.previous, n -> n.cost, pathHeuristic(map, cache, type), pathTarget.getRadius(), max, partial);
			if (info.path() != null) {
				return new AIPather.PathInfo<>(info.nodesConsidered() + path.nodesConsidered(), new AIPath<>(info.path()));
			}
		}
		return new AIPather.PathInfo<>(path.nodesConsidered(), null);
	}

	private static <T, N extends AIPathNode<T, N>> ToDoubleFunction<N> pathHeuristic(final Long2ReferenceMap<SubNode<T, N>> subNodes, final ShapeCache cache, final ChunkSectionRegionType<T, N> type) {
		return value -> {
			final ChunkSubSectionRegions<T, N> regions = cache.getRegions(value.x, value.y, value.z, type);
			if (regions == null) {
				return Double.NaN;
			}
			final int containingRegion = regions.getContainingRegion(value.x, value.y, value.z);
			if (!regions.isValidRegion(containingRegion)) {
				return Double.NaN;
			}
			final long key = SubChunkSectionUtil.pack(SubChunkSectionUtil.blockToSubSection(value.x), SubChunkSectionUtil.blockToSubSection(value.y), SubChunkSectionUtil.blockToSubSection(value.z), containingRegion);
			final SubNode<T, N> subNode = subNodes.get(key);
			if (subNode == null) {
				return Double.NaN;
			}
			return subNode.heuristic;
		};
	}

	private static final class SubNode<T, N extends AIPathNode<T, N>> {
		private final @Nullable SubNode<T, N> previous;
		private final ChunkSectionRegionType<T, N> type;
		private final long regionInfo;
		private final ChunkSubSectionRegions<T, N> regions;
		private final ShapeCache shapeCache;
		private final double heuristic;
		private final double cost;

		private SubNode(@Nullable final SubNode<T, N> previous, final ChunkSectionRegionType<T, N> type, final long regionInfo, final ChunkSubSectionRegions<T, N> regions, final ShapeCache shapeCache, final double heuristic, final double cost) {
			this.previous = previous;
			this.type = type;
			this.regionInfo = regionInfo;
			this.regions = regions;
			this.shapeCache = shapeCache;
			this.heuristic = heuristic;
			this.cost = cost;
		}


		public int getNeighbours(final T context, final AStar.CostGetter costGetter, final PathTarget target, final Object[] successors) {
			int i = 0;
			final LongSet outgoingEdges = regions.getOutgoingEdges(SubChunkSectionUtil.unpackFlag(regionInfo), context, null);
			final LongIterator iterator = outgoingEdges.iterator();
			final LongSet regions = new LongOpenHashSet();
			while (iterator.hasNext()) {
				final long blockPos = iterator.nextLong();
				final int x = BlockPos.unpackLongX(blockPos);
				final int y = BlockPos.unpackLongY(blockPos);
				final int z = BlockPos.unpackLongZ(blockPos);
				final ChunkSubSectionRegions<T, N> neighbourRegions = shapeCache.getRegions(x, y, z, type);
				if (neighbourRegions != null) {
					final int containingRegion = neighbourRegions.getContainingRegion(x, y, z);
					if (neighbourRegions.isValidRegion(containingRegion)) {
						final long key = SubChunkSectionUtil.pack(SubChunkSectionUtil.blockToSubSection(x), SubChunkSectionUtil.blockToSubSection(y), SubChunkSectionUtil.blockToSubSection(z), containingRegion);
						if (regions.add(key)) {
							final long pos = ChunkSubSectionRegions.getRepresentativeBlockPos(x, y, z);
							final double heuristic = target.heuristic(BlockPos.unpackLongX(pos), BlockPos.unpackLongY(pos), BlockPos.unpackLongZ(pos));
							final double cost = 1 + this.cost;
							if (costGetter.cost(key) > cost) {
								successors[i++] = new SubNode<>(this, type, key, neighbourRegions, shapeCache, heuristic, cost);
							}
						}
					}
				}
			}
			return i;
		}
	}
}
