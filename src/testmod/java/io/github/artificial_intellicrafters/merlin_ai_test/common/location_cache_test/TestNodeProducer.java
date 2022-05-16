package io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TestNodeProducer implements NodeProducer {
	private final Entity aiEntity;
	private final World world;
	private final ValidLocationSetType<BasicLocationType> locationSetType;
	private ShapeCache shapeCache;

	public TestNodeProducer(final Entity aiEntity, final World world, final ValidLocationSetType<BasicLocationType> locationSetType) {
		this.aiEntity = aiEntity;
		this.world = world;
		this.locationSetType = locationSetType;
	}


	@Override
	public AIPathNode getStart(final ShapeCache cache) {
		final BlockPos pos = aiEntity.getBlockPos();
		shapeCache = cache;
		final boolean walk = world.getBlockState(pos.down()).hasSolidTopSurface(world, pos.down(), aiEntity);
		return new AIPathNode(pos.getX(), pos.getY(), pos.getZ(), 0, walk ? AIPathNode.Type.LAND : AIPathNode.Type.AIR, null, walk);
	}

	@Override
	public int getNeighbours(final AIPathNode previous, final AIPathNode[] successors) {
		int i = 0;
		AIPathNode node;
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
		if (previous.type != AIPathNode.Type.AIR) {
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
		if (previous.type != AIPathNode.Type.AIR) {
			node = createAir(previous.x, previous.y + 1, previous.z, previous);
			if (node != null) {
				successors[i++] = node;
			}
		}
		//down
		if (previous.type != AIPathNode.Type.LAND) {
			node = createAuto(previous.x, previous.y - 1, previous.z, previous);
			if (node != null) {
				successors[i++] = node;
			}
		}
		return i;
	}

	private AIPathNode createDoubleHeightChecked(final int x, final int y, final int z, final AIPathNode prev) {
		final BasicLocationType walkable = isWalkable(x, y + 1, z);
		final BasicLocationType groundWalkable = isWalkable(x, y, z);
		if (groundWalkable != BasicLocationType.CLOSED && walkable != BasicLocationType.CLOSED) {
			return new AIPathNode(x, y, z, prev.distance + 1, groundWalkable == BasicLocationType.GROUND ? AIPathNode.Type.LAND : AIPathNode.Type.AIR, prev, true);
		}
		return null;
	}

	private AIPathNode createAir(final int x, final int y, final int z, final AIPathNode prev) {
		if (isWalkable(x, y, z) == BasicLocationType.OPEN) {
			return new AIPathNode(x, y, z, prev.distance + 1, AIPathNode.Type.AIR, prev, true);
		}
		return null;
	}

	private AIPathNode createBasic(final int x, final int y, final int z, final AIPathNode prev) {
		if (isWalkable(x, y, z) == BasicLocationType.GROUND) {
			return new AIPathNode(x, y, z, prev.distance + 1, AIPathNode.Type.LAND, prev, true);
		}
		return null;
	}

	private AIPathNode createAuto(final int x, final int y, final int z, final AIPathNode prev) {
		final BasicLocationType type = isWalkable(x, y, z);
		if (type != BasicLocationType.CLOSED) {
			final boolean ground = type == BasicLocationType.GROUND;
			return new AIPathNode(x, y, z, prev.distance + (ground?10:1), ground ? AIPathNode.Type.LAND : AIPathNode.Type.AIR, prev, true);
		}
		return null;
	}

	private BasicLocationType isWalkable(final int x, final int y, final int z) {
		return shapeCache.getLocationType(x, y, z, locationSetType);
	}
}
