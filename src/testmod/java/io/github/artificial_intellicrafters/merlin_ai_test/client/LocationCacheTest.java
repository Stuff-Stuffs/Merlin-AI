package io.github.artificial_intellicrafters.merlin_ai_test.client;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationClassifier;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetTypeRegistry;
import io.github.artificial_intellicrafters.merlin_ai.api.path.NeighbourGetter;
import io.github.artificial_intellicrafters.merlin_ai.api.util.CollisionUtil;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai_test.common.BasicAIPathNode;
import io.github.artificial_intellicrafters.merlin_ai_test.common.MerlinAITest;
import io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test.*;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBind;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3f;
import org.lwjgl.glfw.GLFW;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientTickEvents;

public final class LocationCacheTest {
	public static final ValidLocationSetType<BasicLocationType> ONE_X_TWO_BASIC_LOCATION_SET_TYPE;
	public static final NeighbourGetter<Entity, BasicAIPathNode> BASIC_NEIGHBOUR_GETTER;
	public static final KeyBind PATH_KEYBIND = new KeyBind("merlin_ai.location_cache_test", GLFW.GLFW_KEY_F7, "misc");
	private static AIPath<Entity, BasicAIPathNode> LAST_PATH = null;
	private static int REMAINING_VISIBLE_TICKS = 0;

	public static void init() {
		KeyBindingHelper.registerKeyBinding(PATH_KEYBIND);
		ClientTickEvents.START.register(client -> {
			if (REMAINING_VISIBLE_TICKS > 0) {
				REMAINING_VISIBLE_TICKS--;
			}
			if (REMAINING_VISIBLE_TICKS == 0) {
				LAST_PATH = null;
			}
			if (PATH_KEYBIND.wasPressed()) {
				final AIPather<Entity, BasicAIPathNode> pather = new AIPather<>(client.world, new TestNodeProducer(ONE_X_TWO_BASIC_LOCATION_SET_TYPE), Entity::getBlockPos, RegionCacheTest.BASIC_REGION_TYPE);
				LAST_PATH = pather.calculatePath(PathTarget.createBlockTarget(35, BlockPos.ORIGIN), 1000, true, client.cameraEntity);
				if (LAST_PATH != null) {
					REMAINING_VISIBLE_TICKS = 6000;
				}
			}
		});
		WorldRenderEvents.START.register(context -> {
			if (LAST_PATH != null && REMAINING_VISIBLE_TICKS > 0) {
				final DustParticleEffect effect = new DustParticleEffect(new Vec3f(1, 0, 0), 1);
				if (REMAINING_VISIBLE_TICKS % 10 == 0) {
					for (final Object o : LAST_PATH.getNodes()) {
						final BasicAIPathNode node = (BasicAIPathNode) o;
						context.world().addParticle(effect, node.x + 0.5, node.y + 0.5, node.z + 0.5, 0, 0, 0);
					}
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
			public BasicLocationType validate(final int x, final int y, final int z, final ShapeCache cache) {
				if (CollisionUtil.doesCollide(BOX.offset(x, y, z), cache)) {
					return BasicLocationType.CLOSED;
				}
				if (CollisionUtil.doesCollide(FLOOR.offset(x, y, z), cache)) {
					return BasicLocationType.GROUND;
				}
				return BasicLocationType.OPEN;
			}
		}, BasicLocationType.class, new Identifier(MerlinAITest.MOD_ID, "basic_1x2"));
		ONE_X_TWO_BASIC_LOCATION_SET_TYPE = ValidLocationSetTypeRegistry.INSTANCE.get(BasicLocationType.class, new Identifier(MerlinAITest.MOD_ID, "basic_1x2"));
		BASIC_NEIGHBOUR_GETTER = new TestNodeProducer(ONE_X_TWO_BASIC_LOCATION_SET_TYPE);
	}
}
