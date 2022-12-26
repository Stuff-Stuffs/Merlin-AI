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
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetTypeRegistry;
import io.github.artificial_intellicrafters.merlin_ai.api.path.NeighbourGetter;
import io.github.artificial_intellicrafters.merlin_ai.api.util.CollisionUtil;
import io.github.artificial_intellicrafters.merlin_ai.api.util.OrablePredicate;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.PathingChunkSection;
import io.github.artificial_intellicrafters.merlin_ai_test.common.BasicAIPathNode;
import io.github.artificial_intellicrafters.merlin_ai_test.common.MerlinAITest;
import io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test.*;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayFIFOQueue;
import it.unimi.dsi.fastutil.shorts.ShortPriorityQueue;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBind;
import net.minecraft.client.render.RenderLayer;
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
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientTickEvents;

import java.util.Optional;

public final class LocationCacheTest {
	public static final ValidLocationSetType<BasicLocationType> ONE_X_TWO_BASIC_LOCATION_SET_TYPE;
	public static final NeighbourGetter<Entity, BasicAIPathNode> BASIC_NEIGHBOUR_GETTER;
	public static final HierarchyInfo<BasicLocationType, Void, Void, Tmp> HIERARCHY_INFO;
	public static final KeyBind PATH_KEYBIND = new KeyBind("merlin_ai.location_cache_test", GLFW.GLFW_KEY_F7, "misc");
	public static final KeyBind REGION_KEYBIND = new KeyBind("merlin_ai.region_test", GLFW.GLFW_KEY_F8, "misc");
	private static AIPath<Entity, BasicAIPathNode> LAST_PATH = null;
	private static int REMAINING_VISIBLE_TICKS = 0;
	private static ChunkSectionPos LAST_REGIONS_POS = null;
	private static int REMAINING_VISIBLE_REGION_TICKS = 0;

