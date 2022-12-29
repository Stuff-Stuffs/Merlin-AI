package io.github.artificial_intellicrafters.merlin_ai.impl.common;

import com.mojang.datafixers.util.Pair;
import io.github.artificial_intellicrafters.merlin_ai.api.AIWorld;
import io.github.artificial_intellicrafters.merlin_ai.api.ChunkRegionGraph;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegionConnectivityGraph;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.HierarchyInfo;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.location_caching.ValidLocationSetImpl;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.task.ChunkSectionRegionLinkingAITask;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.task.ChunkSectionRegionsAnalysisAITask;
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
import java.util.Optional;
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
		private final ChunkSectionPos pos;
		private final PathingChunkSection section;
		private final Map<ValidLocationSetType<?>, Object> locationSetCache;
		private final Map<HierarchyInfo<?, ?, ?, ?>, Object> regionsCache;
		private final Map<HierarchyInfo<?, ?, ?, ?>, Object> graphCache;
		private final long[] modCounts;
		private final long[] positions;
		private final long[] regionsModCounts;
		private long lastTickAccessed = Long.MIN_VALUE;
		private long lastTickAccessedRegions = Long.MIN_VALUE;

		public EntryImpl(final PathingChunkSection section, final ChunkSectionPos pos) {
			this.section = section;
			this.pos = pos;
			locationSetCache = new Reference2ReferenceOpenHashMap<>();
			regionsCache = new Reference2ReferenceOpenHashMap<>();
			graphCache = new Reference2ReferenceOpenHashMap<>();
			modCounts = new long[27];
			positions = new long[27];
			regionsModCounts = new long[27];
			final long packed = pos.asLong();
			for (int x = -1; x <= 1; x++) {
				for (int y = -1; y <= 1; y++) {
					for (int z = -1; z <= 1; z++) {
						final int index = ValidLocationAnalysisChunkSectionAITTask.index(x, y, z);
						positions[index] = ChunkSectionPos.offset(packed, x, y, z);
						modCounts[index] = -1;
					}
				}
			}
			modCounts[ValidLocationAnalysisChunkSectionAITTask.index(0, 0, 0)] = section.merlin_ai$getModCount();
		}

		@Override
		public @Nullable <T> ValidLocationSet<T> getValidLocationSet(final ValidLocationSetType<T> type, final long tick) {
			boolean modPassing = true;
			if (lastTickAccessed != tick) {
				lastTickAccessed = tick;
				final long[] oldModCounts = Arrays.copyOf(modCounts, modCounts.length);
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
					if (modCounts[i] == -1) {
						if (section != null) {
							modCounts[i] = section.merlin_ai$getModCount();
						}
					}
					if ((section == null && modCounts[i] > 0) || (section != null && modCounts[i] != section.merlin_ai$getModCount())) {
						if (section == null) {
							modCounts[i] = 0;
						} else {
							modCounts[i] = section.merlin_ai$getModCount();
						}
						modPassing = false;
					}
				}
				if (!modPassing) {
					final Object set = locationSetCache.get(type);
					locationSetCache.clear();
					enqueueLocationSet(oldModCounts, type, set == MerlinAI.PLACEHOLDER_OBJECT ? null : (ValidLocationSetImpl<T>) set);
					return null;
				}
				final Object set = locationSetCache.get(type);
				if (set == MerlinAI.PLACEHOLDER_OBJECT) {
					return null;
				}
				if (set == null) {
					enqueueLocationSet(modCounts, type, null);
				}
				return (ValidLocationSet<T>) set;
			}
			final Object set = locationSetCache.get(type);
			if (set == MerlinAI.PLACEHOLDER_OBJECT) {
				return null;
			}
			if (set == null) {
				enqueueLocationSet(modCounts, type, null);
			}
			return (ValidLocationSet<T>) set;
		}

		private <T, N, C> @Nullable Pair<ChunkSectionRegions, C> getRegionPrecomputedPair(final HierarchyInfo<T, N, C, ?> info, final long tick) {
			if (!checkRegionValidity(info.validLocationSetType(), tick)) {
				enqueueRegions(info);
				return null;
			}
			final Object set = regionsCache.get(info);
			if (set == MerlinAI.PLACEHOLDER_OBJECT) {
				return null;
			}
			if (set == null) {
				enqueueRegions(info);
			}
			return (Pair<ChunkSectionRegions, C>) set;
		}

		@Override
		public @Nullable ChunkSectionRegions getRegions(final HierarchyInfo<?, ?, ?, ?> info, final long tick) {
			final Pair<ChunkSectionRegions, ?> pair = getRegionPrecomputedPair(info, tick);
			return pair == null ? null : pair.getFirst();
		}

		private void enqueueRegions(final HierarchyInfo<?, ?, ?, ?> info) {
			final BlockPos blockPos = pos.getMinPos();
			final BlockPos minCachePos = blockPos.add(-16, -16, -16);
			final BlockPos maxCachePos = blockPos.add(16, 16, 16);
			final long[] newOldModCounts = Arrays.copyOf(regionsModCounts, regionsModCounts.length);
			final BooleanSupplier matcher = () -> compatible(newOldModCounts, regionsModCounts);
			if (((AIWorld) world).merlin_ai$getTaskExecutor().submitTask(new ChunkSectionRegionsAnalysisAITask<>(info, () -> ShapeCache.create(world, minCachePos, maxCachePos), matcher, pos, world, pair -> regionsCache.put(info, pair), () -> {
				if (regionsCache.get(info) == MerlinAI.PLACEHOLDER_OBJECT) {
					regionsCache.remove(info);
				}
			}))) {
				regionsCache.put(info, MerlinAI.PLACEHOLDER_OBJECT);
			}
		}

		private boolean checkRegionValidity(final ValidLocationSetType<?> type, final long tick) {
			if (lastTickAccessedRegions != tick) {
				lastTickAccessedRegions = tick;
				boolean modPassing = true;
				final long[] modCounts = regionsModCounts;
				final long[] positions = this.positions;
				assert modCounts.length == positions.length;
				for (int i = 0; i < modCounts.length; i++) {
					final EntryImpl entry = entries.get(positions[i]);
					if (entry != null) {
						final ValidLocationSet<?> set = entry.getValidLocationSet(type, tick);
						if (set != null && modCounts[i] != set.revision()) {
							modCounts[i] = set.revision();
							modPassing = false;
						}
					} else {
						if (modCounts[i] != -1) {
							modCounts[0] = -1;
							modPassing = false;
						}
					}
				}
				if (!modPassing) {
					regionsCache.clear();
					graphCache.clear();
					return false;
				}
			}
			return true;
		}

		@Override
		public <N> ChunkSectionRegionConnectivityGraph<N> getGraph(final HierarchyInfo<?, N, ?, ?> info, final long tick) {
			if (!checkRegionValidity(info.validLocationSetType(), tick)) {
				enqueueGraph(info);
			}
			final Object set = graphCache.get(info);
			if (set == MerlinAI.PLACEHOLDER_OBJECT) {
				return null;
			}
			if (set == null) {
				enqueueGraph(info);
			}
			return (ChunkSectionRegionConnectivityGraph<N>) set;
		}

		private <T, N, C> void enqueueGraph(final HierarchyInfo<T, N, C, ?> info) {
			final BlockPos blockPos = pos.getMinPos();
			final BlockPos minCachePos = blockPos.add(-16, -16, -16);
			final BlockPos maxCachePos = blockPos.add(16, 16, 16);
			final long[] newOldModCounts = Arrays.copyOf(regionsModCounts, regionsModCounts.length);
			final BooleanSupplier matcher = () -> compatible(newOldModCounts, regionsModCounts);
			if (((AIWorld) world).merlin_ai$getTaskExecutor().submitTask(new ChunkSectionRegionLinkingAITask<>(info, () -> {
				final var a = getRegionPrecomputedPair(info, world.getTime());
				return a == null ? null : Optional.ofNullable(a.getSecond());
			}, () -> getRegions(info, world.getTime()), () -> ShapeCache.create(world, minCachePos, maxCachePos), matcher, pos, pair -> graphCache.put(info, pair), () -> {
				if (graphCache.get(info) == MerlinAI.PLACEHOLDER_OBJECT) {
					graphCache.remove(info);
				}
			}))) {
				graphCache.put(info, MerlinAI.PLACEHOLDER_OBJECT);
			}
		}

		private <T> void enqueueLocationSet(long[] oldModCounts, final ValidLocationSetType<T> type, @Nullable final ValidLocationSetImpl<T> previous) {
			final BlockPos blockPos = pos.getMinPos();
			final BlockPos minCachePos = blockPos.add(-16, -16, -16);
			final BlockPos maxCachePos = blockPos.add(16, 16, 16);
			final long[] newOldModCounts;
			final BooleanSupplier matcher;
			if (type.columnar()) {
				newOldModCounts = toColumnar(modCounts);
				oldModCounts = toColumnar(oldModCounts);
				matcher = () -> compatible(newOldModCounts, new long[]{modCounts[ValidLocationAnalysisChunkSectionAITTask.index(0, -1, 0)], modCounts[ValidLocationAnalysisChunkSectionAITTask.index(0, 0, 0)], modCounts[ValidLocationAnalysisChunkSectionAITTask.index(0, 1, 0)]});
			} else {
				newOldModCounts = Arrays.copyOf(modCounts, modCounts.length);
				matcher = () -> compatible(newOldModCounts, modCounts);
			}
			if (((AIWorld) world).merlin_ai$getTaskExecutor().submitTask(new ValidLocationAnalysisChunkSectionAITTask<>(matcher, oldModCounts, previous, type, pos, () -> ShapeCache.create(world, minCachePos, maxCachePos), locationSet -> locationSetCache.put(type, locationSet), () -> {
				if (locationSetCache.get(type) == MerlinAI.PLACEHOLDER_OBJECT) {
					locationSetCache.remove(type);
				}
			}))) {
				locationSetCache.put(type, MerlinAI.PLACEHOLDER_OBJECT);
			}
		}

		private static boolean compatible(final long[] current, final long[] old) {
			for (int i = 0; i < current.length; i++) {
				if (current[i] != old[i] && current[i] != -1) {
					return false;
				}
			}
			return true;
		}

		private long[] toColumnar(final long[] data) {
			final long[] columnarData = new long[3];
			columnarData[ValidLocationAnalysisChunkSectionAITTask.indexColumnar(-1)] = data[ValidLocationAnalysisChunkSectionAITTask.index(0, -1, 0)];
			columnarData[ValidLocationAnalysisChunkSectionAITTask.indexColumnar(0)] = data[ValidLocationAnalysisChunkSectionAITTask.index(0, 0, 0)];
			columnarData[ValidLocationAnalysisChunkSectionAITTask.indexColumnar(1)] = data[ValidLocationAnalysisChunkSectionAITTask.index(0, 1, 0)];
			return columnarData;
		}
	}
}
