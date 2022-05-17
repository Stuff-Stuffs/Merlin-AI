package io.github.artificial_intellicrafters.merlin_ai_test.client;

import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegion;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionTypeRegistry;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai_test.common.BasicAIPathNode;
import io.github.artificial_intellicrafters.merlin_ai_test.common.MerlinAITest;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBind;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.BitSetVoxelSet;
import org.lwjgl.glfw.GLFW;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientTickEvents;

import java.util.Random;
import java.util.Set;

public final class RegionCacheTest {
	public static final ChunkSectionRegionType<Entity, BasicAIPathNode> BASIC_REGION_TYPE;
	public static final KeyBind REGION_KEYBIND = new KeyBind("merlin_ai.location_region_cache_test", GLFW.GLFW_KEY_F8, "misc");
	private static final Int2ReferenceMap<BitSetVoxelSet> VOXEL_SETS = new Int2ReferenceOpenHashMap<>();
	private static ChunkSectionPos DISPLAY_POS = null;
	private static int DISPLAY_TICKS = 0;

	public static void init() {
		KeyBindingHelper.registerKeyBinding(REGION_KEYBIND);
		ClientTickEvents.START.register(client -> {
			if (DISPLAY_TICKS > 0) {
				DISPLAY_TICKS--;
			}
			if (REGION_KEYBIND.wasPressed()) {
				final ChunkSectionPos pos = ChunkSectionPos.from(client.cameraEntity.getBlockPos());
				final BlockPos minPos = pos.getMinPos();
				final ShapeCache cache = ShapeCache.create(client.world, minPos.add(-15, -15, -15), minPos.add(16, 16, 16));
				VOXEL_SETS.clear();
				DISPLAY_POS = pos;
				DISPLAY_TICKS = 600;
				final Int2IntMap counts = new Int2IntOpenHashMap();
				for (int x = 0; x < 16; x++) {
					for (int y = 0; y < 16; y++) {
						for (int z = 0; z < 16; z++) {
							final int x0 = x + minPos.getX();
							final int y0 = y + minPos.getY();
							final int z0 = z + minPos.getZ();
							final ChunkSectionRegion<Entity, BasicAIPathNode> region = cache.getRegion(x0, y0, z0, BASIC_REGION_TYPE);
							if (region != null) {
								VOXEL_SETS.computeIfAbsent(region.id(), i -> new BitSetVoxelSet(16, 16, 16)).set(x, y, z);
								counts.put(region.id(), counts.get(region.id()) + 1);
							}
						}
					}
				}
				for (final Int2IntMap.Entry entry : counts.int2IntEntrySet()) {
					if (entry.getIntValue() < 2) {
						VOXEL_SETS.remove(entry.getIntKey());
					}
				}
			}
		});
		WorldRenderEvents.AFTER_ENTITIES.register(context -> {
			if (DISPLAY_TICKS > 0 && DISPLAY_POS != null) {
				final MatrixStack stack = context.matrixStack();
				stack.push();
				stack.translate(-context.camera().getPos().x, -context.camera().getPos().y, -context.camera().getPos().z);
				stack.translate(DISPLAY_POS.getMinX(), DISPLAY_POS.getMinY(), DISPLAY_POS.getMinZ());
				final VertexConsumer vertexConsumer = context.consumers().getBuffer(RenderLayer.getLines());
				for (final Int2ReferenceMap.Entry<BitSetVoxelSet> entry : VOXEL_SETS.int2ReferenceEntrySet()) {
					final Random random = new Random(HashCommon.murmurHash3(HashCommon.murmurHash3(entry.getIntKey())));
					final int colour = MathHelper.hsvToRgb(random.nextFloat(), 1, 1) | 0xFF000000;
					entry.getValue().forEachBox((i, j, k, l, m, n) -> WorldRenderer.drawBox(stack, vertexConsumer, i + 0.25, j + 0.25, k + 0.25, l - 0.25, m - 0.25, n - 0.25, ((colour >> 16) & 0xFF) / 255.0F, ((colour >> 8) & 0xFF) / 255.0F, ((colour >> 0) & 0xFF) / 255.0F, 1), true);
				}
				stack.pop();
			}
		});
	}

	static {
		ChunkSectionRegionTypeRegistry.INSTANCE.register(Set.of(LocationCacheTest.ONE_X_TWO_BASIC_LOCATION_SET_TYPE), LocationCacheTest.BASIC_NEIGHBOUR_GETTER, new Identifier(MerlinAITest.MOD_ID, "basic_test"));
		BASIC_REGION_TYPE = ChunkSectionRegionTypeRegistry.INSTANCE.get(LocationCacheTest.BASIC_NEIGHBOUR_GETTER, new Identifier(MerlinAITest.MOD_ID, "basic_test"));
	}
}
