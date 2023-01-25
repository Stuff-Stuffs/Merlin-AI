package io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test;

import io.github.artificial_intellicrafters.merlin_ai.api.util.AStar;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.MerlinAI;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class AIPather<T, N extends AIPathNode<T, N>> {
	private final World world;
	private final NeighbourGetter<T, N> neighbourGetter;
	private final Function<T, BlockPos> startingPositionRetriever;

	public AIPather(final World world, final NeighbourGetter<T, N> neighbourGetter, final Function<T, BlockPos> startingPositionRetriever) {
		this.world = world;
		this.neighbourGetter = neighbourGetter;
		this.startingPositionRetriever = startingPositionRetriever;
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
		final AStar.PathInfo<N> path = AStar.findPath(startingNode, context, node -> BlockPos.asLong(node.x, node.y, node.z), (previous, context1, costGetter, successors) -> neighbourGetter.getNeighbours(cache, previous, context1, costGetter, successors), node -> node.previous, node -> node.cost, node -> pathTarget.heuristic(node.x, node.y, node.z), pathTarget.getRadius(), max, partial);
		if (path.path() == null) {
			return new PathInfo<>(path.nodesConsidered(), null);
		} else {
			return new PathInfo<>(path.nodesConsidered(), new AIPath<>(path.path()));
		}
	}

	private record PathInfo<T, N extends AIPathNode<T, N>>(int nodesConsidered, @Nullable AIPath<T, N> path) {
	}
}
