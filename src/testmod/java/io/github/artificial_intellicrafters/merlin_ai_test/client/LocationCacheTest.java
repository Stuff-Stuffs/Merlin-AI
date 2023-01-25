package io.github.artificial_intellicrafters.merlin_ai_test.client;

import com.mojang.datafixers.util.Pair;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegion;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegionConnectivityGraph;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.HierarchyInfo;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationClassifier;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetTypeRegistry;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutionContext;
import io.github.artificial_intellicrafters.merlin_ai.api.util.CollisionUtil;
import io.github.artificial_intellicrafters.merlin_ai.api.util.OrablePredicate;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.PathingChunkSection;
import io.github.artificial_intellicrafters.merlin_ai_test.common.BasicAIPathNode;
import io.github.artificial_intellicrafters.merlin_ai_test.common.MerlinAITest;
import io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test.BasicLocationType;
import io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test.NeighbourGetter;
import io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test.TestNodeProducer;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayFIFOQueue;
import it.unimi.dsi.fastutil.shorts.ShortPriorityQueue;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.Nullable;

public final class LocationCacheTest {
	public static final ValidLocationSetType<BasicLocationType> ONE_X_TWO_BASIC_LOCATION_SET_TYPE;
	public static final NeighbourGetter<Entity, BasicAIPathNode> BASIC_NEIGHBOUR_GETTER;
	public static final HierarchyInfo<BasicLocationType, Void, CacheData, Tmp> HIERARCHY_INFO;

	public static void init() {

	}

	private LocationCacheTest() {
	}

	private static final class Tmp implements OrablePredicate<Void, Tmp> {

		@Override
		public boolean test(final Void value) {
			return false;
		}

		@Override
		public Tmp or(final Tmp other) {
			return null;
		}
	}

	private static final class CacheData {
		private final Short2ObjectMap<LongSet> data = new Short2ObjectOpenHashMap<>();
	}

