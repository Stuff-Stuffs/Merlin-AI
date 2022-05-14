package io.github.artificial_intellicrafters.merlin_ai.impl.common.util;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.util.WorldCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.location_caching.PathingChunkSection;
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

import java.util.Arrays;

public class WorldCacheImpl extends ChunkCache implements WorldCache {
	private static final int CACHE_SIZE = 8192;
	private static final int CACHE_MASK = CACHE_SIZE - 1;
	private static final long DEFAULT_KEY = HashCommon.mix(BlockPos.asLong(~0, ~0, ~0));
	private static final BlockState AIR = Blocks.AIR.getDefaultState();
	private static final VoxelShape EMPTY = VoxelShapes.empty();
	private final long[] keys = new long[CACHE_SIZE];
	private final BlockPos.Mutable mutable = new BlockPos.Mutable();
	private final BlockState[] blockStates = new BlockState[CACHE_SIZE];
	private final VoxelShape[] collisionShapes = new VoxelShape[CACHE_SIZE];

	private int cacheHits = 0;
	private int cacheMisses = 0;
	private int cacheEvictions = 0;

	public WorldCacheImpl(final World world, final BlockPos minPos, final BlockPos maxPos) {
		super(world, minPos, maxPos);
		Arrays.fill(keys, DEFAULT_KEY);
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

	public <T> T getLocationType(final int x, final int y, final int z, final ValidLocationSetType<T> type) {
		final Chunk chunk = getChunk(x >> 4, z >> 4);
		if (chunk == null) {
			return type.universeInfo().getDefaultValue();
		}
		final ChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
		if (section == null) {
			return type.universeInfo().getDefaultValue();
		}
		return ((PathingChunkSection) section).vaa$getValidLocationSet(type, x, y, z, this).get(x, y, z);
	}

	private void populateCache(final int x, final int y, final int z, final long idx, final int pos) {
		final Chunk chunk = getChunk(x >> 4, z >> 4);
		if (keys[pos] != DEFAULT_KEY) {
			cacheEvictions++;
		}
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

	public BlockState getBlockState(final int x, final int y, final int z) {
		if (world.isOutOfHeightLimit(y)) {
			return AIR;
		} else {
			final long idx = HashCommon.mix(BlockPos.asLong(x, y, z));
			final int pos = (int) (idx & CACHE_MASK);
			if (keys[pos] == idx) {
				cacheHits++;
				return blockStates[pos];
			}
			cacheMisses++;
			populateCache(x, y, z, idx, pos);
			return blockStates[pos];
		}
	}

	public VoxelShape getCollisionShape(final int x, final int y, final int z) {
		if (world.isOutOfHeightLimit(y)) {
			return EMPTY;
		} else {
			final long idx = HashCommon.mix(BlockPos.asLong(x, y, z));
			final int pos = (int) (idx & CACHE_MASK);
			if (keys[pos] == idx) {
				cacheHits++;
				return collisionShapes[pos];
			}
			cacheMisses++;
			populateCache(x, y, z, idx, pos);
			return collisionShapes[pos];
		}
	}

	public void stats() {
		System.out.println("Hits: " + cacheHits + ", Misses: " + cacheMisses + ", Evictions: " + cacheEvictions);
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
