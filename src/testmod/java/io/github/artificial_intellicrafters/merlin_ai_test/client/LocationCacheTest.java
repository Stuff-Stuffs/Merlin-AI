package io.github.artificial_intellicrafters.merlin_ai_test.client;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationClassifier;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetTypeRegistry;
import io.github.artificial_intellicrafters.merlin_ai.api.path.NeighbourGetter;
import io.github.artificial_intellicrafters.merlin_ai.api.util.CollisionUtil;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.PathingChunkSection;
import io.github.artificial_intellicrafters.merlin_ai_test.common.BasicAIPathNode;
import io.github.artificial_intellicrafters.merlin_ai_test.common.MerlinAITest;
import io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test.*;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.option.KeyBind;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkSection;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientTickEvents;

import java.util.Optional;

public final class LocationCacheTest {
	public static final ValidLocationSetType<BasicLocationType> ONE_X_TWO_BASIC_LOCATION_SET_TYPE;
	public static final NeighbourGetter<Entity, BasicAIPathNode> BASIC_NEIGHBOUR_GETTER;
	public static final KeyBind PATH_KEYBIND = new KeyBind("merlin_ai.location_cache_test", GLFW.GLFW_KEY_F7, "misc");
	private static AIPath<Entity, BasicAIPathNode> LAST_PATH = null;
	private static int REMAINING_VISIBLE_TICKS = 0;

	public static void init() {
		KeyBindingHelper.registerKeyBinding(PATH_KEYBIND);
		ClientTickEvents.START.register(client -> {
			if (PATH_KEYBIND.wasPressed()) {
				final AIPather<Entity, BasicAIPathNode> pather = new AIPather<>(client.world, new TestNodeProducer(ONE_X_TWO_BASIC_LOCATION_SET_TYPE), Entity::getBlockPos);
				LAST_PATH = pather.calculatePath(PathTarget.yLevel(0), 256, true, client.cameraEntity);
				if (LAST_PATH != null) {
					REMAINING_VISIBLE_TICKS = 6000;
				}
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
		});
	}

	private LocationCacheTest() {
	}

	static {
		ValidLocationSetTypeRegistry.INSTANCE.register(BasicLocationType.UNIVERSE_INFO, new ValidLocationClassifier<>() {
			private static final Box BOX = new Box(0, 0, 0, 1, 2, 1);
			private static final Box FLOOR = new Box(0, -1, 0, 1, 0, 1);

			@Override
			public Optional<BasicLocationType> fill(final ChunkSectionPos pos, final ShapeCache cache) {
				final PathingChunkSection chunk = cache.getPathingChunk(pos.getMinX(), pos.getMinY(), pos.getMinZ());
				if (chunk != null) {
					if (chunk.flagCount(MerlinAITest.FULL_BLOCK) == 16 * 16 * 16) {
						return Optional.of(BasicLocationType.CLOSED);
					} else if (chunk.flagCount(MerlinAITest.AIR_BLOCK) == 16 * 16 * 16) {
						return Optional.of(BasicLocationType.OPEN);
					}
				}
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
	}
}
