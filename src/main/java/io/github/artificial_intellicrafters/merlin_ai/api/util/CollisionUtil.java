package io.github.artificial_intellicrafters.merlin_ai.api.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public final class CollisionUtil {
	private static final VoxelShape FULL_CUBE = VoxelShapes.fullCube();
	private static final VoxelShape EMPTY = VoxelShapes.empty();

	public static boolean doesCollide(final Box box, final ShapeCache world) {
		final int minX = MathHelper.floor(box.minX) - 1;
		final int maxX = MathHelper.floor(box.maxX) + 1;
		final int minY = MathHelper.floor(box.minY) - 1;
		final int maxY = MathHelper.floor(box.maxY) + 1;
		final int minZ = MathHelper.floor(box.minZ) - 1;
		final int maxZ = MathHelper.floor(box.maxZ) + 1;
		VoxelShape boxShape = null;
		for (int x = minX; x <= maxX; x++) {
			final int xEdge = x == minX || x == maxX ? 1 : 0;
			for (int y = minY; y <= maxY; y++) {
				final int yEdge = y == minY || y == maxY ? 1 : 0;
				for (int z = minZ; z <= maxZ; z++) {
					final int count = (z == minZ || z == maxZ ? 1 : 0) + xEdge + yEdge;
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
						} else if (VoxelShapes.matchesAnywhere((boxShape == null ? boxShape = VoxelShapes.cuboid(box) : boxShape), voxelShape, BooleanBiFunction.AND)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public static FloorCollision open(final Box box, final double floorPercent, final ShapeCache world) {
		final int minX = MathHelper.floor(box.minX) - 1;
		final int maxX = MathHelper.floor(box.maxX) + 1;
		final int minY = MathHelper.floor(box.minY) - 1;
		final int maxY = MathHelper.floor(box.maxY) + 1;
		final int minZ = MathHelper.floor(box.minZ) - 1;
		final int maxZ = MathHelper.floor(box.maxZ) + 1;
		final int floor = MathHelper.floor((box.maxY - box.minY) * floorPercent) + minY + 1;
		VoxelShape boxShape = null;
		boolean hitFloor = false;
		for (int y = minY; y <= maxY; y++) {
			final int yEdge = y == minY || y == maxY ? 1 : 0;
			final boolean f = y <= floor;
			for (int x = minX; x <= maxX; x++) {
				final int xEdge = x == minX || x == maxX ? 1 : 0;
				for (int z = minZ; z <= maxZ; z++) {
					final int count = (z == minZ || z == maxZ ? 1 : 0) + xEdge + yEdge;
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
								if (f) {
									hitFloor = true;
								} else {
									return FloorCollision.CLOSED;
								}
							}
						} else if (VoxelShapes.matchesAnywhere((boxShape == null ? boxShape = VoxelShapes.cuboid(box) : boxShape), voxelShape, BooleanBiFunction.AND)) {
							if (f) {
								hitFloor = true;
							} else {
								return FloorCollision.CLOSED;
							}
						}
					}
				}
			}
		}
		return hitFloor ? FloorCollision.FLOOR : FloorCollision.OPEN;
	}

	public enum FloorCollision {
		OPEN,
		FLOOR,
		CLOSED
	}

	private CollisionUtil() {
	}
}
