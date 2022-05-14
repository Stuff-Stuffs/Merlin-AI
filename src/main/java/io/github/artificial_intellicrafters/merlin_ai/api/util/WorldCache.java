package io.github.artificial_intellicrafters.merlin_ai.api.util;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.util.WorldCacheImpl;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public interface WorldCache extends BlockView {
	BlockState getBlockState(int x, int y, int z);

	VoxelShape getCollisionShape(int x, int y, int z);

	default <T> T getLocationType(final BlockPos pos, final ValidLocationSetType<T> validLocationSetType) {
		return getLocationType(pos.getX(), pos.getY(), pos.getZ(), validLocationSetType);
	}

	<T> T getLocationType(int x, int y, int z, ValidLocationSetType<T> validLocationSetType);

	static int computeCacheSize(final BlockPos minPos, final BlockPos maxPos) {
		if (minPos.compareTo(maxPos) >= 0) {
			throw new IllegalArgumentException("Argument minPos must be less than maxPos!");
		}
		final ChunkPos min = new ChunkPos(minPos);
		final ChunkPos max = new ChunkPos(maxPos);
		final int xSideLength = max.getEndX() - min.getStartX();
		final int zSideLength = max.getEndZ() - min.getStartZ();

		final int averageSideLength = (xSideLength + zSideLength) / 2;
		//make the cache size according to 4 * sidelength
		int cacheSizeTarget = (averageSideLength * 4) - 1;
		//Round up to power of 2 using bit magic
		cacheSizeTarget = cacheSizeTarget | (cacheSizeTarget >>> 1);
		cacheSizeTarget = cacheSizeTarget | (cacheSizeTarget >>> 2);
		cacheSizeTarget = cacheSizeTarget | (cacheSizeTarget >>> 4);
		cacheSizeTarget = cacheSizeTarget | (cacheSizeTarget >>> 8);
		cacheSizeTarget = cacheSizeTarget | (cacheSizeTarget >>> 16);
		cacheSizeTarget = cacheSizeTarget + 1;

		//These are arbitrary
		final int minCacheSize = 16;
		final int maxCacheSize = 8192;
		return Math.max(Math.min(cacheSizeTarget, maxCacheSize), minCacheSize);
	}

	static WorldCache create(final World world, final BlockPos minPos, final BlockPos maxPos) {
		return create(world, minPos, maxPos, computeCacheSize(minPos, maxPos));
	}

	static WorldCache create(final World world, final BlockPos minPos, final BlockPos maxPos, final int cacheSize) {
		if (minPos.compareTo(maxPos) >= 0) {
			throw new IllegalArgumentException("Argument minPos must be less than maxPos!");
		}
		//Check for power of two cache size
		if ((cacheSize & (cacheSize - 1)) != 0) {
			throw new IllegalArgumentException("Cache size must be a power of 2!");
		}
		return new WorldCacheImpl(world, minPos, maxPos, cacheSize);
	}
}
