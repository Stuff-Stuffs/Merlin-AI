package io.github.artificial_intellicrafters.merlin_ai_test.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import io.github.artificial_intellicrafters.merlin_ai.api.AIWorld;
import io.github.artificial_intellicrafters.merlin_ai.api.ChunkRegionGraph;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegion;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegionConnectivityGraph;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.HierarchyInfo;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationClassifier;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetTypeRegistry;
import io.github.artificial_intellicrafters.merlin_ai.api.path.NeighbourGetter;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutionContext;
import io.github.artificial_intellicrafters.merlin_ai.api.util.CollisionUtil;
import io.github.artificial_intellicrafters.merlin_ai.api.util.OrablePredicate;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.PathingChunkSection;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.hierarchy.ChunkSectionRegionsImpl;
import io.github.artificial_intellicrafters.merlin_ai_test.common.BasicAIPathNode;
import io.github.artificial_intellicrafters.merlin_ai_test.common.MerlinAITest;
import io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test.*;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayFIFOQueue;
import it.unimi.dsi.fastutil.shorts.ShortPriorityQueue;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.option.KeyBind;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.BitSetVoxelSet;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientTickEvents;

import java.util.OptionalLong;

public final class LocationCacheTest {
	public static final ValidLocationSetType<BasicLocationType> ONE_X_TWO_BASIC_LOCATION_SET_TYPE;
	public static final NeighbourGetter<Entity, BasicAIPathNode> BASIC_NEIGHBOUR_GETTER;
	public static final HierarchyInfo<BasicLocationType, Void, CacheData, Tmp> HIERARCHY_INFO;
	public static final KeyBind PATH_KEYBIND = new KeyBind("merlin_ai.location_cache_test", GLFW.GLFW_KEY_F7, "misc");
	public static final KeyBind REGION_KEYBIND = new KeyBind("merlin_ai.region_test", GLFW.GLFW_KEY_F8, "misc");
	public static final KeyBind LINK_KEYBIND = new KeyBind("merlin_ai.link_test", GLFW.GLFW_KEY_F9, "misc");
	private static AIPath<Entity, BasicAIPathNode> LAST_PATH = null;
	private static int REMAINING_VISIBLE_TICKS = 0;
	private static final Long2ObjectMap<BakeableDebugRenderers.Key> VISIBLE = new Long2ObjectOpenHashMap<>();
	private static ChunkSectionPos LAST_REGIONS_POS = null;
	private static int REMAINING_VISIBLE_REGION_TICKS = 0;
	private static int REMAINING_VISIBLE_LINK_TICKS = 0;
	private static @Nullable ChunkSectionPos LAST_SECTION_POS = null;
	private static OptionalLong LAST_SECTION = OptionalLong.empty();

