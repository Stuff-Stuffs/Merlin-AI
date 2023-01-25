package io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.util.AStar;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai_test.common.BasicAIPathNode;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class TestNodeProducer implements NeighbourGetter<Entity, BasicAIPathNode> {
	private final ValidLocationSetType<BasicLocationType> locationSetType;

	public TestNodeProducer(final ValidLocationSetType<BasicLocationType> locationSetType) {
		this.locationSetType = locationSetType;
	}

	private BasicAIPathNode createDoubleHeightChecked(final int x, final int y, final int z, final BasicAIPathNode prev, final ShapeCache shapeCache, final AStar.CostGetter costGetter) {
		final BasicLocationType walkable = getLocationType(x, y + 1, z, shapeCache);
		final BasicLocationType groundWalkable = getLocationType(x, y, z, shapeCache);
		if (groundWalkable != BasicLocationType.CLOSED && walkable == BasicLocationType.OPEN && costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost + 1) {
			return new BasicAIPathNode(x, y, z, prev.cost + 1, groundWalkable, prev);
		}
		return null;
	}

	private BasicAIPathNode createAir(final int x, final int y, final int z, final BasicAIPathNode prev, final ShapeCache shapeCache, final AStar.CostGetter costGetter) {
		if (costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost + 1 && getLocationType(x, y, z, shapeCache) == BasicLocationType.OPEN) {
			return new BasicAIPathNode(x, y, z, prev.cost + 1, BasicLocationType.OPEN, prev);
		}
		return null;
	}

	private BasicAIPathNode createBasic(final int x, final int y, final int z, final BasicAIPathNode prev, final ShapeCache shapeCache, final AStar.CostGetter costGetter) {
		if (costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost + 1 && getLocationType(x, y, z, shapeCache) == BasicLocationType.GROUND) {
			return new BasicAIPathNode(x, y, z, prev.cost + 1, BasicLocationType.GROUND, prev);
		}
		return null;
	}

	private BasicAIPathNode createAuto(final int x, final int y, final int z, final BasicAIPathNode prev, final ShapeCache shapeCache, final AStar.CostGetter costGetter) {
		final BasicLocationType type;
		if (costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost + 1 && (type = getLocationType(x, y, z, shapeCache)) != BasicLocationType.CLOSED) {
			final boolean ground = type == BasicLocationType.GROUND;
			return new BasicAIPathNode(x, y, z, prev.cost + (ground ? 10 : 1), ground ? BasicLocationType.GROUND : BasicLocationType.OPEN, prev);
		}
		return null;
	}

	private BasicLocationType getLocationType(final int x, final int y, final int z, final ShapeCache shapeCache) {
		return shapeCache.getLocationType(x, y, z, locationSetType, null);
	}

	@Override
	public @Nullable BasicAIPathNode createStartNode(final ShapeCache cache, final int x, final int y, final int z) {
		final BasicLocationType locationType = cache.getLocationType(x, y, z, locationSetType, null);
		if (locationType == BasicLocationType.CLOSED) {
			return null;
		}
		return new BasicAIPathNode(x, y, z, 0, locationType, null);
	}

	@Override
	public int getNeighbours(final ShapeCache cache, final BasicAIPathNode previous, final Entity context, final AStar.CostGetter costGetter, final Object[] successors) {
		int i = 0;
		BasicAIPathNode node;
		node = createBasic(previous.x + 1, previous.y, previous.z, previous, cache, costGetter);
		if (node != null) {
			successors[i++] = node;
		}
		node = createBasic(previous.x - 1, previous.y, previous.z, previous, cache, costGetter);
		if (node != null) {
			successors[i++] = node;
		}
		node = createBasic(previous.x, previous.y, previous.z + 1, previous, cache, costGetter);
		if (node != null) {
			successors[i++] = node;
		}
		node = createBasic(previous.x, previous.y, previous.z - 1, previous, cache, costGetter);
		if (node != null) {
			successors[i++] = node;
		}

		//FALL DIAGONAL
		if (previous.type == BasicLocationType.GROUND) {
			node = createDoubleHeightChecked(previous.x + 1, previous.y - 1, previous.z, previous, cache, costGetter);
			if (node != null) {
				successors[i++] = node;
			}
			node = createDoubleHeightChecked(previous.x - 1, previous.y - 1, previous.z, previous, cache, costGetter);
			if (node != null) {
				successors[i++] = node;
			}
			node = createDoubleHeightChecked(previous.x, previous.y - 1, previous.z + 1, previous, cache, costGetter);
			if (node != null) {
				successors[i++] = node;
			}
			node = createDoubleHeightChecked(previous.x, previous.y - 1, previous.z - 1, previous, cache, costGetter);
			if (node != null) {
				successors[i++] = node;
			}
		}

		//Jump
		if (previous.type == BasicLocationType.GROUND) {
			node = createAir(previous.x, previous.y + 1, previous.z, previous, cache, costGetter);
			if (node != null) {
				successors[i++] = node;
			}
		}
		//down
		if (previous.type == BasicLocationType.OPEN) {
			node = createAuto(previous.x, previous.y - 1, previous.z, previous, cache, costGetter);
			if (node != null) {
				successors[i++] = node;
			}
		}
		return i;
	}
}
