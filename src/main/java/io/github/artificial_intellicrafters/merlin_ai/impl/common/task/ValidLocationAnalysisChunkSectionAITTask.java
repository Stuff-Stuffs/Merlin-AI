package io.github.artificial_intellicrafters.merlin_ai.impl.common.task;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITask;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.location_caching.ValidLocationSetImpl;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.function.Consumer;
import java.util.function.LongSupplier;

public class ValidLocationAnalysisChunkSectionAITTask implements AITask {
	private final long modCount;
	private final LongSupplier currentModCount;
	private final ValidLocationSetType<?> type;
	private final ChunkSectionPos pos;
	private final ShapeCache cache;
	private final Consumer<ValidLocationSet<?>> completionConsumer;
	private ValidLocationSetImpl<?> output = null;
	private boolean finished = false;

	public ValidLocationAnalysisChunkSectionAITTask(final long modCount, final LongSupplier currentModCount, final ValidLocationSetType<?> type, final ChunkSectionPos pos, final ShapeCache cache, final Consumer<ValidLocationSet<?>> completionConsumer) {
		this.modCount = modCount;
		this.currentModCount = currentModCount;
		this.type = type;
		this.pos = pos;
		this.cache = cache;
		this.completionConsumer = completionConsumer;
	}

	@Override
	public boolean done() {
		//done or chunk has changed since task submission
		return output != null || modCount != currentModCount.getAsLong();
	}

	@Override
	public void runIteration() {
		if (output == null) {
			output = new ValidLocationSetImpl<>(pos, cache, type);
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