	public static void init() {
		KeyBindingHelper.registerKeyBinding(PATH_KEYBIND);
		KeyBindingHelper.registerKeyBinding(REGION_KEYBIND);
		KeyBindingHelper.registerKeyBinding(LINK_KEYBIND);
		ClientTickEvents.START.register(client -> {
			if (PATH_KEYBIND.wasPressed()) {
				final AIPather<Entity, BasicAIPathNode> pather = new AIPather<>(client.world, new TestNodeProducer(ONE_X_TWO_BASIC_LOCATION_SET_TYPE), Entity::getBlockPos);
				LAST_PATH = pather.calculatePath(PathTarget.yLevel(-64), 256, true, client.cameraEntity);
				if (LAST_PATH != null) {
					REMAINING_VISIBLE_TICKS = 600;
				}
			}
			if (REGION_KEYBIND.wasPressed()) {
				final BlockPos pos = client.cameraEntity.getBlockPos();
				LAST_REGIONS_POS = ChunkSectionPos.from(pos);
				VISIBLE.clear();
				REMAINING_VISIBLE_REGION_TICKS = 600;
			}
			if (LINK_KEYBIND.isPressed()) {
				final ChunkSectionPos sectionPos = ChunkSectionPos.from(client.cameraEntity.getBlockPos());
				final short local = ChunkSectionPos.packLocal(client.cameraEntity.getBlockPos());
				final ChunkRegionGraph.Entry entry = ((AIWorld) client.world).merlin_ai$getChunkGraph().getEntry(sectionPos);
				if (entry != null) {
					final ChunkSectionRegions regions = entry.getRegions(HIERARCHY_INFO, client.world.getTime(), null);
					if (regions != null) {
						final ChunkSectionRegion region = regions.query(local);
						if (region != null) {
							LAST_SECTION = OptionalLong.of(region.id());
							LAST_SECTION_POS = sectionPos;
							REMAINING_VISIBLE_LINK_TICKS = 6000;
						}
					}
				}
			}
		});
		WorldRenderEvents.START.register(context -> {
			if (LAST_PATH != null && REMAINING_VISIBLE_LINK_TICKS > 0) {
				final DustParticleEffect effect = new DustParticleEffect(new Vector3f(1, 0, 0), 1);
				if (REMAINING_VISIBLE_LINK_TICKS % 10 == 0) {
					for (final Object o : LAST_PATH.getNodes()) {
						final BasicAIPathNode node = (BasicAIPathNode) o;
						context.world().addParticle(effect, node.x + 0.5, node.y + 0.5, node.z + 0.5, 0, 0, 0);
					}
				}
				REMAINING_VISIBLE_TICKS--;
				if (REMAINING_VISIBLE_TICKS == 0) {
					LAST_PATH = null;
				}
			}
			if (REMAINING_VISIBLE_LINK_TICKS > 0) {
				REMAINING_VISIBLE_LINK_TICKS--;
				if (LAST_SECTION.isPresent() && LAST_SECTION_POS != null) {
					final BlockPos minPos = LAST_SECTION_POS.getMinPos();
					final ShapeCache cache = ShapeCache.create(context.world(), minPos.add(-16, -16, -16), minPos.add(16, 16, 16), null);
					final ChunkSectionRegionConnectivityGraph<Void> graph = cache.getGraph(minPos.getX(), minPos.getY(), minPos.getZ(), HIERARCHY_INFO);
					if (graph != null) {
						final LongIterator iterator = graph.unconditionalLinks(LAST_SECTION.getAsLong());
						while (iterator.hasNext()) {
							final long key = iterator.nextLong();
							final ChunkSectionRegion region = cache.getRegion(key, HIERARCHY_INFO);
							if (region != null) {
								final ChunkSectionPos pos = ChunkSectionRegionsImpl.unpackChunkSectionPosCompact(region.id(), cache);
								final BitSetVoxelSet set = new BitSetVoxelSet(16, 16, 16);
								for (int i = 0; i < 16; i++) {
									for (int j = 0; j < 16; j++) {
										for (int k = 0; k < 16; k++) {
											if (region.contains(PathingChunkSection.packLocal(i, j, k))) {
												set.set(i, j, k);
											}
										}
									}
								}
								final int color = (int) HashCommon.murmurHash3(HashCommon.murmurHash3(key)) | 0xFF00_0000;
								final VertexConsumer buffer = context.consumers().getBuffer(RenderLayer.LINES);
								final int ox = pos.getMinX();
								final int oy = pos.getMinY();
								final int oz = pos.getMinZ();
								final MatrixStack matrices = context.matrixStack();
								matrices.push();
								final Vec3d d = context.camera().getPos();
								matrices.translate(-d.x, -d.y, -d.z);
								set.forEachEdge((i, j, k, l, m, n) -> {
									final Vec3d start = new Vec3d(i, j, k);
									final Vec3d end = new Vec3d(l, m, n);
									buffer.m_rkxaaknb(matrices.peek().getModel(), (float) start.x + ox, (float) start.y + oy, (float) start.z + oz).color(color).normal(0, 1, 0).next();
									buffer.m_rkxaaknb(matrices.peek().getModel(), (float) end.x + ox, (float) end.y + oy, (float) end.z + oz).color(color).normal(0, 1, 0).next();
								}, true);
								matrices.pop();
							}
						}
					}
				}
			}
		});
		ClientTickEvents.END.register(client -> {
			if (LAST_REGIONS_POS != null && REMAINING_VISIBLE_REGION_TICKS > 0) {
				if (client.world == null) {
					return;
				}
				for (int offX = -1; offX <= 1; offX++) {
					for (int offY = -1; offY <= 1; offY++) {
						for (int offZ = -1; offZ <= 1; offZ++) {
							final ChunkSectionPos sectionPos = LAST_REGIONS_POS.add(offX, offY, offZ);
							final ChunkRegionGraph.Entry e = ((AIWorld) client.world).merlin_ai$getChunkGraph().getEntry(sectionPos.getMinX(), sectionPos.getMinY(), sectionPos.getMinZ());
							final ChunkSectionRegions lastRegions = e == null ? null : e.getRegions(HIERARCHY_INFO, client.world.getTime(), null);
							final long chunkKey = sectionPos.asLong();
							final boolean b = VISIBLE.containsKey(chunkKey);
							if (lastRegions == null && b) {
								VISIBLE.remove(chunkKey).delete();
							} else if (lastRegions != null && !b) {
								final Matrix4f matrix4f = new Matrix4f();
								matrix4f.identity();
								VISIBLE.put(chunkKey, BakeableDebugRenderers.render(consumers -> {
									final Long2ObjectMap<VoxelSet> sets = new Long2ObjectOpenHashMap<>();
									for (int i = 0; i < 16; i++) {
										for (int j = 0; j < 16; j++) {
											for (int k = 0; k < 16; k++) {
												final ChunkSectionRegion query = lastRegions.query(PathingChunkSection.packLocal(i, j, k));
												if (query != null) {
													sets.computeIfAbsent(query.id(), l -> new BitSetVoxelSet(16, 16, 16)).set(i, j, k);
												}
											}
										}
									}
									final VertexConsumer consumer = consumers.getBuffer(RenderLayer.LINES);
									for (final Long2ObjectMap.Entry<VoxelSet> entry : sets.long2ObjectEntrySet()) {
										final int color = (int) HashCommon.murmurHash3(HashCommon.murmurHash3(entry.getLongKey() + sectionPos.asLong())) | 0xFF00_0000;
										final int ox = sectionPos.getMinX();
										final int oy = sectionPos.getMinY();
										final int oz = sectionPos.getMinZ();
										entry.getValue().forEachEdge((i, j, k, l, m, n) -> {
											final Vec3d start = new Vec3d(i, j, k);
											final Vec3d end = new Vec3d(l, m, n);
											consumer.vertex((float) start.x + ox, (float) start.y + oy, (float) start.z + oz).color(color).normal(0, 1, 0).next();
											consumer.vertex((float) end.x + ox, (float) end.y + oy, (float) end.z + oz).color(color).normal(0, 1, 0).next();
										}, true);
									}
								}));
							}
						}
					}
				}
				REMAINING_VISIBLE_REGION_TICKS--;
				if (REMAINING_VISIBLE_REGION_TICKS == 0) {
					VISIBLE.clear();
					LAST_REGIONS_POS = null;
				}
			}
		});
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
			private static final Box BOX = new Box(0, 0, 0, 1, 2, 1);
			private static final Box FLOOR = new Box(0, -0.001, 0, 1, 0, 1);

			@Override
			public BasicLocationType classify(final int x, final int y, final int z, final ShapeCache cache, final AITaskExecutionContext executionContext) {
				if (CollisionUtil.doesCollide(BOX.offset(x, y, z), cache)) {
					return BasicLocationType.CLOSED;
				}
				if (CollisionUtil.doesCollide(FLOOR.offset(x, y, z), cache)) {
					return BasicLocationType.GROUND;
				}
				return BasicLocationType.OPEN;
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
			public Pair<ChunkSectionRegions, CacheData> regionify(final ShapeCache shapeCache, final ChunkSectionPos pos, final ValidLocationSetType<BasicLocationType> type, final HeightLimitView limitView, AITaskExecutionContext executionContext) {
				int missing = 0;
				for (int i = -1; i <= 1; i++) {
					for (int j = -1; j <= 1; j++) {
						if (!shapeCache.isOutOfHeightLimit(pos.getMinY() + j * 16)) {
							for (int k = -1; k <= 1; k++) {
								if (!shapeCache.doesLocationSetExist(pos.getMinX() + i * 16, pos.getMinY() + j * 16, pos.getMinZ() + k * 16, type)) {
									shapeCache.getLocationType(pos.getMinX() + i * 16, pos.getMinY() + j * 16, pos.getMinZ() + k * 16, type);
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
								floodFill(x, y, z, shapeCache, builder, type, pos, data);
							}
						}
					}
				}
				return Pair.of(builder.build(), data);
			}

			private void check(final int x, final int y, final int z, final int sX, final int sY, final int sZ, final int offY, final ShapeCache cache, final ChunkSectionRegions.RegionKey key, final ChunkSectionRegions.Builder builder, final ValidLocationSetType<BasicLocationType> type, final ShortPriorityQueue queue, final LongSet outgoing, final boolean expand) {
				final int height = y + sY + offY;
				final boolean limit = cache.isOutOfHeightLimit(height);
				short i = PathingChunkSection.packLocal(x + 1, y + offY, z);
				if (x != 15) {
					if (expand && !builder.contains(i) && cache.getLocationType(x + 1 + sX, height, z + sZ, type) == BasicLocationType.GROUND) {
						queue.enqueue(i);
						builder.expand(key, i);
					}
				}
				if (!limit && !builder.contains(i)) {
					outgoing.add(BlockPos.asLong(x + sX + 1, height, z + sZ));
				}
				i = PathingChunkSection.packLocal(x - 1, y + offY, z);
				if (x != 0) {
					if (expand && !builder.contains(i) && cache.getLocationType(x - 1 + sX, height, z + sZ, type) == BasicLocationType.GROUND) {
						queue.enqueue(i);
						builder.expand(key, i);
					}
				}
				if (!limit && !builder.contains(i)) {
					outgoing.add(BlockPos.asLong(x + sX - 1, height, z + sZ));
				}
				i = PathingChunkSection.packLocal(x, y + offY, z + 1);
				if (z != 15) {
					if (expand && !builder.contains(i) && cache.getLocationType(x + sX, height, z + 1 + sZ, type) == BasicLocationType.GROUND) {
						queue.enqueue(i);
						builder.expand(key, i);
					}
				}
				if (!limit && !builder.contains(i)) {
					outgoing.add(BlockPos.asLong(x + sX, height, z + sZ + 1));
				}
				i = PathingChunkSection.packLocal(x, y + offY, z - 1);
				if (z != 0) {
					if (expand && !builder.contains(i) && cache.getLocationType(x + sX, height, z - 1 + sZ, type) == BasicLocationType.GROUND) {
						queue.enqueue(i);
						builder.expand(key, i);
					}
				}
				if (!limit && !builder.contains(i)) {
					outgoing.add(BlockPos.asLong(x + sX, height, z + sZ - 1));
				}
			}

			private void floodFill(final int lx, final int ly, final int lz, final ShapeCache cache, final ChunkSectionRegions.Builder builder, final ValidLocationSetType<BasicLocationType> type, final ChunkSectionPos pos, final CacheData data) {
				final int sX = pos.getMinX();
				final int sY = pos.getMinY();
				final int sZ = pos.getMinZ();
				if (cache.getLocationType(lx + sX, ly + sY, lz + sZ, type) != BasicLocationType.GROUND) {
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
					check(x, y, z, sX, sY, sZ, 0, cache, key, builder, type, queue, outgoing, true);
					check(x, y, z, sX, sY, sZ, -1, cache, key, builder, type, queue, outgoing, y != 0);
					check(x, y, z, sX, sY, sZ, 1, cache, key, builder, type, queue, outgoing, y != 15);
				}
			}

			@Override
			public ChunkSectionRegionConnectivityGraph<Void> link(final CacheData precomputed, final ShapeCache shapeCache, final ChunkSectionPos pos, final ChunkSectionRegions regions, AITaskExecutionContext executionContext) {
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
							ChunkSectionRegions chunk = shapeCache.getRegions(x, y, z, HIERARCHY_INFO);
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
							while (!shapeCache.isOutOfHeightLimit(y) && shapeCache.getLocationType(x, y, z, ONE_X_TWO_BASIC_LOCATION_SET_TYPE) == BasicLocationType.OPEN) {
								y--;
							}
							if (!shapeCache.isOutOfHeightLimit(y) && shapeCache.getLocationType(x, y, z, ONE_X_TWO_BASIC_LOCATION_SET_TYPE) == BasicLocationType.GROUND) {
								chunk = shapeCache.getRegions(x, y, z, HIERARCHY_INFO);
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
