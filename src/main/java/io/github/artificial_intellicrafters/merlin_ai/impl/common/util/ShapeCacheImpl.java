package io.github.artificial_intellicrafters.merlin_ai.impl.common.util;

import io.github.artificial_intellicrafters.merlin_ai.api.AIWorld;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegion;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.region.graph.ChunkRegionGraph;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
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
	private static final long DEFAULT_KEY = HashCommon.mix(BlockPos.asLong(0, Integer.MAX_VALUE, 0));
	private static final BlockState AIR = Blocks.AIR.getDefaultState();
	private static final VoxelShape EMPTY = VoxelShapes.empty();
	private final int cacheMask;
	private final long[] keys;
	private final BlockPos.Mutable mutable = new BlockPos.Mutable();
	private final BlockState[] blockStates;
	private final VoxelShape[] collisionShapes;
	private final int smallCacheMask;
	private final long[] locationKeys;
	private final ValidLocationSet<?>[] locationSets;
	private final long[] regionKeys;
	private final ChunkSectionRegions<?, ?>[] regionSets;

	public ShapeCacheImpl(final World world, final BlockPos minPos, final BlockPos maxPos, final int cacheSize) {
		super(world, minPos, maxPos);
		cacheMask = cacheSize - 1;
		keys = new long[cacheSize];
		blockStates = new BlockState[cacheSize];
		collisionShapes = new VoxelShape[cacheSize];
		Arrays.fill(keys, DEFAULT_KEY);
		final int smallCacheSize = Math.max(cacheSize / 8, 16);
		smallCacheMask = smallCacheSize - 1;
		locationKeys = new long[smallCacheSize];
		locationSets = new ValidLocationSet[smallCacheSize];
		Arrays.fill(locationKeys, DEFAULT_KEY);
		regionKeys = new long[smallCacheSize];
		regionSets = new ChunkSectionRegions[smallCacheSize];
		Arrays.fill(regionKeys, DEFAULT_KEY);
	}

	@Override
	public World getDelegate() {
		return world;
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
	public <T> T getLocationType(final int x, final int y, final int z, final ValidLocationSetType<T> type) {
		final long idx = HashCommon.mix(BlockPos.asLong(x >> 4, y >> 4, z >> 4));
		final int pos = (int) (idx) & smallCacheMask;
		if (locationKeys[pos] == idx && locationSets[pos].type() == type) {
			return (T) locationSets[pos].get(x, y, z);
		}
		final ChunkRegionGraph.Entry entry = ((AIWorld) world).merlin_ai$getChunkGraph().getEntry(x, y, z);
		if (entry != null) {
			final ValidLocationSet<T> set = entry.getValidLocationSet(type);
			if (set != null) {
				locationKeys[pos] = idx;
				locationSets[pos] = set;
				return set.get(x, y, z);
			}
		}
		return type.universeInfo().getDefaultValue();
	}

	@Override
	public boolean doesLocationSetExist(final int x, final int y, final int z, final ValidLocationSetType<?> type) {
		final long idx = HashCommon.mix(BlockPos.asLong(x >> 4, y >> 4, z >> 4));
		final int pos = (int) (idx) & smallCacheMask;
		if (locationKeys[pos] == idx && locationSets[pos].type() == type) {
			return true;
		}
		final ChunkRegionGraph.Entry entry = ((AIWorld) world).merlin_ai$getChunkGraph().getEntry(x, y, z);
		if (entry != null) {
			final ValidLocationSet<?> set = entry.getValidLocationSet(type);
			if (set != null) {
				locationKeys[pos] = idx;
				locationSets[pos] = set;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean doesRegionsExist(final int x, final int y, final int z, final ChunkSectionRegionType<?, ?> type) {
		final long idx = HashCommon.mix(BlockPos.asLong(x >> 4, y >> 4, z >> 4));
		final int pos = (int) (idx) & smallCacheMask;
		if (regionKeys[pos] == idx && regionSets[pos].type() == type) {
			return true;
		}
		final ChunkRegionGraph.Entry entry = ((AIWorld) world).merlin_ai$getChunkGraph().getEntry(x, y, z);
		if (entry != null) {
			final ChunkSectionRegions<?, ?> set = entry.getChunkSectionRegions(type);
			if (set != null) {
				regionKeys[pos] = idx;
				regionSets[pos] = set;
				return true;
			}
		}
		return false;
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
	public <T, N extends AIPathNode<T, N>> @Nullable ChunkSectionRegion<T, N> getRegion(final int x, final int y, final int z, final ChunkSectionRegionType<T, N> type) {
		final long idx = HashCommon.mix(BlockPos.asLong(x >> 4, y >> 4, z >> 4));
		final int pos = (int) (idx) & smallCacheMask;
		if (regionKeys[pos] == idx && regionSets[pos].type() == type) {
			return ((ChunkSectionRegions<T, N>) regionSets[pos]).getRegion(x, y, z);
		}
		final ChunkRegionGraph.Entry entry = ((AIWorld) world).merlin_ai$getChunkGraph().getEntry(x, y, z);
		if (entry != null) {
			final ChunkSectionRegions<T, N> set = entry.getChunkSectionRegions(type);
			if (set != null) {
				regionKeys[pos] = idx;
				regionSets[pos] = set;
				return set.getRegion(x, y, z);
			}
		}
		return null;
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
