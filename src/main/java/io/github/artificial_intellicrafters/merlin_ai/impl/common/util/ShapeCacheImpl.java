package io.github.artificial_intellicrafters.merlin_ai.impl.common.util;

import io.github.artificial_intellicrafters.merlin_ai.api.AIWorld;
import io.github.artificial_intellicrafters.merlin_ai.api.ChunkRegionGraph;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutionContext;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.PathingChunkSection;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkCache;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class ShapeCacheImpl extends ChunkCache implements ShapeCache {
	private static final long DEFAULT_KEY = HashCommon.mix(BlockPos.asLong(0, 2049, 0));
	private static final BlockState AIR = Blocks.AIR.getDefaultState();
	private static final VoxelShape EMPTY = VoxelShapes.empty();
	private final int cacheMask;
	private final long[] keys;
	private final BlockPos.Mutable mutable = new BlockPos.Mutable();
	private final BlockState[] blockStates;
	private final VoxelShape[] collisionShapes;
	private final int smallCacheMask;
	private final long[] entryKeys;
	private final ChunkRegionGraph.Entry[] entries;

	public ShapeCacheImpl(final World world, final BlockPos minPos, final BlockPos maxPos, final int cacheSize) {
		super(world, minPos, maxPos);
		cacheMask = cacheSize - 1;
		keys = new long[cacheSize];
		blockStates = new BlockState[cacheSize];
		collisionShapes = new VoxelShape[cacheSize];
		Arrays.fill(keys, DEFAULT_KEY);
		final int smallCacheSize = Math.max(cacheSize / 4, 16);
		smallCacheMask = smallCacheSize - 1;
		entryKeys = new long[smallCacheSize];
		entries = new ChunkRegionGraph.Entry[smallCacheSize];
		Arrays.fill(entryKeys, DEFAULT_KEY);
	}

	@Override
	public World getDelegate() {
		return world;
	}

	@Override
	public PathingChunkSection getPathingChunk(final int x, final int y, final int z) {
		if (world.isOutOfHeightLimit(y)) {
			return null;
		}
		final Chunk chunk = getChunk(x >> 4, z >> 4);
		if (chunk == null) {
			return null;
		}
		final ChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
		if (section == null) {
			return null;
		}
		return (PathingChunkSection) section;
	}

	private Chunk getChunk(final int chunkX, final int chunkZ) {
		final int k = chunkX - minX;
		final int l = chunkZ - minZ;
		if (k >= 0 && k < chunks.length && l >= 0 && l < chunks[k].length) {
			return chunks[k][l];
		} else {
			return null;
		}
	}

	@Override
	public boolean doesLocationSetExist(final int x, final int y, final int z, final ValidLocationSetType<?> type, @Nullable final AITaskExecutionContext executionContext) {
		final long idx = HashCommon.mix(BlockPos.asLong(x >> 4, y >> 4, z >> 4));
		final int pos = (int) (idx) & smallCacheMask;
		if (entryKeys[pos] == idx) {
			final ValidLocationSet<?> set = entries[pos] == null ? null : entries[pos].getValidLocationSet(type, world.getTime(), executionContext, false);
			return set != null;
		}
		populateCacheSmall(x, y, z, idx, pos);
		final ValidLocationSet<?> set = entries[pos] == null ? null : entries[pos].getValidLocationSet(type, world.getTime(), executionContext, false);
		return set != null;
	}

	private void populateCacheSmall(final int x, final int y, final int z, final long idx, final int pos) {
		final ChunkRegionGraph.Entry entry = ((AIWorld) world).merlin_ai$getChunkGraph().getEntry(x, y, z);
		entryKeys[pos] = idx;
		entries[pos] = entry;
	}

	private void populateCache(final int x, final int y, final int z, final long idx, final int pos) {
		final Chunk chunk = getChunk(x >> 4, z >> 4);
		if (chunk != null) {
			final ChunkSection chunkSection = chunk.getSectionArray()[world.getSectionIndex(y)];
			keys[pos] = idx;
			if (chunkSection == null) {
				blockStates[pos] = AIR;
				collisionShapes[pos] = EMPTY;
			} else {
				final BlockState state = chunkSection.getBlockState(x & 15, y & 15, z & 15);
				blockStates[pos] = state;
				collisionShapes[pos] = state.getCollisionShape(world, mutable.set(x, y, z));
			}
		} else {
			keys[pos] = idx;
			blockStates[pos] = AIR;
			collisionShapes[pos] = EMPTY;
		}
	}

	@Override
	public BlockState getBlockState(final int x, final int y, final int z) {
		if (world.isOutOfHeightLimit(y)) {
			return AIR;
		} else {
			final long idx = HashCommon.mix(BlockPos.asLong(x, y, z));
			final int pos = (int) (idx & cacheMask);
			if (keys[pos] == idx) {
				return blockStates[pos];
			}
			populateCache(x, y, z, idx, pos);
			return blockStates[pos];
		}
	}

	@Override
	public VoxelShape getCollisionShape(final int x, final int y, final int z) {
		if (world.isOutOfHeightLimit(y)) {
			return EMPTY;
		} else {
			final long idx = HashCommon.mix(BlockPos.asLong(x, y, z));
			final int pos = (int) (idx & cacheMask);
			if (keys[pos] == idx) {
				return collisionShapes[pos];
			}
			populateCache(x, y, z, idx, pos);
			return collisionShapes[pos];
		}
	}

	@Override
	public ChunkRegionGraph.Entry getEntry(final int x, final int y, final int z) {
		final long idx = HashCommon.mix(BlockPos.asLong(x >> 4, y >> 4, z >> 4));
		final int pos = (int) (idx) & smallCacheMask;
		if (entryKeys[pos] == idx) {
			return entries[pos];
		}
		populateCacheSmall(x, y, z, idx, pos);
		return entries[pos];
	}

	@Override
	public BlockState getBlockState(final BlockPos pos) {
		if (world.isOutOfHeightLimit(pos.getY())) {
			return Blocks.AIR.getDefaultState();
		} else {
			return getBlockState(pos.getX(), pos.getY(), pos.getZ());
		}
	}
}
