package io.github.artificial_intellicrafters.merlin_ai.impl.common.task;

import com.mojang.datafixers.util.Pair;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.HierarchyInfo;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITask;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ChunkSectionRegionsAnalysisAITask<T, N, C> implements AITask {
	private final HierarchyInfo<T, N, C, ?> info;
	private final Supplier<ShapeCache> cacheFactory;
	private final BooleanSupplier shouldContinue;
	private final ChunkSectionPos pos;
	private final HeightLimitView view;
	private final Consumer<Pair<ChunkSectionRegions, C>> completionConsumer;
	private final Runnable cancel;
	private Pair<ChunkSectionRegions, C> output = null;
	private boolean finished = false;

	public ChunkSectionRegionsAnalysisAITask(final HierarchyInfo<T, N, C, ?> info, final Supplier<ShapeCache> factory, final BooleanSupplier aContinue, final ChunkSectionPos pos, final HeightLimitView view, final Consumer<Pair<ChunkSectionRegions, C>> consumer, Runnable cancel) {
		this.info = info;
		cacheFactory = factory;
		shouldContinue = aContinue;
		this.pos = pos;
		this.view = view;
		completionConsumer = consumer;
		this.cancel = cancel;
	}

	@Override
	public int priority() {
		return 2;
	}

	@Override
	public boolean done() {
		return !shouldContinue.getAsBoolean() || output != null;
	}

	@Override
	public void runIteration() {
		if (shouldContinue.getAsBoolean()) {
			output = info.regionify(cacheFactory.get(), pos, info.validLocationSetType(), view);
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
