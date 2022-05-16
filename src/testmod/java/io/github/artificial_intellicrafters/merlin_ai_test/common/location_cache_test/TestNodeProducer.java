package io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.path.NeighbourGetter;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai_test.common.BasicAIPathNode;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TestNodeProducer implements NeighbourGetter<Entity, BasicAIPathNode<Entity>> {
	private final Entity aiEntity;
	private final World world;
	private final ValidLocationSetType<BasicLocationType> locationSetType;
	private ShapeCache shapeCache;

	public TestNodeProducer(final Entity aiEntity, final World world, final ValidLocationSetType<BasicLocationType> locationSetType) {
		this.aiEntity = aiEntity;
		this.world = world;
		this.locationSetType = locationSetType;
	}

	private BasicAIPathNode<Entity> createDoubleHeightChecked(final int x, final int y, final int z, final BasicAIPathNode<Entity> prev) {
		final BasicLocationType walkable = isWalkable(x, y + 1, z);
		final BasicLocationType groundWalkable = isWalkable(x, y, z);
		if (groundWalkable != BasicLocationType.CLOSED && walkable != BasicLocationType.CLOSED) {
			return new BasicAIPathNode<>(x, y, z, prev.cost + 1, walkable);
		}
		return null;
	}

	private BasicAIPathNode<Entity> createAir(final int x, final int y, final int z, final BasicAIPathNode<Entity> prev) {
		if (isWalkable(x, y, z) == BasicLocationType.OPEN) {
			return new BasicAIPathNode<>(x, y, z, prev.cost + 1, BasicLocationType.OPEN);
		}
		return null;
	}

	private BasicAIPathNode<Entity> createBasic(final int x, final int y, final int z, final BasicAIPathNode<Entity> prev) {
		if (isWalkable(x, y, z) == BasicLocationType.GROUND) {
			return new BasicAIPathNode<>(x, y, z, prev.cost + 1, BasicLocationType.GROUND);
		}
		return null;
	}

	private BasicAIPathNode<Entity> createAuto(final int x, final int y, final int z, final BasicAIPathNode<Entity> prev) {
		final BasicLocationType type = isWalkable(x, y, z);
		if (type != BasicLocationType.CLOSED) {
			final boolean ground = type == BasicLocationType.GROUND;
			return new BasicAIPathNode<>(x, y, z, prev.cost + (ground ? 10 : 1), ground ? BasicLocationType.GROUND : BasicLocationType.OPEN);
		}
		return null;
	}

	private BasicLocationType isWalkable(final int x, final int y, final int z) {
		return shapeCache.getLocationType(x, y, z, locationSetType);
	}

	@Override
	public BasicAIPathNode<Entity> createStartNode(final ShapeCache cache, final Entity context) {
		final BlockPos pos = aiEntity.getBlockPos();
		shapeCache = cache;
		final boolean walk = world.getBlockState(pos.down()).hasSolidTopSurface(world, pos.down(), aiEntity);
		return new BasicAIPathNode<>(pos.getX(), pos.getY(), pos.getZ(), 0, walk ? BasicLocationType.GROUND : BasicLocationType.OPEN);
	}

	@Override
	public int getNeighbours(final ShapeCache cache, final BasicAIPathNode<Entity> previous, final Object[] successors) {
		int i = 0;
		BasicAIPathNode<Entity> node;
		node = createBasic(previous.x + 1, previous.y, previous.z, previous);
		if (node != null) {
			successors[i++] = node;
		}
		node = createBasic(previous.x - 1, previous.y, previous.z, previous);
		if (node != null) {
			successors[i++] = node;
		}
		node = createBasic(previous.x, previous.y, previous.z + 1, previous);
		if (node != null) {
			successors[i++] = node;
		}
		node = createBasic(previous.x, previous.y, previous.z - 1, previous);
		if (node != null) {
			successors[i++] = node;
		}

		//FALL DIAGONAL
		if (previous.type != BasicLocationType.OPEN) {
			node = createDoubleHeightChecked(previous.x + 1, previous.y - 1, previous.z, previous);
			if (node != null) {
				successors[i++] = node;
			}
			node = createDoubleHeightChecked(previous.x - 1, previous.y - 1, previous.z, previous);
			if (node != null) {
				successors[i++] = node;
			}
			node = createDoubleHeightChecked(previous.x, previous.y - 1, previous.z + 1, previous);
			if (node != null) {
				successors[i++] = node;
			}
			node = createDoubleHeightChecked(previous.x, previous.y - 1, previous.z - 1, previous);
			if (node != null) {
				successors[i++] = node;
			}
		}

		//Jump
		if (previous.type != BasicLocationType.OPEN) {
			node = createAir(previous.x, previous.y + 1, previous.z, previous);
			if (node != null) {
				successors[i++] = node;
			}
		}
		//down
		if (previous.type != BasicLocationType.OPEN) {
			node = createAuto(previous.x, previous.y - 1, previous.z, previous);
			if (node != null) {
				successors[i++] = node;
			}
		}
		return i;
	}
}
