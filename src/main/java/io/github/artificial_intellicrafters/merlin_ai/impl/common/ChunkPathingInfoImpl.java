package io.github.artificial_intellicrafters.merlin_ai.impl.common;

import com.mojang.datafixers.util.Pair;
import io.github.artificial_intellicrafters.merlin_ai.api.AIWorld;
import io.github.artificial_intellicrafters.merlin_ai.api.ChunkPathingInfo;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegionConnectivityGraph;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.HierarchyInfo;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutionContext;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
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

public class ChunkPathingInfoImpl implements ChunkPathingInfo {
	private final World world;
	private final Long2ReferenceMap<WorldChunk> chunks;
	private final Long2ReferenceMap<EntryImpl> entries;

	public ChunkPathingInfoImpl(final World world) {
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
		return getEntry(ChunkSectionPos.asLong(x >> 4, y >> 4, z >> 4));
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
			for (int x = -1; x <= 1; x++) {
				for (int y = -1; y <= 1; y++) {
					for (int z = -1; z <= 1; z++) {
						final int index = ValidLocationAnalysisChunkSectionAITTask.index(x, y, z);
						positions[index] = pos.add(x, y, z).asLong();
						modCounts[index] = -1;
					}
				}
			}
			modCounts[ValidLocationAnalysisChunkSectionAITTask.index(0, 0, 0)] = section.merlin_ai$getModCount();
		}

