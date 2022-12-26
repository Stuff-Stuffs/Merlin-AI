package io.github.artificial_intellicrafters.merlin_ai.api.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.CuboidBlockIterator;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public final class CollisionUtil {
	private static final VoxelShape FULL_CUBE = VoxelShapes.fullCube();
	private static final VoxelShape EMPTY = VoxelShapes.empty();

	private static CuboidBlockIterator collisionArea(final Box box) {
		final int minX = MathHelper.floor(box.minX - 0.0001f) - 1;
		final int maxX = MathHelper.floor(box.maxX + 0.0001f) + 1;
		final int minY = MathHelper.floor(box.minY - 0.0001f) - 1;
		final int maxY = MathHelper.floor(box.maxY + 0.0001f) + 1;
		final int minZ = MathHelper.floor(box.minZ - 0.0001f) - 1;
		final int maxZ = MathHelper.floor(box.maxZ + 0.0001f) + 1;
		return new CuboidBlockIterator(minX, minY, minZ, maxX, maxY, maxZ);
	}

	public static boolean doesCollide(final Box box, final ShapeCache world) {
		final CuboidBlockIterator blockIterator = collisionArea(box);
		final VoxelShape boxShape = VoxelShapes.cuboid(box);
		do {
			final int x = blockIterator.getX();
			final int y = blockIterator.getY();
			final int z = blockIterator.getZ();
			final int count = blockIterator.getEdgeCoordinatesCount();
			if (count == 3) {
				continue;
			}
			final BlockState state = world.getBlockState(x, y, z);
			if (state.isAir() || !((count != 1 || state.exceedsCube()) && (count != 2 || state.getBlock() == Blocks.MOVING_PISTON))) {
				continue;
			}
			final VoxelShape voxelShape = world.getCollisionShape(x, y, z);
			if (voxelShape != EMPTY && !voxelShape.isEmpty()) {
				if (voxelShape == FULL_CUBE) {
					if (box.intersects(x, y, z, x + 1, y + 1, z + 1)) {
						return true;
					}
				} else if (VoxelShapes.matchesAnywhere(boxShape, voxelShape, BooleanBiFunction.AND)) {
					return true;
				}
			}
		} while (blockIterator.step());
		return false;
	}

	private CollisionUtil() {
	}
}
