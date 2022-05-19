package io.github.artificial_intellicrafters.merlin_ai.impl.common.task;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITask;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.location_caching.ValidLocationSetImpl;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class ValidLocationAnalysisChunkSectionAITTask implements AITask {
	private final BooleanSupplier shouldContinue;
	private final ValidLocationSetType<?> type;
	private final ChunkSectionPos pos;
	private final Supplier<ShapeCache> cacheFactory;
	private final Consumer<ValidLocationSet<?>> completionConsumer;
	private ValidLocationSetImpl<?> output = null;
	private boolean finished = false;

	public ValidLocationAnalysisChunkSectionAITTask(final BooleanSupplier shouldContinue, final ValidLocationSetType<?> type, final ChunkSectionPos pos, final Supplier<ShapeCache> cacheFactory, final Consumer<ValidLocationSet<?>> completionConsumer) {
		this.shouldContinue = shouldContinue;
		this.type = type;
		this.pos = pos;
		this.cacheFactory = cacheFactory;
		this.completionConsumer = completionConsumer;
	}

	@Override
	public boolean done() {
		//done or chunk has changed since task submission
		return output != null || !shouldContinue.getAsBoolean();
	}

	@Override
	public void runIteration() {
		if (output == null) {
			output = new ValidLocationSetImpl<>(pos, cacheFactory.get(), type);
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

	@Override
	public String toString() {
		return "ValidLocationAnalysisChunkSectionAITTask{" + "pos=" + pos + '}';
	}
}
