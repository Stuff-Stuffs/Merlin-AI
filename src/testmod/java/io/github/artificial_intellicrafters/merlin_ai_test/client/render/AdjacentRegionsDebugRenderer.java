package io.github.artificial_intellicrafters.merlin_ai_test.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegion;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegionConnectivityGraph;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.PathingChunkSection;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.hierarchy.ChunkSectionRegionsImpl;
import io.github.artificial_intellicrafters.merlin_ai_test.client.LocationCacheTest;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBind;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.BitSetVoxelSet;

public final class AdjacentRegionsDebugRenderer extends AbstractDebugRenderer {
	private BlockPos pos = null;

	public AdjacentRegionsDebugRenderer(final KeyBind bind, final int time) {
		super(bind);
	}

	@Override
	protected void renderDebug(final WorldRenderContext context) {
		final ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(pos);
		final BlockPos minPos = chunkSectionPos.getMinPos();
		final ShapeCache cache = ShapeCache.create(context.world(), minPos.add(-64, -64, -64), minPos.add(64, 64, 64));
		final ChunkSectionRegions regions = cache.getRegions(pos.getX(), pos.getY(), pos.getZ(), LocationCacheTest.HIERARCHY_INFO, null);
		final ChunkSectionRegionConnectivityGraph<Void> graph = cache.getGraph(minPos.getX(), minPos.getY(), minPos.getZ(), LocationCacheTest.HIERARCHY_INFO, null);
		if (regions != null && graph != null) {
			final ChunkSectionRegion query = regions.query(PathingChunkSection.packLocal(pos.getX(), pos.getY(), pos.getZ()));
			if (query == null) {
				return;
			}
			final LongIterator iterator = graph.unconditionalLinks(query.id());
			while (iterator.hasNext()) {
				final long key = iterator.nextLong();
				final ChunkSectionRegion region = cache.getRegion(key, LocationCacheTest.HIERARCHY_INFO, null);
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
						buffer.vertex(matrices.peek().getModel(), (float) start.x + ox, (float) start.y + oy, (float) start.z + oz).color(color).normal(0, 1, 0).next();
						buffer.vertex(matrices.peek().getModel(), (float) end.x + ox, (float) end.y + oy, (float) end.z + oz).color(color).normal(0, 1, 0).next();
					}, true);
					matrices.pop();
				}
			}
		}
	}

	@Override
	protected void clearState() {
		pos = null;
	}

	@Override
	protected void setup() {
		final MinecraftClient client = MinecraftClient.getInstance();
		pos = client.cameraEntity.getBlockPos();
	}
}
