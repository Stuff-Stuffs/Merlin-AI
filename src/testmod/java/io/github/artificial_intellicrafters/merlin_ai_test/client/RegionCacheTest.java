package io.github.artificial_intellicrafters.merlin_ai_test.client;

import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionTypeRegistry;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.api.util.SubChunkSectionUtil;
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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.BitSetVoxelSet;
import org.lwjgl.glfw.GLFW;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientTickEvents;

import java.util.*;

public final class RegionCacheTest {
	public static final ChunkSectionRegionType<Entity, BasicAIPathNode> BASIC_REGION_TYPE;
	public static final KeyBind REGION_KEYBIND = new KeyBind("merlin_ai.location_region_cache_test", GLFW.GLFW_KEY_F8, "misc");
	private static final List<RenderRegion> REGIONS = new ArrayList<>();


	public static void init() {
		KeyBindingHelper.registerKeyBinding(REGION_KEYBIND);
		ClientTickEvents.START.register(client -> {
			REGIONS.removeIf(renderRegion -> renderRegion.displayTicks-- <= 0);
			if (REGION_KEYBIND.wasPressed()) {
				final ChunkSectionPos pos = ChunkSectionPos.from(client.cameraEntity.getBlockPos());
				final BlockPos minPos = pos.getMinPos();
				final ShapeCache cache = ShapeCache.create(client.world, minPos.add(-15, -15, -15), minPos.add(16, 16, 16));
				final Int2IntMap counts = new Int2IntOpenHashMap();
				final int MAX_REGION_COUNT = SubChunkSectionUtil.SUB_SECTION_SIZE * SubChunkSectionUtil.SUB_SECTION_SIZE * SubChunkSectionUtil.SUB_SECTION_SIZE;
				Int2ReferenceMap<BitSetVoxelSet> voxelSets = new Int2ReferenceOpenHashMap<>();
				for (int x = 0; x < 16; x++) {
					for (int y = 0; y < 16; y++) {
						for (int z = 0; z < 16; z++) {
							final int x0 = x + minPos.getX();
							final int y0 = y + minPos.getY();
							final int z0 = z + minPos.getZ();
							final ChunkSectionRegions<Entity, BasicAIPathNode> region = cache.getRegions(x0, y0, z0, BASIC_REGION_TYPE);
							if (region != null) {
								final int containingRegion = region.getContainingRegion(x, y, z);
								if (region.isValidRegion(containingRegion)) {
									final int key = containingRegion + MAX_REGION_COUNT * SubChunkSectionUtil.subSectionIndex(SubChunkSectionUtil.blockToSubSection(x), SubChunkSectionUtil.blockToSubSection(y), SubChunkSectionUtil.blockToSubSection(z));
									voxelSets.computeIfAbsent(key, i -> new BitSetVoxelSet(16, 16, 16)).set(x, y, z);
									counts.put(key, counts.get(key) + 1);
								}
							}
						}
					}
				}
				for (final Int2IntMap.Entry entry : counts.int2IntEntrySet()) {
					if (entry.getIntValue() < 2) {
						voxelSets.remove(entry.getIntKey());
					}
				}
				REGIONS.add(new RenderRegion(voxelSets, pos));
			}
		});
		WorldRenderEvents.AFTER_ENTITIES.register(context -> {
			for (RenderRegion region : REGIONS) {
				final MatrixStack stack = context.matrixStack();
				stack.push();
				stack.translate(-context.camera().getPos().x, -context.camera().getPos().y, -context.camera().getPos().z);
				ChunkSectionPos displayPos = region.displayPos;
				stack.translate(displayPos.getMinX(), displayPos.getMinY(), displayPos.getMinZ());
				final VertexConsumer vertexConsumer = context.consumers().getBuffer(RenderLayer.getLines());
				final Matrix4f model = stack.peek().getModel();
				final Matrix3f normalMat = stack.peek().getNormal();
				for (final Int2ReferenceMap.Entry<BitSetVoxelSet> entry : region.voxelSets.int2ReferenceEntrySet()) {
					final Random random = new Random(HashCommon.murmurHash3(HashCommon.murmurHash3(entry.getIntKey())));
					final int colour = MathHelper.hsvToRgb(random.nextFloat(), 1, 1) | 0xFF000000;
					entry.getValue().forEachEdge((i, j, k, l, m, n) -> {
						final Vec3f normal = new Vec3f(i - l, j - m, k - n);
						normal.normalize();
						vertexConsumer.vertex(model, i, j, k).color(colour >> 16 & 255, colour >> 8 & 255, colour & 255, 255).normal(normalMat, -normal.getX(), -normal.getY(), -normal.getZ()).next();
						vertexConsumer.vertex(model, l, m, n).color(colour >> 16 & 255, colour >> 8 & 255, colour & 255, 255).normal(normalMat, -normal.getX(), -normal.getY(), -normal.getZ()).next();
					}, true);
				}
				stack.pop();
			}
		});
	}

	private static final class RenderRegion {
		private final Int2ReferenceMap<BitSetVoxelSet> voxelSets;
		private final ChunkSectionPos displayPos;
		private int displayTicks = 1000;

		private RenderRegion(Int2ReferenceMap<BitSetVoxelSet> voxelSets, ChunkSectionPos displayPos) {
			this.voxelSets = voxelSets;
			this.displayPos = displayPos;
		}
	}

	static {
		ChunkSectionRegionTypeRegistry.INSTANCE.register(Set.of(LocationCacheTest.ONE_X_TWO_BASIC_LOCATION_SET_TYPE), LocationCacheTest.BASIC_NEIGHBOUR_GETTER, new Identifier(MerlinAITest.MOD_ID, "basic_test"));
		BASIC_REGION_TYPE = ChunkSectionRegionTypeRegistry.INSTANCE.get(LocationCacheTest.BASIC_NEIGHBOUR_GETTER, new Identifier(MerlinAITest.MOD_ID, "basic_test"));
	}
}
