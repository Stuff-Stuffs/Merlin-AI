package io.github.artificial_intellicrafters.merlin_ai.api.util;

import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegion;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegionConnectivityGraph;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.HierarchyInfo;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.PathingChunkSection;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.hierarchy.ChunkSectionRegionsImpl;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.function.ToDoubleFunction;

public final class CoarseUtil {

	public static AStar.PathInfo<RegionInfo> findPathDynamicHeuristic(final ChunkSectionRegion start, final ShapeCache cache, final ToDoubleFunction<BlockPos> heuristic, final RegionHeuristic regionHeuristic, final double error, final double maxCost, final boolean partial, final HierarchyInfo<?, ?, ?, ?> hierarchyInfo) {
		final ToDoubleFunction<RegionInfo> heuristicMapped = value -> {
			final long id = value.region().id();
			final ChunkSectionRegion region = cache.getRegion(id, hierarchyInfo, null);
			if (region != null) {
				final int sx = ChunkSectionRegionsImpl.unpackChunkSectionPosX(id);
				final int sy = ChunkSectionRegionsImpl.unpackChunkSectionPosY(id, cache);
				final int sz = ChunkSectionRegionsImpl.unpackChunkSectionPosZ(id);
				regionHeuristic.setup(sx, sy, sz);
				for (final short repr : region.all()) {
					if (regionHeuristic.add(repr)) {
						break;
					}
				}
				return regionHeuristic.calculate();
			}
			return Double.POSITIVE_INFINITY;
		};
		return findPath(start, cache, heuristicMapped, error, maxCost, partial, hierarchyInfo);
	}
	public static AStar.PathInfo<RegionInfo> findPath(final ChunkSectionRegion start, final ShapeCache cache, final RegionHeuristic regionHeuristic, final double error, final double maxCost, final boolean partial, final HierarchyInfo<?, ?, ?, ?> hierarchyInfo) {
		final ToDoubleFunction<RegionInfo> heuristicMapped = value -> {
			final long id = value.region().id();
			final ChunkSectionRegion region = cache.getRegion(id, hierarchyInfo, null);
			if (region != null) {
				final int sx = ChunkSectionRegionsImpl.unpackChunkSectionPosX(id);
				final int sy = ChunkSectionRegionsImpl.unpackChunkSectionPosY(id, cache);
				final int sz = ChunkSectionRegionsImpl.unpackChunkSectionPosZ(id);
				regionHeuristic.setup(sx, sy, sz);
				for (final short repr : region.all()) {
					if (regionHeuristic.add(repr)) {
						break;
					}
				}
				return regionHeuristic.calculate();
			}
			return Double.POSITIVE_INFINITY;
		};
		return findPath(start, cache, heuristicMapped, error, maxCost, partial, hierarchyInfo);
	}

	public static AStar.PathInfo<RegionInfo> findPathAllHeuristic(final ChunkSectionRegion start, final ShapeCache cache, final ToDoubleFunction<BlockPos> heuristic, final double error, final double maxCost, final boolean partial, final HierarchyInfo<?, ?, ?, ?> hierarchyInfo) {
		final BlockPos.Mutable mutable = new BlockPos.Mutable();
		final ToDoubleFunction<RegionInfo> heuristicMapped = value -> {
			final long id = value.region().id();
			final ChunkSectionRegion region = cache.getRegion(id, hierarchyInfo, null);
			if (region != null) {
				final int sx = ChunkSectionRegionsImpl.unpackChunkSectionPosX(id) << 4;
				final int sy = ChunkSectionRegionsImpl.unpackChunkSectionPosY(id, cache) << 4;
				final int sz = ChunkSectionRegionsImpl.unpackChunkSectionPosZ(id) << 4;
				double min = Double.POSITIVE_INFINITY;
				for (final short repr : region.all()) {
					final int x = PathingChunkSection.unpackLocalX(repr) + sx;
					final int y = PathingChunkSection.unpackLocalY(repr) + sy;
					final int z = PathingChunkSection.unpackLocalZ(repr) + sz;
					min = Math.min(heuristic.applyAsDouble(mutable.set(x, y, z)), min);
				}
				return min;
			}
			return Double.POSITIVE_INFINITY;
		};
		return findPath(start, cache, heuristicMapped, error, maxCost, partial, hierarchyInfo);
	}

