package io.github.artificial_intellicrafters.merlin_ai.api.util;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.util.WorldCacheImpl;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.CollisionView;
import net.minecraft.world.World;

public interface WorldCache extends BlockView, CollisionView {
	BlockState getBlockState(int x, int y, int z);

	VoxelShape getCollisionShape(int x, int y, int z);

	default <T> T getLocationType(final BlockPos pos, final ValidLocationSetType<T> validLocationSetType) {
		return getLocationType(pos.getX(), pos.getY(), pos.getZ(), validLocationSetType);
	}

	<T> T getLocationType(int x, int y, int z, ValidLocationSetType<T> validLocationSetType);

	static WorldCache create(final World world, final BlockPos minPos, final BlockPos maxPos) {
		return new WorldCacheImpl(world, minPos, maxPos);
	}
}
