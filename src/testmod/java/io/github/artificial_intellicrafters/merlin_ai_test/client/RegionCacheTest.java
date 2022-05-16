package io.github.artificial_intellicrafters.merlin_ai_test.client;

import io.github.artificial_intellicrafters.merlin_ai.api.region.BasicRegionClassifier;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegion;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionTypeRegistry;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai_test.common.MerlinAITest;
import io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test.BasicLocationType;
import it.unimi.dsi.fastutil.HashCommon;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBind;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3f;
import org.lwjgl.glfw.GLFW;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientTickEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class RegionCacheTest {
	public static final ChunkSectionRegionType BASIC_REGION_TYPE;
	public static final KeyBind REGION_KEYBIND = new KeyBind("merlin_ai.location_region_cache_test", GLFW.GLFW_KEY_F8, "misc");

	public static void init() {
		KeyBindingHelper.registerKeyBinding(REGION_KEYBIND);
		ClientTickEvents.START.register(client -> {
			if (REGION_KEYBIND.wasPressed()) {
				final ChunkSectionPos pos = ChunkSectionPos.from(client.cameraEntity.getBlockPos());
				final BlockPos minPos = pos.getMinPos();
				final ShapeCache cache = ShapeCache.create(client.world, minPos, minPos.add(15, 15, 15));
				for (int x = 0; x < 16; x++) {
					for (int y = 0; y < 16; y++) {
						for (int z = 0; z < 16; z++) {
							final int x0 = x + minPos.getX();
							final int y0 = y + minPos.getY();
							final int z0 = z + minPos.getZ();
							final ChunkSectionRegion region = cache.getRegion(x0, y0, z0, BASIC_REGION_TYPE);
							if (region != null) {
								final Random random = new Random(HashCommon.murmurHash3(HashCommon.murmurHash3(region.id())));
								final int i = MathHelper.hsvToRgb(random.nextFloat(), 1, 1);
								final DustParticleEffect effect = new DustParticleEffect(new Vec3f(((i >> 16) & 0xFF) / 255.0F, ((i >> 8) & 0xFF) / 255.0F, ((i >> 0) & 0xFF) / 255.0F), 1);
								client.world.addParticle(effect, x0 + 0.5, y0 + 0.5, z0 + 0.5, 0, 0, 0);
							}
						}
					}
				}
			}
		});
	}

	static {
		final List<BasicRegionClassifier.SymmetricMovement> movements = new ArrayList<>();
		movements.add(new BasicRegionClassifier.SymmetricMovement() {
			@Override
			public int xOff() {
				return 1;
			}

			@Override
			public int yOff() {
				return 0;
			}

			@Override
			public int zOff() {
				return 0;
			}

			@Override
			public boolean check(final int x, final int y, final int z, final ShapeCache cache) {
				final BasicLocationType locationType = cache.getLocationType(x + 1, y, z, LocationCacheTest.ONE_X_TWO_BASIC_LOCATION_SET_TYPE);
				return locationType == BasicLocationType.GROUND;
			}

			@Override
			public boolean checkReverse(final int x, final int y, final int z, final ShapeCache cache) {
				final BasicLocationType locationType = cache.getLocationType(x - 1, y, z, LocationCacheTest.ONE_X_TWO_BASIC_LOCATION_SET_TYPE);
				return locationType == BasicLocationType.GROUND;
			}

			@Override
			public boolean validStart(final int x, final int y, final int z, final ShapeCache cache) {
				return cache.getLocationType(x, y, z, LocationCacheTest.ONE_X_TWO_BASIC_LOCATION_SET_TYPE) == BasicLocationType.GROUND;
			}
		});
		movements.add(new BasicRegionClassifier.SymmetricMovement() {
			@Override
			public int xOff() {
				return 0;
			}

			@Override
			public int yOff() {
				return 0;
			}

			@Override
			public int zOff() {
				return 1;
			}

			@Override
			public boolean check(final int x, final int y, final int z, final ShapeCache cache) {
				final BasicLocationType locationType = cache.getLocationType(x, y, z + 1, LocationCacheTest.ONE_X_TWO_BASIC_LOCATION_SET_TYPE);
				return locationType == BasicLocationType.GROUND;
			}

			@Override
			public boolean checkReverse(final int x, final int y, final int z, final ShapeCache cache) {
				final BasicLocationType locationType = cache.getLocationType(x, y, z - 1, LocationCacheTest.ONE_X_TWO_BASIC_LOCATION_SET_TYPE);
				return locationType == BasicLocationType.GROUND;
			}

			@Override
			public boolean validStart(final int x, final int y, final int z, final ShapeCache cache) {
				return cache.getLocationType(x, y, z, LocationCacheTest.ONE_X_TWO_BASIC_LOCATION_SET_TYPE) == BasicLocationType.GROUND;
			}
		});
		ChunkSectionRegionTypeRegistry.INSTANCE.register(Set.of(LocationCacheTest.ONE_X_TWO_BASIC_LOCATION_SET_TYPE), new BasicRegionClassifier(movements), new Identifier(MerlinAITest.MOD_ID, "basic_test"));
		BASIC_REGION_TYPE = ChunkSectionRegionTypeRegistry.INSTANCE.get(new Identifier(MerlinAITest.MOD_ID, "basic_test"));
	}
}