	public static AStar.PathInfo<RegionInfo> findPathAnyHeuristic(final ChunkSectionRegion start, final ShapeCache cache, final ToDoubleFunction<BlockPos> heuristic, final double error, final double maxCost, final boolean partial, final HierarchyInfo<?, ?, ?, ?> hierarchyInfo) {
		final BlockPos.Mutable mutable = new BlockPos.Mutable();
		final ToDoubleFunction<RegionInfo> heuristicMapped = value -> {
			final long id = value.region().id();
			final ChunkSectionRegion region = cache.getRegion(id, hierarchyInfo, null);
			if (region != null) {
				final int sx = ChunkSectionRegionsImpl.unpackChunkSectionPosX(id) << 4;
				final int sy = ChunkSectionRegionsImpl.unpackChunkSectionPosY(id, cache) << 4;
				final int sz = ChunkSectionRegionsImpl.unpackChunkSectionPosZ(id) << 4;
				final short repr = region.any();
				final int x = PathingChunkSection.unpackLocalX(repr) + sx;
				final int y = PathingChunkSection.unpackLocalY(repr) + sy;
				final int z = PathingChunkSection.unpackLocalZ(repr) + sz;
				return heuristic.applyAsDouble(mutable.set(x, y, z));
			}
			return Double.POSITIVE_INFINITY;
		};
		return findPath(start, cache, heuristicMapped, error, maxCost, partial, hierarchyInfo);
	}

	public static AStar.PathInfo<RegionInfo> findPath(final ChunkSectionRegion start, final ShapeCache cache, final ToDoubleFunction<RegionInfo> heuristic, final double error, final double maxCost, final boolean partial, final HierarchyInfo<?, ?, ?, ?> hierarchyInfo) {
		final RegionInfo startInfo = new RegionInfo(null, start, 0);
		return AStar.findPath(startInfo, null, i -> i.region().id(), (previous, context, costGetter, successors) -> {
			final long id = previous.region().id();
			final int x = (ChunkSectionRegionsImpl.unpackChunkSectionPosX(id) << 4);
			final int y = (ChunkSectionRegionsImpl.unpackChunkSectionPosY(id, cache) << 4);
			final int z = (ChunkSectionRegionsImpl.unpackChunkSectionPosZ(id) << 4);
			final ChunkSectionRegionConnectivityGraph<?> graph = cache.getGraph(x, y, z, hierarchyInfo, null);
			if (graph != null) {
				int i = 0;
				final LongIterator iterator = graph.unconditionalLinks(id);
				while (iterator.hasNext()) {
					final long l = iterator.nextLong();
					final double cost = costGetter.cost(l);
					if (Double.isNaN(cost) || cost >= Double.MAX_VALUE) {
						final ChunkSectionRegion region = cache.getRegion(l, hierarchyInfo, null);
						if (region != null) {
							successors[i++] = new RegionInfo(previous, region, previous.cost() + Integer.highestOneBit(region.all().length) * 8 + 1);
						}
					}
				}
				return i;
			}
			return 0;
		}, RegionInfo::prev, RegionInfo::cost, heuristic, error, maxCost, partial);
	}

	public record RegionInfo(@Nullable RegionInfo prev, ChunkSectionRegion region, double cost) {
	}

	public interface RegionHeuristic {
		void setup(int chunkSectionX, int chunkSectionY, int chunkSectionZ);

		boolean add(short packed);

		double calculate();
	}

	private CoarseUtil() {
	}
}
