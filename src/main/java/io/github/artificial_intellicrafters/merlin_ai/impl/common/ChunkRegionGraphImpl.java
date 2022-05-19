package io.github.artificial_intellicrafters.merlin_ai.impl.common;

import io.github.artificial_intellicrafters.merlin_ai.api.AIWorld;
import io.github.artificial_intellicrafters.merlin_ai.api.ChunkRegionGraph;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
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
		chunks.remove(pos.toLong());
		final int topSectionCoord = chunk.getTopSectionCoord();
		for (int i = chunk.getBottomSectionCoord(); i < topSectionCoord; i++) {
			final long key = ChunkSectionPos.asLong(pos.x, i, pos.z);
			entries.remove(key);
		}
	}

	@Override
	public @Nullable EntryImpl getEntry(final ChunkSectionPos pos) {
		return getEntry(pos.asLong());
	}

	@Override
	public @Nullable EntryImpl getEntry(final int x, final int y, final int z) {
		return getEntry(ChunkSectionPos.from(new BlockPos(x, y, z)).asLong());
	}

	public EntryImpl getEntry(final long pos) {
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
		final ChunkSectionPos pos = ChunkSectionPos.from(packed);
		if (world.isOutOfHeightLimit(pos.getMaxY())) {
			return null;
		}
		final long chunkKey = ChunkPos.toLong(pos.getSectionX(), pos.getSectionZ());
		final WorldChunk chunk = chunks.get(chunkKey);
		if (chunk == null) {
			return null;
		}
		final ChunkSection section = chunk.getSection(chunk.sectionCoordToIndex(pos.getSectionY()));
		if (section == null) {
			return null;
		}
		return new EntryImpl((PathingChunkSection) section, pos);
	}

	public final class EntryImpl implements Entry {
		private final long id;
		private final ChunkSectionPos pos;
		private final PathingChunkSection section;
		private final Map<ValidLocationSetType<?>, Object> locationSetCache;
		private final long[] modCounts;
		private final long[] positions;

		public EntryImpl(final PathingChunkSection section, final ChunkSectionPos pos) {
			id = section.merlin_ai$getSectionId();
			this.section = section;
			this.pos = pos;
			locationSetCache = new Reference2ReferenceOpenHashMap<>();
			modCounts = new long[27];
			modCounts[modCountIndex(0, 0, 0)] = section.merlin_ai$getModCount();
			positions = new long[27];
			final long packed = pos.asLong();
			for (int x = -1; x <= 1; x++) {
				for (int y = -1; y <= 1; y++) {
					for (int z = -1; z <= 1; z++) {
						final int index = modCountIndex(x, y, z);
						positions[index] = ChunkSectionPos.offset(packed, x, y, z);
						modCounts[index] = -1;
					}
				}
			}
		}

		private static int modCountIndex(final int x, final int y, final int z) {
			return ((x + 1) * 3 + y + 1) * 3 + z + 1;
		}

		@Override
		public long getId() {
			return id;
		}

		@Override
		public synchronized @Nullable <T> ValidLocationSet<T> getValidLocationSet(final ValidLocationSetType<T> type) {
			boolean modPassing = true;
			final long[] modCounts = this.modCounts;
			final long[] positions = this.positions;
			assert modCounts.length == positions.length;
			for (int i = 0; i < modCounts.length; i++) {
				final EntryImpl entry = entries.get(positions[i]);
				PathingChunkSection section = null;
				if (entry == null) {
					final WorldChunk chunk = chunks.get(positions[i]);
					if (chunk != null) {
						final int y = ChunkSectionPos.unpackY(positions[i]);
						if (!world.isOutOfHeightLimit(y << 4)) {
							section = (PathingChunkSection) chunk.getSectionArray()[world.sectionCoordToIndex(y)];
						}
					}
				} else {
					section = entry.section;
				}
				if ((section == null && modCounts[i] != -1) || (section != null && modCounts[i] != section.merlin_ai$getModCount())) {
					if (section == null) {
						modCounts[i] = -1;
					} else {
						modCounts[i] = section.merlin_ai$getModCount();
					}
					modPassing = false;
				}
			}
			if (!modPassing) {
				locationSetCache.clear();
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

		private synchronized void enqueueLocationSet(final ValidLocationSetType<?> type) {
			final BlockPos blockPos = pos.getMinPos();
			final BlockPos minCachePos = blockPos.add(-15, -15, -15);
			final BlockPos maxCachePos = blockPos.add(16, 16, 16);
			final long[] oldModCounts = Arrays.copyOf(modCounts, modCounts.length);
			final BooleanSupplier matcher = () -> Arrays.equals(oldModCounts, modCounts);
			((AIWorld) world).merlin_ai$getTaskExecutor().submitTask(new ValidLocationAnalysisChunkSectionAITTask(matcher, section::merlin_ai$getModCount, type, pos, () -> ShapeCache.create(world, minCachePos, maxCachePos), locationSet -> {
				if (matcher.getAsBoolean()) {
					locationSetCache.put(type, locationSet);
				}
			}));
			locationSetCache.put(type, MerlinAI.PLACEHOLDER_OBJECT);
		}
	}
}
