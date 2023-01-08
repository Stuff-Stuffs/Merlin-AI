package io.github.artificial_intellicrafters.merlin_ai_test.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.artificial_intellicrafters.merlin_ai.api.AIWorld;
import io.github.artificial_intellicrafters.merlin_ai.api.ChunkRegionGraph;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegion;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.PathingChunkSection;
import io.github.artificial_intellicrafters.merlin_ai_test.client.LocationCacheTest;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBind;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.BitSetVoxelSet;
import net.minecraft.util.shape.VoxelSet;

public final class NearbyRegionDebugRenderer extends AbstractDebugRenderer {
	private final Long2ObjectMap<BakeableDebugRenderers.Key> keys;
	private ChunkSectionPos lastPos = null;

	public NearbyRegionDebugRenderer(final KeyBind bind) {
		super(bind);
		keys = new Long2ObjectOpenHashMap<>();
	}

	@Override
	protected void renderDebug(final WorldRenderContext context) {
		final MinecraftClient client = MinecraftClient.getInstance();
		if (lastPos != null) {
			if (client.world == null) {
				return;
			}
			for (int offX = -1; offX <= 1; offX++) {
				for (int offY = -1; offY <= 1; offY++) {
					for (int offZ = -1; offZ <= 1; offZ++) {
						final ChunkSectionPos sectionPos = lastPos.add(offX, offY, offZ);
						final ChunkRegionGraph.Entry e = ((AIWorld) client.world).merlin_ai$getChunkGraph().getEntry(sectionPos.getMinX(), sectionPos.getMinY(), sectionPos.getMinZ());
						final ChunkSectionRegions lastRegions = e == null ? null : e.getRegions(LocationCacheTest.HIERARCHY_INFO, client.world.getTime(), null);
						final long chunkKey = sectionPos.asLong();
						final boolean b = keys.containsKey(chunkKey);
						if (lastRegions == null && b) {
							keys.remove(chunkKey).delete();
						} else if (lastRegions != null && !b) {
							final Vec3d offset = Vec3d.of(sectionPos.getMinPos());
							keys.put(chunkKey, BakeableDebugRenderers.render(consumers -> {
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
									entry.getValue().forEachEdge((i, j, k, l, m, n) -> {
										final Vec3d start = new Vec3d(i, j, k);
										final Vec3d end = new Vec3d(l, m, n);
										consumer.vertex((float) start.x, (float) start.y, (float) start.z).color(color).normal(0, 1, 0).next();
										consumer.vertex((float) end.x, (float) end.y, (float) end.z).color(color).normal(0, 1, 0).next();
									}, true);
								}
							}, () -> offset));
						}
					}
				}
			}
		}
	}

	@Override
	protected void clearState() {
		keys.values().forEach(BakeableDebugRenderers.Key::delete);
		keys.clear();
		lastPos = null;
	}

	@Override
	protected void setup() {
		final BlockPos pos = MinecraftClient.getInstance().cameraEntity.getBlockPos();
		lastPos = ChunkSectionPos.from(pos);
	}
}
