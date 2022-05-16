package io.github.artificial_intellicrafters.merlin_ai.impl.common.task;

import io.github.artificial_intellicrafters.merlin_ai.api.PathingChunkSection;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITask;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.region.ChunkSectionRegionImpl;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.region.ChunkSectionRegionsImpl;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public class ChunkRegionsAnalysisAITask implements AITask {
	private final long modCount;
	private final LongSupplier currentModCount;
	private final ChunkSectionRegionType type;
	private final ChunkSectionPos pos;
	private final PathingChunkSection section;
	private final ShapeCache cache;
	private final Consumer<ChunkSectionRegions> completionConsumer;
	private ChunkSectionRegions output = null;
	private boolean finished = false;

	public ChunkRegionsAnalysisAITask(final long modCount, final LongSupplier currentModCount, final ChunkSectionRegionType type, final ChunkSectionPos pos, final PathingChunkSection section, final ShapeCache cache, final Consumer<ChunkSectionRegions> completionConsumer) {
		this.modCount = modCount;
		this.currentModCount = currentModCount;
		this.type = type;
		this.pos = pos;
		this.section = section;
		this.cache = cache;
		this.completionConsumer = completionConsumer;
	}

	@Override
	public int priority() {
		return AITask.super.priority() + 1;
	}

	@Override
	public boolean done() {
		return output != null || modCount != currentModCount.getAsLong();
	}

	@Override
	public void runIteration() {
		final int x = pos.getMinX();
		final int y = pos.getMinY();
		final int z = pos.getMinZ();
		boolean ready = true;
		for (final ValidLocationSetType<?> dependency : type.dependencies()) {
			if (!cache.locationSetExists(x,y,z, dependency)) {
				ready = false;
				break;
			}
		}
		if (ready) {
			final List<ShortSet> sets = new ArrayList<>();
			type.classifier().classify(cache, pos, sets::add, s -> {
				for (final ShortSet set : sets) {
					if (set.contains(s)) {
						return true;
					}
				}
				return false;
			});
			final ChunkSectionRegionImpl[] regions = new ChunkSectionRegionImpl[sets.size()];
			int i = 0;
			for (final ShortSet set : sets) {
				final int nextRegionId = section.getNextRegionId();
				regions[i] = new ChunkSectionRegionImpl(nextRegionId, set);
				section.setNextRegionId(nextRegionId + 1);
				i++;
			}
			output = new ChunkSectionRegionsImpl(regions);
		}
	}

	@Override
	public void runFinish() {
		if (finished) {
			throw new RuntimeException("Tried to call runFinish twice!");
		}
		if (output != null) {
			if (modCount == currentModCount.getAsLong()) {
				completionConsumer.accept(output);
				finished = true;
			}
		}
	}
}