	public static void init() {
		KeyBindingHelper.registerKeyBinding(PATH_KEYBIND);
		KeyBindingHelper.registerKeyBinding(REGION_KEYBIND);
		ClientTickEvents.START.register(client -> {
			if (PATH_KEYBIND.wasPressed()) {
				final AIPather<Entity, BasicAIPathNode> pather = new AIPather<>(client.world, new TestNodeProducer(ONE_X_TWO_BASIC_LOCATION_SET_TYPE), Entity::getBlockPos);
				LAST_PATH = pather.calculatePath(PathTarget.yLevel(-64), 256, true, client.cameraEntity);
				if (LAST_PATH != null) {
					REMAINING_VISIBLE_TICKS = 6000;
				}
			}
			if (REGION_KEYBIND.wasPressed()) {
				final BlockPos pos = client.cameraEntity.getBlockPos();
				LAST_REGIONS_POS = ChunkSectionPos.from(pos);
				REMAINING_VISIBLE_REGION_TICKS = 6000;
			}
		});
		WorldRenderEvents.START.register(context -> {
			if (LAST_PATH != null && REMAINING_VISIBLE_TICKS > 0) {
				final DustParticleEffect effect = new DustParticleEffect(new Vector3f(1, 0, 0), 1);
				if (REMAINING_VISIBLE_TICKS % 10 == 0) {
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
			final Vec3d d = context.camera().getPos();
			if (LAST_REGIONS_POS != null && REMAINING_VISIBLE_REGION_TICKS > 0) {
				for (int offX = -1; offX <= 1; offX++) {
					for (int offY = -1; offY <= 1; offY++) {
						for (int offZ = -1; offZ <= 1; offZ++) {
							final ChunkRegionGraph.Entry entry1 = ((AIWorld) MinecraftClient.getInstance().world).merlin_ai$getChunkGraph().getEntry(LAST_REGIONS_POS.getMinX() + offX * 16, LAST_REGIONS_POS.getMinY() + offY * 16, LAST_REGIONS_POS.getMinZ() + offZ * 16);
							final ChunkSectionRegions lastRegions = entry1 == null ? null : entry1.getRegions(HIERARCHY_INFO, context.world().getTime());
							if (lastRegions != null) {
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
								final VertexConsumer consumer = context.consumers().getBuffer(RenderLayer.LINES);
								context.matrixStack().push();
								context.matrixStack().translate(-d.x + LAST_REGIONS_POS.getMinX() + offX * 16, -d.y + LAST_REGIONS_POS.getMinY() + offY * 16, -d.z + LAST_REGIONS_POS.getMinZ() + offZ * 16);
								final Matrix4f matrix = context.matrixStack().peek().getModel();
								for (final Long2ObjectMap.Entry<VoxelSet> entry : sets.long2ObjectEntrySet()) {
									final int color = (int) HashCommon.murmurHash3(HashCommon.murmurHash3(entry.getLongKey() + LAST_REGIONS_POS.add(offX, offY, offZ).asLong())) | 0xFF00_0000;
									entry.getValue().forEachEdge((i, j, k, l, m, n) -> {
										final Vec3d start = new Vec3d(i, j, k);
										final Vec3d end = new Vec3d(l, m, n);
										consumer.m_rkxaaknb(matrix, (float) start.x, (float) start.y, (float) start.z).color(color).normal(0, 1, 0).next();
										consumer.m_rkxaaknb(matrix, (float) end.x, (float) end.y, (float) end.z).color(color).normal(0, 1, 0).next();
									}, true);
								}
								context.matrixStack().pop();

							}
						}
					}
				}
				REMAINING_VISIBLE_REGION_TICKS--;
				if (REMAINING_VISIBLE_REGION_TICKS == 0) {
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

	static {
		ValidLocationSetTypeRegistry.INSTANCE.register(BasicLocationType.UNIVERSE_INFO, new ValidLocationClassifier<>() {
			private static final Box BOX = new Box(0, 0, 0, 1, 2, 1);
			private static final Box FLOOR = new Box(0, -1, 0, 1, 0, 1);

			@Override
			public Optional<BasicLocationType> fill(final ChunkSectionPos pos, final ShapeCache cache) {
				//final PathingChunkSection chunk = cache.getPathingChunk(pos.getMinX(), pos.getMinY(), pos.getMinZ());
				//if (chunk != null) {
				//	if (chunk.flagCount(MerlinAITest.FULL_BLOCK) == 16 * 16 * 16) {
				//		return Optional.of(BasicLocationType.CLOSED);
				//	} else if (chunk.flagCount(MerlinAITest.AIR_BLOCK) == 16 * 16 * 16) {
				//		return Optional.of(BasicLocationType.OPEN);
				//	}
				//}
				return Optional.empty();
			}

			@Override
			public BasicLocationType postProcess(final BasicLocationType defaultVal, final int x, final int y, final int z, final ShapeCache cache) {
				if (defaultVal == BasicLocationType.OPEN && y % 16 == 0) {
					if (CollisionUtil.doesCollide(FLOOR.offset(x, y, z), cache)) {
						return BasicLocationType.GROUND;
					}
					return BasicLocationType.OPEN;
				} else {
					return defaultVal;
				}
			}

			@Override
			public BasicLocationType classify(final int x, final int y, final int z, final ShapeCache cache) {
				if (CollisionUtil.doesCollide(BOX.offset(x, y, z), cache)) {
					return BasicLocationType.CLOSED;
				}
				if (CollisionUtil.doesCollide(FLOOR.offset(x, y, z), cache)) {
					return BasicLocationType.GROUND;
				}
				return BasicLocationType.OPEN;
			}

			@Override
			public void rebuild(final BlockState[] updateBlockStates, final short[] updatedPositions, final int updateCount, final int chunkSectionX, final int chunkSectionY, final int chunkSectionZ, final int offsetX, final int offsetY, final int offsetZ, final RebuildConsumer<BasicLocationType> consumer, final ShapeCache cache) {
				if (offsetX != 0 || offsetZ != 0 || offsetY < 0) {
					return;
				}
				for (int i = 0; i < updateCount; i++) {
					final short updatePosition = updatedPositions[i];
					final int x = (chunkSectionX + offsetX) * ChunkSection.SECTION_WIDTH + PathingChunkSection.unpackLocalX(updatePosition);
					final int unpackedYOffset = PathingChunkSection.unpackLocalY(updatePosition);
					final int y = (chunkSectionY + offsetY) * ChunkSection.SECTION_WIDTH + unpackedYOffset;
					final int z = (chunkSectionZ + offsetZ) * ChunkSection.SECTION_WIDTH + PathingChunkSection.unpackLocalZ(updatePosition);
					if (offsetY == 0) {
						consumer.update(classify(x, y, z, cache), x, y, z);
						if (unpackedYOffset < 15) {
							consumer.update(classify(x, y + 1, z, cache), x, y + 1, z);
						}
						if (unpackedYOffset > 0) {
							consumer.update(classify(x, y - 1, z, cache), x, y - 1, z);
						}
					} else if (unpackedYOffset == 0) {
						consumer.update(classify(x, y - 1, z, cache), x, y - 1, z);
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
			public Pair<ChunkSectionRegions, Void> regionify(final ShapeCache shapeCache, final ChunkSectionPos pos, final ValidLocationSetType<BasicLocationType> type, final HeightLimitView limitView) {
				boolean skip = false;
				for (int i = -1; i <= 1; i++) {
					for (int j = -1; j <= 1; j++) {
						for (int k = -1; k <= 1; k++) {
							if (!shapeCache.isOutOfHeightLimit(pos.getMinY() + j * 16)) {
								if (!shapeCache.doesLocationSetExist(pos.getMinX() + i * 16, pos.getMinY() + j * 16, pos.getMinZ() + k * 16, type)) {
									shapeCache.getLocationType(pos.getMinX() + i * 16, pos.getMinY() + j * 16, pos.getMinZ() + k * 16, type);
									skip = true;
								}
							}
						}
					}
				}
				if (skip) {
					return null;
				}
				int x = 0;
				int y;
				int z;
				final ChunkSectionRegions.Builder builder = ChunkSectionRegions.builder(pos, limitView);
				for (; x < 16; x++) {
					for (y = 0; y < 16; y++) {
						for (z = 0; z < 16; z++) {
							if (!builder.contains(PathingChunkSection.packLocal(x, y, z))) {
								floodFill(x, y, z, shapeCache, builder, type, pos);
							}
						}
					}
				}
				return Pair.of(builder.build(), null);
			}

			private void floodFill(final int lx, final int ly, final int lz, final ShapeCache cache, final ChunkSectionRegions.Builder builder, final ValidLocationSetType<BasicLocationType> type, final ChunkSectionPos pos) {
				final int sX = pos.getMinX();
				final int sY = pos.getMinY();
				final int sZ = pos.getMinZ();
				if (cache.getLocationType(lx + sX, ly + sY, lz + sZ, type) != BasicLocationType.GROUND) {
					return;
				}
				final ShortPriorityQueue queue = new ShortArrayFIFOQueue(16 * 16);
				final short local = PathingChunkSection.packLocal(lx, ly, lz);
				queue.enqueue(local);
				final ChunkSectionRegions.RegionKey key = builder.newRegion();
				builder.expand(key, local);
				while (!queue.isEmpty()) {
					final short s = queue.dequeueShort();
					final int x = PathingChunkSection.unpackLocalX(s);
					final int y = PathingChunkSection.unpackLocalY(s);
					final int z = PathingChunkSection.unpackLocalZ(s);
					if (x != 15 && cache.getLocationType(x + 1 + sX, y + sY, z + sZ, type) == BasicLocationType.GROUND) {
						final short i = PathingChunkSection.packLocal(x + 1, y, z);
						if (!builder.contains(i)) {
							queue.enqueue(i);
							builder.expand(key, i);
						}
					}
					if (x != 0 && cache.getLocationType(x - 1 + sX, y + sY, z + sZ, type) == BasicLocationType.GROUND) {
						final short i = PathingChunkSection.packLocal(x - 1, y, z);
						if (!builder.contains(i)) {
							queue.enqueue(i);
							builder.expand(key, i);
						}
					}
					if (z != 15 && cache.getLocationType(x + sX, y + sY, z + 1 + sZ, type) == BasicLocationType.GROUND) {
						final short i = PathingChunkSection.packLocal(x, y, z + 1);
						if (!builder.contains(i)) {
							queue.enqueue(i);
							builder.expand(key, i);
						}
					}
					if (z != 0 && cache.getLocationType(x + sX, y + sY, z - 1 + sZ, type) == BasicLocationType.GROUND) {
						final short i = PathingChunkSection.packLocal(x, y, z - 1);
						if (!builder.contains(i)) {
							queue.enqueue(i);
							builder.expand(key, i);
						}
					}
					if (y != 15) {
						if (x != 15 && cache.getLocationType(x + 1 + sX, y + sY + 1, z + sZ, type) == BasicLocationType.GROUND && cache.getLocationType(x + sX, y + sY + 1, z + sZ, type) == BasicLocationType.OPEN) {
							final short i = PathingChunkSection.packLocal(x + 1, y + 1, z);
							if (!builder.contains(i)) {
								queue.enqueue(i);
								builder.expand(key, i);
							}
						}
						if (x != 0 && cache.getLocationType(x - 1 + sX, y + sY + 1, z + sZ, type) == BasicLocationType.GROUND && cache.getLocationType(x + sX, y + sY + 1, z + sZ, type) == BasicLocationType.OPEN) {
							final short i = PathingChunkSection.packLocal(x - 1, y + 1, z);
							if (!builder.contains(i)) {
								queue.enqueue(i);
								builder.expand(key, i);
							}
						}
						if (z != 15 && cache.getLocationType(x + sX, y + sY + 1, z + 1 + sZ, type) == BasicLocationType.GROUND && cache.getLocationType(x + sX, y + sY + 1, z + sZ, type) == BasicLocationType.OPEN) {
							final short i = PathingChunkSection.packLocal(x, y + 1, z + 1);
							if (!builder.contains(i)) {
								queue.enqueue(i);
								builder.expand(key, i);
							}
						}
						if (z != 0 && cache.getLocationType(x + sX, y + sY + 1, z - 1 + sZ, type) == BasicLocationType.GROUND && cache.getLocationType(x + sX, y + sY + 1, z + sZ, type) == BasicLocationType.OPEN) {
							final short i = PathingChunkSection.packLocal(x, y + 1, z - 1);
							if (!builder.contains(i)) {
								queue.enqueue(i);
								builder.expand(key, i);
							}
						}
					}
					if (y != 0) {
						if (x != 15 && cache.getLocationType(x + 1 + sX, y + sY - 1, z + sZ, type) == BasicLocationType.GROUND && cache.getLocationType(x + 1 + sX, y + sY, z + sZ, type) == BasicLocationType.OPEN) {
							final short i = PathingChunkSection.packLocal(x + 1, y - 1, z);
							if (!builder.contains(i)) {
								queue.enqueue(i);
								builder.expand(key, i);
							}
						}
						if (x != 0 && cache.getLocationType(x - 1 + sX, y + sY - 1, z + sZ, type) == BasicLocationType.GROUND && cache.getLocationType(x - 1 + sX, y + sY, z + sZ, type) == BasicLocationType.OPEN) {
							final short i = PathingChunkSection.packLocal(x - 1, y - 1, z);
							if (!builder.contains(i)) {
								queue.enqueue(i);
								builder.expand(key, i);
							}
						}
						if (z != 15 && cache.getLocationType(x + sX, y + sY - 1, z + 1 + sZ, type) == BasicLocationType.GROUND && cache.getLocationType(x + sX, y + sY, z + 1 + sZ, type) == BasicLocationType.OPEN) {
							final short i = PathingChunkSection.packLocal(x, y - 1, z + 1);
							if (!builder.contains(i)) {
								queue.enqueue(i);
								builder.expand(key, i);
							}
						}
						if (z != 0 && cache.getLocationType(x + sX, y + sY - 1, z - 1 + sZ, type) == BasicLocationType.GROUND && cache.getLocationType(x + sX, y + sY, z - 1 + sZ, type) == BasicLocationType.OPEN) {
							final short i = PathingChunkSection.packLocal(x, y - 1, z - 1);
							if (!builder.contains(i)) {
								queue.enqueue(i);
								builder.expand(key, i);
							}
						}
					}
				}
			}

			@Override
			public ChunkSectionRegionConnectivityGraph<Void> link(final Void precomputed, final ShapeCache shapeCache, final ChunkSectionPos pos, final ChunkSectionRegions regions, final ValidLocationSet<BasicLocationType> locationSet) {
				return null;
			}
		};
	}
}
