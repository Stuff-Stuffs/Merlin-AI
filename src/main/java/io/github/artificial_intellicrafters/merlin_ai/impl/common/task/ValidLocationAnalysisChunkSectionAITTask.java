package io.github.artificial_intellicrafters.merlin_ai.impl.common.task;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITask;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.MerlinAI;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.PathingChunkSection;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.location_caching.ValidLocationSetImpl;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ValidLocationAnalysisChunkSectionAITTask<T> implements AITask {
	private final BooleanSupplier shouldContinue;
	private final long[] currentModCounts;
	private final @Nullable ValidLocationSetImpl<T> previous;
	private final ValidLocationSetType<T> type;
	private final ChunkSectionPos pos;
	private final Supplier<ShapeCache> cacheFactory;
	private final Consumer<ValidLocationSet<T>> completionConsumer;
	private ValidLocationSetImpl<T> output = null;
	private boolean finished = false;

	public ValidLocationAnalysisChunkSectionAITTask(final BooleanSupplier shouldContinue, final long[] currentModCounts, @Nullable final ValidLocationSetImpl<T> previous, final ValidLocationSetType<T> type, final ChunkSectionPos pos, final Supplier<ShapeCache> cacheFactory, final Consumer<ValidLocationSet<T>> completionConsumer) {
		this.shouldContinue = shouldContinue;
		this.currentModCounts = currentModCounts;
		this.previous = previous;
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
		if (type.columnar()) {
			runIterationColumnar();
		} else {
			runIterationFull();
		}
	}

	private void runIterationFull() {
		if (output == null) {
			final ShapeCache cache = cacheFactory.get();
			if (previous != null) {
				final PathingChunkSection[] region = new PathingChunkSection[27];
				for (int i = -1; i <= 1; i++) {
					for (int j = -1; j <= 1; j++) {
						for (int k = -1; k <= 1; k++) {
							region[index(i, j, k)] = cache.getPathingChunk(
									pos.getMinX() + ChunkSection.SECTION_WIDTH * i,
									pos.getMinY() + ChunkSection.SECTION_HEIGHT * j,
									pos.getMinZ() + ChunkSection.SECTION_WIDTH * k
							);
						}
					}
				}
				boolean recoverable = true;
				int sum = 0;
				for (int i = 0; i < region.length; i++) {
					if (region[i] != null) {
						final long l = region[i].merlin_ai$getModCount() - currentModCounts[i];
						if (l >= MerlinAI.PATHING_CHUNK_REMEMBERED_CHANGES) {
							recoverable = false;
							break;
						}
						sum += l;
						if (sum >= MerlinAI.PATHING_CHUNK_CHANGES_BEFORE_RESET) {
							recoverable = false;
							break;
						}
					}
				}
				if (recoverable) {
					output = new ValidLocationSetImpl<>(pos, cache, previous, region, currentModCounts);
				} else {
					output = new ValidLocationSetImpl<>(pos, cache, type);
				}
			} else {
				output = new ValidLocationSetImpl<>(pos, cache, type);
			}
		}
	}

	private void runIterationColumnar() {
		if (output == null) {
			final ShapeCache cache = cacheFactory.get();
			if (previous != null) {
				final PathingChunkSection[] region = new PathingChunkSection[3];
				for (int j = -1; j <= 1; j++) {
					region[indexColumnar(j)] = cache.getPathingChunk(
							pos.getMinX(),
							pos.getMinY() + ChunkSection.SECTION_HEIGHT * j,
							pos.getMinZ()
					);
				}
				boolean recoverable = true;
				int sum = 0;
				for (int i = 0; i < region.length; i++) {
					if (region[i] != null) {
						final long l = region[i].merlin_ai$getModCount() - currentModCounts[i];
						if (l >= MerlinAI.PATHING_CHUNK_REMEMBERED_CHANGES) {
							recoverable = false;
							break;
						}
						sum += l;
						if (sum >= MerlinAI.PATHING_CHUNK_CHANGES_BEFORE_RESET) {
							recoverable = false;
							break;
						}
					}
				}
				if (recoverable) {
					output = new ValidLocationSetImpl<>(pos, cache, previous, region, currentModCounts);
				} else {
					output = new ValidLocationSetImpl<>(pos, cache, type);
				}
			} else {
				output = new ValidLocationSetImpl<>(pos, cache, type);
			}
		}
	}

	public static int index(final int x, final int y, final int z) {
		return ((x + 1) * 3 + y + 1) * 3 + z + 1;
	}

	public static int indexColumnar(final int y) {
		return y + 1;
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