		@Override
		public @Nullable <T> ValidLocationSet<T> getValidLocationSet(final ValidLocationSetType<T> type, final long tick, @Nullable final AITaskExecutionContext executionContext, final boolean enqueue) {
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
					if ((section == null && modCounts[i] >= 0) || (section != null && modCounts[i] != section.merlin_ai$getModCount())) {
						if (section == null) {
							modCounts[i] = -1;
						} else {
							modCounts[i] = section.merlin_ai$getModCount();
						}
						modPassing = false;
					}
				}
				if (!modPassing) {
					if (!enqueue) {
						return null;
					}
					final Object set = locationSetCache.get(type);
					locationSetCache.clear();
					regionsCache.clear();
					graphCache.clear();
					enqueueLocationSet(oldModCounts, type, set == MerlinAI.PLACEHOLDER_OBJECT ? null : (ValidLocationSet<T>) set, executionContext);
					return null;
				}
				final Object set = locationSetCache.get(type);
				if (set == MerlinAI.PLACEHOLDER_OBJECT) {
					return null;
				}
				if (set == null && enqueue) {
					enqueueLocationSet(modCounts, type, null, executionContext);
				}
				return (ValidLocationSet<T>) set;
			}
			final Object set = locationSetCache.get(type);
			if (set == MerlinAI.PLACEHOLDER_OBJECT) {
				return null;
			}
			if (set == null && enqueue) {
				enqueueLocationSet(modCounts, type, null, executionContext);
			}
			return (ValidLocationSet<T>) set;
		}

		private <T, N, C> @Nullable Pair<ChunkSectionRegions, C> getRegionPrecomputedPair(final HierarchyInfo<T, N, C, ?> info, final long tick, @Nullable final AITaskExecutionContext executionContext, final boolean enqueue) {
			if (!checkRegionValidity(info.validLocationSetType(), tick, executionContext)) {
				if (enqueue) {
					enqueueRegions(info, executionContext);
				}
				return null;
			}
			final Object set = regionsCache.get(info);
			if (set == MerlinAI.PLACEHOLDER_OBJECT) {
				return null;
			}
			if (set == null) {
				enqueueRegions(info, executionContext);
			}
			return (Pair<ChunkSectionRegions, C>) set;
		}

		@Override
		public @Nullable ChunkSectionRegions getRegions(final HierarchyInfo<?, ?, ?, ?> info, final long tick, @Nullable final AITaskExecutionContext executionContext, final boolean enqueue) {
			final Pair<ChunkSectionRegions, ?> pair = getRegionPrecomputedPair(info, tick, executionContext, enqueue);
			return pair == null ? null : pair.getFirst();
		}

		private void enqueueRegions(final HierarchyInfo<?, ?, ?, ?> info, @Nullable final AITaskExecutionContext executionContext) {
			final BlockPos blockPos = pos.getMinPos();
			final BlockPos minCachePos = blockPos.add(-16, -16, -16);
			final BlockPos maxCachePos = blockPos.add(16, 16, 16);
			final long[] newOldModCounts = Arrays.copyOf(regionsModCounts, regionsModCounts.length);
			final BooleanSupplier matcher = () -> compatible(newOldModCounts, regionsModCounts);
			final ChunkSectionRegionsAnalysisAITask<?, ?, ?> task = new ChunkSectionRegionsAnalysisAITask<>(info, () -> ShapeCache.create(world, minCachePos, maxCachePos), matcher, pos, world, pair -> regionsCache.put(info, pair), () -> {
				if (regionsCache.get(info) == MerlinAI.PLACEHOLDER_OBJECT) {
					regionsCache.remove(info);
				}
			});
			final Optional<AITaskExecutionContext> context;
			if (executionContext == null) {
				context = ((AIWorld) world).merlin_ai$getTaskExecutor().submitTask(task);
			} else {
				context = ((AIWorld) world).merlin_ai$getTaskExecutor().submitTaskBefore(task, executionContext);
			}
			if (context.isPresent()) {
				task.setContext(context.get());
				regionsCache.put(info, MerlinAI.PLACEHOLDER_OBJECT);
			}
		}

		private boolean checkRegionValidity(final ValidLocationSetType<?> type, final long tick, @Nullable final AITaskExecutionContext executionContext) {
			if (lastTickAccessedRegions != tick) {
				lastTickAccessedRegions = tick;
				boolean modPassing = true;
				final long[] modCounts = regionsModCounts;
				final long[] positions = this.positions;
				assert modCounts.length == positions.length;
				for (int i = 0; i < modCounts.length; i++) {
					final EntryImpl entry = entries.get(positions[i]);
					if (entry != null) {
						final ValidLocationSet<?> set = entry.getValidLocationSet(type, tick, executionContext);
						if (set != null && modCounts[i] != set.revision()) {
							modCounts[i] = set.revision();
							modPassing = false;
						}
					} else {
						if (modCounts[i] != -1) {
							modCounts[i] = -1;
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
		public <N> ChunkSectionRegionConnectivityGraph<N> getGraph(final HierarchyInfo<?, N, ?, ?> info, final long tick, @Nullable final AITaskExecutionContext executionContext, final boolean enqueue) {
			if (enqueue && !checkRegionValidity(info.validLocationSetType(), tick, executionContext)) {
				enqueueGraph(info, executionContext);
			}
			final Object set = graphCache.get(info);
			if (set == MerlinAI.PLACEHOLDER_OBJECT) {
				return null;
			}
			if (set == null) {
				enqueueGraph(info, executionContext);
			}
			return (ChunkSectionRegionConnectivityGraph<N>) set;
		}

		private <T, N, C> void enqueueGraph(final HierarchyInfo<T, N, C, ?> info, @Nullable final AITaskExecutionContext executionContext) {
			final BlockPos blockPos = pos.getMinPos();
			final BlockPos minCachePos = blockPos.add(-16, -16, -16);
			final BlockPos maxCachePos = blockPos.add(16, 16, 16);
			final long[] newOldModCounts = Arrays.copyOf(regionsModCounts, regionsModCounts.length);
			final BooleanSupplier matcher = () -> compatible(newOldModCounts, regionsModCounts);
			final ChunkSectionRegionLinkingAITask<N, C, ?> task = new ChunkSectionRegionLinkingAITask<>(info, () -> {
				final var a = getRegionPrecomputedPair(info, world.getTime(), executionContext, true);
				return a == null ? null : Optional.ofNullable(a.getSecond());
			}, () -> getRegions(info, world.getTime(), executionContext), () -> ShapeCache.create(world, minCachePos, maxCachePos), matcher, pos, pair -> graphCache.put(info, pair), () -> {
				if (graphCache.get(info) == MerlinAI.PLACEHOLDER_OBJECT) {
					graphCache.remove(info);
				}
			});
			final Optional<AITaskExecutionContext> context;
			if (executionContext == null) {
				context = ((AIWorld) world).merlin_ai$getTaskExecutor().submitTask(task);
			} else {
				context = ((AIWorld) world).merlin_ai$getTaskExecutor().submitTaskBefore(task, executionContext);
			}
			if (context.isPresent()) {
				task.setContext(context.get());
				graphCache.put(info, MerlinAI.PLACEHOLDER_OBJECT);
			}
		}

		private <T> void enqueueLocationSet(final long[] oldModCounts, final ValidLocationSetType<T> type, @Nullable final ValidLocationSet<T> previous, @Nullable final AITaskExecutionContext executionContext) {
			final BlockPos blockPos = pos.getMinPos();
			final BlockPos minCachePos = blockPos.add(-16, -16, -16);
			final BlockPos maxCachePos = blockPos.add(16, 16, 16);
			final long[] newOldModCounts;
			final BooleanSupplier matcher;
			newOldModCounts = Arrays.copyOf(modCounts, modCounts.length);
			matcher = () -> compatible(newOldModCounts, modCounts);
			final ValidLocationAnalysisChunkSectionAITTask<T> task = new ValidLocationAnalysisChunkSectionAITTask<>(matcher, oldModCounts, previous, type, pos, () -> ShapeCache.create(world, minCachePos, maxCachePos), locationSet -> locationSetCache.put(type, locationSet), () -> {
				if (locationSetCache.get(type) == MerlinAI.PLACEHOLDER_OBJECT) {
					locationSetCache.remove(type);
				}
			});
			final Optional<AITaskExecutionContext> context;
			if (executionContext == null) {
				context = ((AIWorld) world).merlin_ai$getTaskExecutor().submitTask(task);
			} else {
				context = ((AIWorld) world).merlin_ai$getTaskExecutor().submitTaskBefore(task, executionContext);
			}
			if (context.isPresent()) {
				task.setContext(context.get());
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
	}
}
