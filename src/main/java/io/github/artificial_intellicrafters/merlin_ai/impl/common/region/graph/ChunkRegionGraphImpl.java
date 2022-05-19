package io.github.artificial_intellicrafters.merlin_ai.impl.common.region.graph;

import io.github.artificial_intellicrafters.merlin_ai.api.AIWorld;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.region.graph.ChunkRegionGraph;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.MerlinAI;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.PathingChunkSection;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.task.ChunkRegionLinkAITask;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.task.ChunkRegionsAnalysisAITask;
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

	public <T, N extends AIPathNode<T, N>> void getAdjacentRegions(final ChunkSectionRegionType<T, N> region, final int id, final T context) {

	}

	public void tick() {
		entries.values().forEach(EntryImpl::verify);
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
				entry.verify();
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
		private final Map<ChunkSectionRegionType<?, ?>, Object> regionsCache;
		private final Map<ChunkSectionRegionType<?, ?>, Object> regionAdjacentCache;
		private final long[] modCounts;
		private final long[] positions;
		private int nextRegionId = Integer.MIN_VALUE;
		private boolean cleared = true;

		public EntryImpl(final PathingChunkSection section, final ChunkSectionPos pos) {
			id = section.merlin_ai$getSectionId();
			this.section = section;
			this.pos = pos;
			locationSetCache = new Reference2ReferenceOpenHashMap<>();
			regionsCache = new Reference2ReferenceOpenHashMap<>();
			regionAdjacentCache = new Reference2ReferenceOpenHashMap<>();
			modCounts = new long[27];
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

		public int getNextRegionId() {
			return nextRegionId;
		}

		public void setNextRegionId(final int nextRegionId) {
			this.nextRegionId = nextRegionId;
		}

		private static int modCountIndex(final int x, final int y, final int z) {
			return ((x + 1) * 3 + y + 1) * 3 + z + 1;
		}

		@Override
		public long getId() {
			return id;
		}

		@Override
		public <T, N extends AIPathNode<T, N>> int getAdjacentRegions(final ChunkSectionRegionType<T, N> type, final long id, final long[] neighbours) {
			final Object o = regionAdjacentCache.get(type);
			if (o == MerlinAI.PLACEHOLDER_OBJECT) {
				return 0;
			}
			if (o == null) {
				enqueueRegionLink(type, id);
				return 0;
			}
			final long[] entries = ((ChunkRegionNeighbourGraph) o).getEntries(id);
			if (entries == null) {
				return 0;
			}
			final int min = Math.min(entries.length, neighbours.length);
			System.arraycopy(entries, 0, neighbours, 0, min);
			return min;
		}

		private <T, N extends AIPathNode<T, N>> void enqueueRegionLink(final ChunkSectionRegionType<T, N> type, final long id) {
			final BlockPos blockPos = pos.getMinPos();
			final BlockPos minCachePos = blockPos.add(-15, -15, -15);
			final BlockPos maxCachePos = blockPos.add(16, 16, 16);
			final long[] oldModCounts = Arrays.copyOf(modCounts, modCounts.length);
			final BooleanSupplier matcher = () -> Arrays.equals(oldModCounts, modCounts);
			((AIWorld) world).merlin_ai$getTaskExecutor().submitTask(new ChunkRegionLinkAITask(matcher, type, pos, () -> ShapeCache.create(world, minCachePos, maxCachePos), map -> {
				if (matcher.getAsBoolean()) {
					regionAdjacentCache.put(type, new ChunkRegionNeighbourGraph(map));
				}
			}));
			regionAdjacentCache.put(type, MerlinAI.PLACEHOLDER_OBJECT);
			cleared = false;
		}

		public void verify() {
			if (!cleared) {
				boolean modPassing = true;
				final long[] modCounts = this.modCounts;
				final long[] positions = this.positions;
				assert modCounts.length == positions.length;
				for (int i = 0; i < modCounts.length; i++) {
					final EntryImpl entry = entries.get(positions[i]);
					PathingChunkSection section = entry == null ? null : entry.section;
					if (section == null) {
						final WorldChunk chunk = chunks.get(ChunkPos.toLong(ChunkSectionPos.unpackX(positions[i]), ChunkSectionPos.unpackZ(positions[i])));
						if (chunk != null) {
							final int index = chunk.sectionCoordToIndex(ChunkSectionPos.unpackY(positions[i]));
							if (index >= 0 && index < chunk.getSectionArray().length) {
								section = (PathingChunkSection) chunk.getSection(index);
							}
						}
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
					regionsCache.clear();
					regionAdjacentCache.clear();
					cleared = true;
				}
			}
		}

		@Override
		public @Nullable <T> ValidLocationSet<T> getValidLocationSet(final ValidLocationSetType<T> type) {
			final Object set = locationSetCache.get(type);
			if (set == MerlinAI.PLACEHOLDER_OBJECT) {
				return null;
			}
			if (set == null) {
				enqueueLocationSet(type);
			}
			return (ValidLocationSet<T>) set;
		}

		private void enqueueLocationSet(final ValidLocationSetType<?> type) {
			final BlockPos blockPos = pos.getMinPos();
			final BlockPos minCachePos = blockPos.add(-15, -15, -15);
			final BlockPos maxCachePos = blockPos.add(16, 16, 16);
			final long[] oldModCounts = Arrays.copyOf(modCounts, modCounts.length);
			final BooleanSupplier matcher = () -> Arrays.equals(oldModCounts, modCounts);
			((AIWorld) world).merlin_ai$getTaskExecutor().submitTask(new ValidLocationAnalysisChunkSectionAITTask(matcher, type, pos, () -> ShapeCache.create(world, minCachePos, maxCachePos), locationSet -> {
				if (matcher.getAsBoolean()) {
					locationSetCache.put(type, locationSet);
				}
			}));
			locationSetCache.put(type, MerlinAI.PLACEHOLDER_OBJECT);
			cleared = false;
		}

		@Override
		public @Nullable <T, N extends AIPathNode<T, N>> ChunkSectionRegions<T, N> getChunkSectionRegions(final ChunkSectionRegionType<T, N> type) {
			final Object set = regionsCache.get(type);
			if (set == MerlinAI.PLACEHOLDER_OBJECT) {
				return null;
			}
			if (set == null) {
				enqueueRegions(type);
			}
			return (ChunkSectionRegions<T, N>) set;
		}

		private void enqueueRegions(final ChunkSectionRegionType<?, ?> type) {
			final BlockPos blockPos = pos.getMinPos();
			final BlockPos minCachePos = blockPos.add(-15, -15, -15);
			final BlockPos maxCachePos = blockPos.add(16, 16, 16);
			final long[] oldModCounts = Arrays.copyOf(modCounts, modCounts.length);
			final BooleanSupplier matcher = () -> Arrays.equals(oldModCounts, modCounts);
			((AIWorld) world).merlin_ai$getTaskExecutor().submitTask(new ChunkRegionsAnalysisAITask(matcher, type, pos, this, () -> ShapeCache.create(world, minCachePos, maxCachePos), regions -> {
				if (matcher.getAsBoolean()) {
					regionsCache.put(type, regions);
				}
			}));
			regionsCache.put(type, MerlinAI.PLACEHOLDER_OBJECT);
			cleared = false;
		}
	}
}