	static {
		ValidLocationSetTypeRegistry.INSTANCE.register(BasicLocationType.UNIVERSE_INFO, new ValidLocationClassifier<>() {
			private static final Box BOX = new Box(0, -1, 0, 1, 2, 1);

			@Override
			public BasicLocationType classify(final int x, final int y, final int z, final ShapeCache cache, final AITaskExecutionContext executionContext) {
				final CollisionUtil.FloorCollision collision = CollisionUtil.open(BOX.offset(x, y, z), 1 / 3.0 - 0.00000000001, cache);
				return switch (collision) {
					case OPEN -> BasicLocationType.OPEN;
					case FLOOR -> BasicLocationType.GROUND;
					case CLOSED -> BasicLocationType.CLOSED;
				};
			}

			@Override
			public void rebuild(final BlockState[] updateBlockStates, final short[] updatedPositions, final int updateCount, final int chunkSectionX, final int chunkSectionY, final int chunkSectionZ, final int offsetX, final int offsetY, final int offsetZ, final RebuildConsumer<BasicLocationType> consumer, final ShapeCache cache, final AITaskExecutionContext executionContext) {
				if (offsetX != 0 || offsetZ != 0) {
					return;
				}
				for (int i = 0; i < updateCount; i++) {
					final short updatePosition = updatedPositions[i];
					final int x = (chunkSectionX + offsetX) * ChunkSection.SECTION_WIDTH + PathingChunkSection.unpackLocalX(updatePosition);
					final int unpackedYOffset = PathingChunkSection.unpackLocalY(updatePosition);
					final int y = (chunkSectionY + offsetY) * ChunkSection.SECTION_WIDTH + unpackedYOffset;
					final int z = (chunkSectionZ + offsetZ) * ChunkSection.SECTION_WIDTH + PathingChunkSection.unpackLocalZ(updatePosition);
					if (offsetY == 0) {
						consumer.update(classify(x, y, z, cache, executionContext), x, y, z);
						if (unpackedYOffset != 15) {
							consumer.update(classify(x, y + 1, z, cache, executionContext), x, y + 1, z);
						}
						if (unpackedYOffset != 0) {
							consumer.update(classify(x, y - 1, z, cache, executionContext), x, y - 1, z);
						}
					} else {
						if (offsetY == -1 && unpackedYOffset == 15) {
							consumer.update(classify(x, y + 1, z, cache, executionContext), x, y + 1, z);
						} else if (offsetY == 1 && unpackedYOffset == 0) {
							consumer.update(classify(x, y - 1, z, cache, executionContext), x, y - 1, z);
						}
					}
				}
			}
		}, BasicLocationType.class, true, new Identifier(MerlinAITest.MOD_ID, "basic_1x2"));
		ONE_X_TWO_BASIC_LOCATION_SET_TYPE = ValidLocationSetTypeRegistry.INSTANCE.get(BasicLocationType.class, new Identifier(MerlinAITest.MOD_ID, "basic_1x2"));
		BASIC_NEIGHBOUR_GETTER = new TestNodeProducer(ONE_X_TWO_BASIC_LOCATION_SET_TYPE);
		HIERARCHY_INFO = new HierarchyInfo<>() {
			@Override
			public ValidLocationSetType<BasicLocationType> validLocationSetType() {
				return ONE_X_TWO_BASIC_LOCATION_SET_TYPE;
			}

			@Override
			public Class<Void> pathContextClass() {
				return Void.class;
			}

			@Override
			public Pair<ChunkSectionRegions, CacheData> regionify(final ShapeCache shapeCache, final ChunkSectionPos pos, final ValidLocationSetType<BasicLocationType> type, final HeightLimitView limitView, final AITaskExecutionContext executionContext) {
				int missing = 0;
				for (int i = -1; i <= 1; i++) {
					for (int j = -1; j <= 1; j++) {
						if (!shapeCache.isOutOfHeightLimit(pos.getMinY() + j * 16)) {
							for (int k = -1; k <= 1; k++) {
								if (!shapeCache.doesLocationSetExist(pos.getMinX() + i * 16, pos.getMinY() + j * 16, pos.getMinZ() + k * 16, type, executionContext)) {
									shapeCache.getLocationType(pos.getMinX() + i * 16, pos.getMinY() + j * 16, pos.getMinZ() + k * 16, type, executionContext);
									missing++;
								}
							}
						}
					}
				}
				if (missing != 0) {
					return null;
				}
				final ChunkSectionRegions.Builder builder = ChunkSectionRegions.builder(pos, limitView);
				final CacheData data = new CacheData();
				for (int x = 0; x < 16; x++) {
					for (int y = 0; y < 16; y++) {
						for (int z = 0; z < 16; z++) {
							if (!builder.contains(PathingChunkSection.packLocal(x, y, z))) {
								floodFill(x, y, z, shapeCache, builder, type, pos, data, executionContext);
							}
						}
					}
				}
				return Pair.of(builder.build(), data);
			}

			private void checkBlockDouble(final int x, final int y, final int z, final int ox, final int oy, final int oz, final ShapeCache cache, final ChunkSectionRegions.RegionKey key, final ChunkSectionRegions.Builder builder, final ValidLocationSetType<BasicLocationType> type, final ShortPriorityQueue queue, final LongSet outgoing, final boolean expand, @Nullable final AITaskExecutionContext executionContext) {
				final boolean outOfBounds = cache.isOutOfHeightLimit(oy);
				if (outOfBounds) {
					return;
				}
				boolean added = false;
				final short i = PathingChunkSection.packLocal(x, y, z);
				if (expand && !(added = builder.contains(i)) && cache.getLocationType(x, y, z, type, executionContext) == BasicLocationType.GROUND && cache.getLocationType(ox, oy, oz, type, executionContext) == BasicLocationType.OPEN) {
					queue.enqueue(i);
					builder.expand(key, i);
					added = true;
				}
				if (!added) {
					outgoing.add(BlockPos.asLong(x, y, z));
				}
			}

			private void checkBlock(final int x, final int y, final int z, final ShapeCache cache, final ChunkSectionRegions.RegionKey key, final ChunkSectionRegions.Builder builder, final ValidLocationSetType<BasicLocationType> type, final ShortPriorityQueue queue, final LongSet outgoing, final boolean expand, @Nullable final AITaskExecutionContext executionContext) {
				boolean added = false;
				final short i = PathingChunkSection.packLocal(x, y, z);
				BasicLocationType locationType = null;
				if (expand && !(added = builder.contains(i)) && (locationType = cache.getLocationType(x, y, z, type, executionContext)) == BasicLocationType.GROUND) {
					queue.enqueue(i);
					builder.expand(key, i);
					added = true;
				}
				if (!added && (locationType == BasicLocationType.OPEN || cache.getLocationType(x, y, z, type, executionContext) == BasicLocationType.GROUND)) {
					outgoing.add(BlockPos.asLong(x, y, z));
				}
			}

			private void check(final int x, final int y, final int z, final ShapeCache cache, final ChunkSectionRegions.RegionKey key, final ChunkSectionRegions.Builder builder, final ValidLocationSetType<BasicLocationType> type, final ShortPriorityQueue queue, final LongSet outgoing, @Nullable final AITaskExecutionContext executionContext) {
				checkBlock(x, y, z + 1, cache, key, builder, type, queue, outgoing, (z & 15) != 15, executionContext);
				checkBlock(x, y, z - 1, cache, key, builder, type, queue, outgoing, (z & 15) != 0, executionContext);
				checkBlock(x + 1, y, z, cache, key, builder, type, queue, outgoing, (x & 15) != 15, executionContext);
				checkBlock(x - 1, y, z, cache, key, builder, type, queue, outgoing, (x & 15) != 0, executionContext);
				if (!cache.isOutOfHeightLimit(y - 1)) {
					checkBlockDouble(x, y - 1, z + 1, x, y, z + 1, cache, key, builder, type, queue, outgoing, (y & 15) != 0 && (z & 15) != 15, executionContext);
					checkBlockDouble(x, y - 1, z - 1, x, y, z - 1, cache, key, builder, type, queue, outgoing, (y & 15) != 0 && (z & 15) != 0, executionContext);
					checkBlockDouble(x + 1, y - 1, z, x + 1, y, z, cache, key, builder, type, queue, outgoing, (y & 15) != 0 && (x & 15) != 15, executionContext);
					checkBlockDouble(x - 1, y - 1, z, x - 1, y, z, cache, key, builder, type, queue, outgoing, (y & 15) != 0 && (x & 15) != 0, executionContext);
				}
				if (!cache.isOutOfHeightLimit(y + 1) && cache.getLocationType(x, y + 1, z, type, executionContext) == BasicLocationType.OPEN) {
					checkBlock(x, y + 1, z + 1, cache, key, builder, type, queue, outgoing, (y & 15) != 15 && (z & 15) != 15, executionContext);
					checkBlock(x, y + 1, z - 1, cache, key, builder, type, queue, outgoing, (y & 15) != 15 && (z & 15) != 0, executionContext);
					checkBlock(x + 1, y + 1, z, cache, key, builder, type, queue, outgoing, (y & 15) != 15 && (x & 15) != 15, executionContext);
					checkBlock(x - 1, y + 1, z, cache, key, builder, type, queue, outgoing, (y & 15) != 15 && (x & 15) != 0, executionContext);
				}
			}

			private void floodFill(final int lx, final int ly, final int lz, final ShapeCache cache, final ChunkSectionRegions.Builder builder, final ValidLocationSetType<BasicLocationType> type, final ChunkSectionPos pos, final CacheData data, @Nullable final AITaskExecutionContext executionContext) {
				final int sx = pos.getMinX();
				final int sy = pos.getMinY();
				final int sz = pos.getMinZ();
				if (cache.getLocationType(lx + sx, ly + sy, lz + sz, type, executionContext) != BasicLocationType.GROUND) {
					return;
				}
				final ShortPriorityQueue queue = new ShortArrayFIFOQueue(16 * 16);
				final short local = PathingChunkSection.packLocal(lx, ly, lz);
				final LongSet outgoing = new LongOpenHashSet();
				data.data.put(local, outgoing);
				queue.enqueue(local);
				final ChunkSectionRegions.RegionKey key = builder.newRegion();
				builder.expand(key, local);
				while (!queue.isEmpty()) {
					final short s = queue.dequeueShort();
					final int x = PathingChunkSection.unpackLocalX(s);
					final int y = PathingChunkSection.unpackLocalY(s);
					final int z = PathingChunkSection.unpackLocalZ(s);
					check(x + sx, y + sy, z + sz, cache, key, builder, type, queue, outgoing, executionContext);
				}
				if (outgoing.isEmpty()) {
					data.data.remove(local);
				}
			}

			@Override
			public ChunkSectionRegionConnectivityGraph<Void> link(final CacheData precomputed, final ShapeCache shapeCache, final ChunkSectionPos pos, final ChunkSectionRegions regions, final AITaskExecutionContext executionContext) {
				final ChunkSectionRegionConnectivityGraph.Builder<Void, Tmp> builder = ChunkSectionRegionConnectivityGraph.builder(HIERARCHY_INFO, regions);
				for (final Short2ObjectMap.Entry<LongSet> entry : precomputed.data.short2ObjectEntrySet()) {
					final ChunkSectionRegion region = regions.query(entry.getShortKey());
					if (region != null) {
						final ChunkSectionRegionConnectivityGraph.RegionBuilder<Void, Tmp> regionBuilder = builder.region(region.id());
						final LongIterator iterator = entry.getValue().iterator();
						while (iterator.hasNext()) {
							final long packed = iterator.nextLong();
							final int x = BlockPos.unpackLongX(packed);
							int y = BlockPos.unpackLongY(packed);
							final int z = BlockPos.unpackLongZ(packed);
							ChunkSectionRegions chunk = shapeCache.getRegions(x, y, z, HIERARCHY_INFO, executionContext);
							boolean accepted = false;
							if (chunk != null) {
								final ChunkSectionRegion query = chunk.query(PathingChunkSection.packLocal(x, y, z));
								if (query != null && query.id() != region.id()) {
									regionBuilder.addLink(query.id());
									accepted = true;
								}
							}
							if (accepted) {
								continue;
							}
							while (!shapeCache.isOutOfHeightLimit(y) && shapeCache.getLocationType(x, y, z, ONE_X_TWO_BASIC_LOCATION_SET_TYPE, executionContext) == BasicLocationType.OPEN) {
								y--;
							}
							if (!shapeCache.isOutOfHeightLimit(y) && shapeCache.getLocationType(x, y, z, ONE_X_TWO_BASIC_LOCATION_SET_TYPE, executionContext) == BasicLocationType.GROUND) {
								chunk = shapeCache.getRegions(x, y, z, HIERARCHY_INFO, executionContext);
								if (chunk != null) {
									final ChunkSectionRegion query = chunk.query(PathingChunkSection.packLocal(x, y, z));
									if (query != null && query.id() != region.id()) {
										regionBuilder.addLink(query.id());
									}
								}
							}
						}
					}
				}
				return builder.build();
			}
		};
	}
}
