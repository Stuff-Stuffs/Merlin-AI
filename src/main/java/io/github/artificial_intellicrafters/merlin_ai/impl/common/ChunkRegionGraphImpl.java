package io.github.artificial_intellicrafters.merlin_ai.impl.common;

import io.github.artificial_intellicrafters.merlin_ai.api.AIWorld;
import io.github.artificial_intellicrafters.merlin_ai.api.ChunkRegionGraph;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.api.util.SubChunkSectionUtil;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.task.RegionAnalysisAITask;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.task.ValidLocationAnalysisChunkSectionAITTask;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class ChunkRegionGraphImpl implements ChunkRegionGraph {
	private final World world;
	private final Long2ReferenceMap<WorldChunk> chunks;
	private final Long2ReferenceMap<EntryImpl> entries;

	public ChunkRegionGraphImpl(final World world) {
		this.world = world;
		chunks = new Long2ReferenceOpenHashMap<>();
		entries = new Long2ReferenceOpenHashMap<>();
	}

	public void load(final WorldChunk chunk) {
		chunks.put(chunk.getPos().toLong(), chunk);
	}

	public void unload(final WorldChunk chunk) {
		final ChunkPos pos = chunk.getPos();
		final int x = SubChunkSectionUtil.blockToSubSection(pos.getStartX());
		final int z = SubChunkSectionUtil.blockToSubSection(pos.getStartZ());
		final int topSectionCoord = chunk.getTopSectionCoord();
		for (int i = chunk.getBottomSectionCoord(); i < topSectionCoord; i++) {
			long key = SubChunkSectionUtil.pack(SubChunkSectionUtil.blockToSubSection(x), SubChunkSectionUtil.blockToSubSection(ChunkSectionPos.getBlockCoord(i)), SubChunkSectionUtil.blockToSubSection(z), 0);
			entries.remove(key);
			key = SubChunkSectionUtil.pack(SubChunkSectionUtil.blockToSubSection(x), SubChunkSectionUtil.blockToSubSection(ChunkSectionPos.getBlockCoord(i)) + 1, SubChunkSectionUtil.blockToSubSection(z), 0);
			entries.remove(key);
		}
	}

	@Override
	public @Nullable EntryImpl getEntry(final long subSectionPos) {
		return getOrCreateEntry(subSectionPos);
	}

	@Override
	public @Nullable EntryImpl getEntry(final int x, final int y, final int z) {
		return getOrCreateEntry(SubChunkSectionUtil.pack(SubChunkSectionUtil.blockToSubSection(x), SubChunkSectionUtil.blockToSubSection(y), SubChunkSectionUtil.blockToSubSection(z), 0));
	}

	public EntryImpl getOrCreateEntry(final long pos) {
		EntryImpl entry = entries.get(pos);
		if (entry == null) {
			entry = tryCreate(pos);
			if (entry != null) {
				entries.put(pos, entry);
			}
		}
		return entry;
	}

	private EntryImpl tryCreate(final long packed) {
		final int y = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackY(packed));
		if (world.isOutOfHeightLimit(y)) {
			return null;
		}
		final long chunkKey = ChunkPos.toLong(
				ChunkSectionPos.getSectionCoord(SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackX(packed))),
				ChunkSectionPos.getSectionCoord(SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackZ(packed)))
		);
		final WorldChunk chunk = chunks.get(chunkKey);
		if (chunk == null) {
			return null;
		}
		final ChunkSection section = chunk.getSection(chunk.sectionCoordToIndex(ChunkSectionPos.getSectionCoord(y)));
		if (section == null) {
			return null;
		}
		return new EntryImpl((PathingChunkSection) section, packed);
	}

	public final class EntryImpl implements Entry {
		private final long subSectionPos;
		private final PathingChunkSection section;
		private final Map<ValidLocationSetType<?>, Object> locationSetCache;
		private final Map<ChunkSectionRegionType<?, ?>, Object> regionsCache;
		private final int[] modCounts;
		private final long[] positions;

		public EntryImpl(final PathingChunkSection section, final long pos) {
			this.section = section;
			subSectionPos = pos;
			locationSetCache = new Reference2ReferenceOpenHashMap<>();
			regionsCache = new Reference2ReferenceOpenHashMap<>();
			modCounts = new int[27];
			positions = new long[27];
			final int subSectionX = SubChunkSectionUtil.unpackX(pos);
			final int subSectionY = SubChunkSectionUtil.unpackY(pos);
			final int subSectionZ = SubChunkSectionUtil.unpackZ(pos);
			for (int x = -1; x <= 1; x++) {
				for (int y = -1; y <= 1; y++) {
					for (int z = -1; z <= 1; z++) {
						final int index = modCountIndex(x, y, z);
						positions[index] = SubChunkSectionUtil.pack(subSectionX + x, subSectionY + y, subSectionZ + z, 0);
						modCounts[index] = -1;
					}
				}
			}
		}

		private static int modCountIndex(final int x, final int y, final int z) {
			return ((x + 1) * 3 + y + 1) * 3 + z + 1;
		}

		public boolean verify() {
			final int[] modCounts = this.modCounts;
			final long[] positions = this.positions;
			assert modCounts.length == positions.length;
			boolean modPassing = true;
			for (int i = 0; i < modCounts.length; i++) {
				//TODO optimize
				final EntryImpl entry = entries.get(positions[i]);
				PathingChunkSection section = null;
				if (entry == null) {
					final long chunkKey = ChunkPos.toLong(
							ChunkSectionPos.getSectionCoord(SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackX(subSectionPos))),
							ChunkSectionPos.getSectionCoord(SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackZ(subSectionPos)))
					);
					final WorldChunk chunk = chunks.get(chunkKey);
					if (chunk != null) {
						final int y = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackY(positions[i]));
						if (!world.isOutOfHeightLimit(y)) {
							section = (PathingChunkSection) chunk.getSectionArray()[world.sectionCoordToIndex(ChunkSectionPos.getSectionCoord(y))];
						}
					}
				} else {
					section = entry.section;
				}
				final int index = SubChunkSectionUtil.subSectionIndex(positions[i]);
				int count;
				if ((section == null && modCounts[i] != (count = -1)) || (section != null && modCounts[i] != (count = section.merlin_ai$getModCount(index)))) {
					modCounts[i] = count;
					modPassing = false;
				}
			}
			return modPassing;
		}

		@Override
		public @Nullable <T> ValidLocationSet<T> getValidLocationSet(final ValidLocationSetType<T> type) {
			if (!verify()) {
				locationSetCache.clear();
				regionsCache.clear();
				enqueueLocationSet(type);
				return null;
			}
			final Object set = locationSetCache.get(type);
			if (set == MerlinAI.PLACEHOLDER_OBJECT) {
				return null;
			}
			if (set == null) {
				enqueueLocationSet(type);
			}
			return (ValidLocationSet<T>) set;
		}

		@Override
		public @Nullable <T, N extends AIPathNode<T, N>> ChunkSectionRegions<T, N> getRegions(final ChunkSectionRegionType<T, N> type) {
			if (!verify()) {
				locationSetCache.clear();
				regionsCache.clear();
				enqueueRegions(type);
				return null;
			}
			final Object o = regionsCache.get(type);
			if (o == MerlinAI.PLACEHOLDER_OBJECT) {
				return null;
			}
			if (o == null) {
				enqueueRegions(type);
			}
			return (ChunkSectionRegions<T, N>) o;
		}

		private void enqueueRegions(final ChunkSectionRegionType<?, ?> type) {
			final int x = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackX(subSectionPos));
			final int y = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackY(subSectionPos));
			final int z = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackZ(subSectionPos));
			final BlockPos minCachePos = new BlockPos(x - SubChunkSectionUtil.SUB_SECTION_SIZE, y - SubChunkSectionUtil.SUB_SECTION_SIZE, z - SubChunkSectionUtil.SUB_SECTION_SIZE);
			final BlockPos maxCachePos = new BlockPos(x + SubChunkSectionUtil.SUB_SECTION_SIZE, y + SubChunkSectionUtil.SUB_SECTION_SIZE, z + SubChunkSectionUtil.SUB_SECTION_SIZE);
			final int[] oldModCounts = Arrays.copyOf(modCounts, modCounts.length);
			final BooleanSupplier matcher = () -> Arrays.equals(oldModCounts, modCounts);
			((AIWorld) world).merlin_ai$getTaskExecutor().submitTask(new RegionAnalysisAITask(matcher, type, subSectionPos, () -> ShapeCache.create(world, minCachePos, maxCachePos), regions -> {
				if (matcher.getAsBoolean()) {
					regionsCache.put(type, regions);
				}
			}));
			regionsCache.put(type, MerlinAI.PLACEHOLDER_OBJECT);
		}

		private void enqueueLocationSet(final ValidLocationSetType<?> type) {
			final int x = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackX(subSectionPos));
			final int y = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackY(subSectionPos));
			final int z = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackZ(subSectionPos));
			final BlockPos minCachePos = new BlockPos(x - SubChunkSectionUtil.SUB_SECTION_SIZE, y - SubChunkSectionUtil.SUB_SECTION_SIZE, z - SubChunkSectionUtil.SUB_SECTION_SIZE);
			final BlockPos maxCachePos = new BlockPos(x + SubChunkSectionUtil.SUB_SECTION_SIZE, y + SubChunkSectionUtil.SUB_SECTION_SIZE, z + SubChunkSectionUtil.SUB_SECTION_SIZE);
			final int[] oldModCounts = Arrays.copyOf(modCounts, modCounts.length);
			final BooleanSupplier matcher = () -> Arrays.equals(oldModCounts, modCounts);
			((AIWorld) world).merlin_ai$getTaskExecutor().submitTask(new ValidLocationAnalysisChunkSectionAITTask(matcher, type, subSectionPos, () -> ShapeCache.create(world, minCachePos, maxCachePos), locationSet -> {
				if (matcher.getAsBoolean()) {
					locationSetCache.put(type, locationSet);
				}
			}));
			locationSetCache.put(type, MerlinAI.PLACEHOLDER_OBJECT);
		}
	}
}
