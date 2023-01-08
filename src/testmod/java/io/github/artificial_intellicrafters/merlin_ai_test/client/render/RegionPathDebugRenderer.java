package io.github.artificial_intellicrafters.merlin_ai_test.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegion;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.util.AStar;
import io.github.artificial_intellicrafters.merlin_ai.api.util.CoarseUtil;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.PathingChunkSection;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.hierarchy.ChunkSectionRegionsImpl;
import io.github.artificial_intellicrafters.merlin_ai_test.client.LocationCacheTest;
import io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test.PathTarget;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBind;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.BitSetVoxelSet;
import net.minecraft.util.shape.VoxelSet;
import org.apache.commons.lang3.time.StopWatch;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class RegionPathDebugRenderer extends AbstractDebugRenderer {
	private final Long2ObjectMap<BakeableDebugRenderers.Key> keys = new Long2ObjectOpenHashMap<>();
	private BlockPos start = null;
	private AStar.PathInfo<CoarseUtil.RegionInfo> lastPath = null;

	public RegionPathDebugRenderer(final KeyBind bind) {
		super(bind);
	}

	@Override
	protected void renderTick() {

	}

	@Override
	protected void renderDebug(final WorldRenderContext context) {

	}

	@Override
	protected void clearState() {
		start = null;
		lastPath = null;
		keys.values().forEach(BakeableDebugRenderers.Key::delete);
		keys.clear();
	}

	@Override
	protected void setup() {
		start = MinecraftClient.getInstance().cameraEntity.getBlockPos();
		final ClientWorld world = MinecraftClient.getInstance().world;
		final ShapeCache cache = ShapeCache.create(world, start.add(-256, -256, -256), start.add(256, 256, 256));
		final ChunkSectionRegions regions = cache.getRegions(start.getX(), start.getY(), start.getZ(), LocationCacheTest.HIERARCHY_INFO, null);
		if (regions == null) {
			return;
		}
		final ChunkSectionRegion query = regions.query(PathingChunkSection.packLocal(start.getX(), start.getY(), start.getZ()));
		final PathTarget target = PathTarget.yLevel(-64);
		if (query != null) {
			final StopWatch stopWatch = StopWatch.createStarted();
			final AStar.PathInfo<CoarseUtil.RegionInfo> path = CoarseUtil.findPathAnyHeuristic(query, cache, pos -> target.heuristic(pos.getX(), pos.getY(), pos.getZ()), 16, 1000000, true, LocationCacheTest.HIERARCHY_INFO);
			stopWatch.stop();
			final double v = stopWatch.getTime(TimeUnit.NANOSECONDS) / 1_000_000D;
			System.out.println("Time: " + v);
			System.out.println("Nodes considered: " + path.nodesConsidered());
			System.out.println("Nodes/Second: " + (path.nodesConsidered() / (v / 1000)));
			lastPath = path;
		}
		if (lastPath != null && lastPath.path() != null) {
			final Map<ChunkSectionRegion, VoxelSet> shapes = new Reference2ReferenceOpenHashMap<>();
			for (final CoarseUtil.RegionInfo info : lastPath.path()) {
				final VoxelSet set = new BitSetVoxelSet(16, 16, 16);
				for (final short packed : info.region().all()) {
					set.set(PathingChunkSection.unpackLocalX(packed), PathingChunkSection.unpackLocalY(packed), PathingChunkSection.unpackLocalZ(packed));
				}
				shapes.put(info.region(), set);
			}
			for (final Map.Entry<ChunkSectionRegion, VoxelSet> entry : shapes.entrySet()) {
				final ChunkSectionPos pos = ChunkSectionRegionsImpl.unpackChunkSectionPosCompact(entry.getKey().id(), world);
				final Vec3d vec = Vec3d.of(pos.getMinPos());
				keys.put(entry.getKey().id(), BakeableDebugRenderers.render(consumers -> {
					final int color = (int) HashCommon.murmurHash3(HashCommon.murmurHash3(entry.getKey().id())) | 0xFF00_0000;
					final VertexConsumer buffer = consumers.getBuffer(RenderLayer.LINES);
					entry.getValue().forEachEdge((i, j, k, l, m, n) -> {
						final Vec3d start = new Vec3d(i, j, k);
						final Vec3d end = new Vec3d(l, m, n);
						final Vec3d delta = end.subtract(start);
						buffer.vertex((float) start.x, (float) start.y, (float) start.z).color(color).normal((float) delta.x, (float) delta.y, (float) delta.z).next();
						buffer.vertex((float) end.x, (float) end.y, (float) end.z).color(color).normal((float) -delta.x, (float) -delta.y, (float) -delta.z).next();
					}, true);
				}, () -> vec));
			}
		}
	}
}
