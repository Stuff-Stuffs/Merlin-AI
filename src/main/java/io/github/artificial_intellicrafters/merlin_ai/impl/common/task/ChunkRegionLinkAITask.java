package io.github.artificial_intellicrafters.merlin_ai.impl.common.task;

import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITask;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.region.AbstractChunkSectionRegionImpl;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.Arrays;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ChunkRegionLinkAITask implements AITask {
	private final BooleanSupplier shouldContinue;
	private final ChunkSectionRegionType<?, ?> type;
	private final ChunkSectionPos pos;
	private final Supplier<ShapeCache> cacheFactory;
	private final Consumer<Long2ReferenceMap<long[]>> completionConsumer;
	private Long2ReferenceMap<long[]> output;
	private boolean finished = false;

	public ChunkRegionLinkAITask(BooleanSupplier shouldContinue, ChunkSectionRegionType<?, ?> type, ChunkSectionPos pos, Supplier<ShapeCache> cacheFactory, Consumer<Long2ReferenceMap<long[]>> completionConsumer) {
		this.shouldContinue = shouldContinue;
		this.type = type;
		this.pos = pos;
		this.cacheFactory = cacheFactory;
		this.completionConsumer = completionConsumer;
	}

	@Override
	public int priority() {
		return AITask.super.priority() + 2;
	}

	@Override
	public boolean done() {
		return output != null || !shouldContinue.getAsBoolean();
	}

	@Override
	public void runIteration() {
		boolean ready = true;
		final ShapeCache cache = cacheFactory.get();
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					final int y0 = pos.getMinY() + y * 16;
					if(!cache.getDelegate().isOutOfHeightLimit(y0)) {
						final ChunkSectionRegions<?, ?> regions = cache.getRegions(pos.getMinX() + x * 16, y0, pos.getMinZ() + z * 16, type);
						if (regions == null) {
							ready = false;
						}
					}
				}
			}
		}
		if (ready) {
			output = new Long2ReferenceOpenHashMap<>();
			final ChunkSectionRegions<?, ?> regions = cache.getRegions(pos.getMinX(), pos.getMinY(), pos.getMinZ(), type);
			assert regions != null;
			final LongIterator regionIds = regions.getRegionIds();
			while (regionIds.hasNext()) {
				final long id = regionIds.nextLong();
				final AbstractChunkSectionRegionImpl<?, ?> region = (AbstractChunkSectionRegionImpl<?,?>)regions.getRegionById(id);
				assert region!=null;
				final LongSet outgoingEdges = region.getOutgoingEdges();
				final long[] longs = outgoingEdges.toLongArray();
				Arrays.sort(longs);
				output.put(id, longs);
			}
		}
	}

	@Override
	public void runFinish() {
		if (finished) {
			throw new RuntimeException("Tried to call runFinish twice!");
		}
		if (output != null) {
			completionConsumer.accept(output);
			finished = true;
		}
	}
}
