package io.github.artificial_intellicrafters.merlin_ai.impl.common.task;

import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegionConnectivityGraph;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.HierarchyInfo;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITask;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutionContext;
import io.github.artificial_intellicrafters.merlin_ai.api.util.OrablePredicate;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import net.minecraft.util.math.ChunkSectionPos;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ChunkSectionRegionLinkingAITask<N, C, O extends OrablePredicate<N, O>> implements AITask {
	private final HierarchyInfo<?, N, C, O> info;
	private final Supplier<@Nullable Optional<C>> precomputed;
	private final Supplier<ChunkSectionRegions> regions;
	private final Supplier<ShapeCache> cacheFactory;
	private final BooleanSupplier shouldContinue;
	private final ChunkSectionPos pos;
	private final Consumer<ChunkSectionRegionConnectivityGraph<N>> completionConsumer;
	private final Runnable cancel;
	private AITaskExecutionContext context;
	private ChunkSectionRegionConnectivityGraph<N> output = null;
	private boolean finished = false;

	public ChunkSectionRegionLinkingAITask(final HierarchyInfo<?, N, C, O> info, final Supplier<@Nullable Optional<C>> precomputed, final Supplier<ChunkSectionRegions> regions, final Supplier<ShapeCache> factory, final BooleanSupplier aContinue, final ChunkSectionPos pos, final Consumer<ChunkSectionRegionConnectivityGraph<N>> consumer, final Runnable cancel) {
		this.info = info;
		this.precomputed = precomputed;
		this.regions = regions;
		cacheFactory = factory;
		shouldContinue = aContinue;
		this.pos = pos;
		completionConsumer = consumer;
		this.cancel = cancel;
	}

	public void setContext(AITaskExecutionContext context) {
		this.context = context;
	}

	@Override
	public boolean done() {
		return !shouldContinue.getAsBoolean() || output != null || !context.valid();
	}

	@Override
	public void runIteration() {
		if(context==null) {
			throw new RuntimeException();
		}
		if (shouldContinue.getAsBoolean()) {
			int missing = 0;
			final ShapeCache shapeCache = cacheFactory.get();
			for (int i = -1; i <= 1; i++) {
				for (int j = -1; j <= 1; j++) {
					if (!shapeCache.isOutOfHeightLimit(pos.getMinY() + j * 16)) {
						for (int k = -1; k <= 1; k++) {
							if (shapeCache.getRegions(pos.getMinX() + i * 16, pos.getMinY() + j * 16, pos.getMinZ() + k * 16, info, context) == null) {
								missing++;
							}
						}
					}
				}
			}
			if (missing != 0) {
				return;
			}
			final @Nullable Optional<C> optional = precomputed.get();
			final ChunkSectionRegions regions = this.regions.get();
			if (optional != null && regions != null) {
				output = info.link(optional.orElse(null), shapeCache, pos, regions, context);
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
		} else {
			cancel.run();
		}
		finished = true;
	}

	@Override
	public void cancel() {
		cancel.run();
	}
}
