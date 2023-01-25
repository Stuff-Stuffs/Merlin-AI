package io.github.artificial_intellicrafters.merlin_ai.impl.common.task;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationClassifier;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITask;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutionContext;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.api.util.UniverseInfo;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.MerlinAI;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.PathingChunkSection;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.location_caching.sets.DenseValidLocationSetImpl;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.location_caching.sets.UniformValidLocationSetImpl;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ValidLocationAnalysisChunkSectionAITTask<T> implements AITask {
	private static final Logger LOGGER = LoggerFactory.getLogger(ValidLocationAnalysisChunkSectionAITTask.class);
	private static final ValidLocationSetFactory[] FACTORIES;
	private final BooleanSupplier shouldContinue;
	private final long[] currentModCounts;
	private final @Nullable ValidLocationSet<T> previous;
	private final ValidLocationSetType<T> type;
	private final ChunkSectionPos pos;
	private final Supplier<ShapeCache> cacheFactory;
	private final Consumer<ValidLocationSet<T>> completionConsumer;
	private final Runnable cancel;
	private AITaskExecutionContext context;
	private ValidLocationSet<T> output = null;
	private boolean finished = false;

	public ValidLocationAnalysisChunkSectionAITTask(final BooleanSupplier shouldContinue, final long[] currentModCounts, @Nullable final ValidLocationSet<T> previous, final ValidLocationSetType<T> type, final ChunkSectionPos pos, final Supplier<ShapeCache> cacheFactory, final Consumer<ValidLocationSet<T>> completionConsumer, final Runnable cancel) {
		this.shouldContinue = shouldContinue;
		this.currentModCounts = currentModCounts;
		this.previous = previous;
		this.type = type;
		this.pos = pos;
		this.cacheFactory = cacheFactory;
		this.completionConsumer = completionConsumer;
		this.cancel = cancel;
	}

	public void setContext(final AITaskExecutionContext context) {
		this.context = context;
	}

	@Override
	public boolean done() {
		//done or chunk has changed since task submission
		return output != null || !shouldContinue.getAsBoolean() || !context.valid();
	}

	@Override
	public void runIteration() {
		if (context == null) {
			throw new NullPointerException();
		}
		if (output == null) {
			final ShapeCache cache = cacheFactory.get();
			if (previous != null) {
				final PathingChunkSection[] region = new PathingChunkSection[27];
				for (int i = -1; i <= 1; i++) {
					for (int j = -1; j <= 1; j++) {
						for (int k = -1; k <= 1; k++) {
							if (!cache.isOutOfHeightLimit(pos.getMinY() + ChunkSection.SECTION_HEIGHT * j)) {
								final PathingChunkSection chunk = cache.getPathingChunk(
										pos.getMinX() + ChunkSection.SECTION_WIDTH * i,
										pos.getMinY() + ChunkSection.SECTION_HEIGHT * j,
										pos.getMinZ() + ChunkSection.SECTION_WIDTH * k
								);
								if (chunk == null) {
									return;
								}
								region[index(i, j, k)] = chunk;
							}
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
					final @Nullable Either<ValidLocationSet<T>, Pair<T[], int[]>> rebuild = previous.rebuild(pos, cache, region, currentModCounts, context);
					if (rebuild == null) {
						final Pair<T[], int[]> pair = dataAndCounts(cache);
						output = build(0, pair.getFirst(), pair.getSecond());
					} else {
						output = rebuild.map(Function.identity(), pair -> build(previous.revision() + 1, pair.getFirst(), pair.getSecond()));
					}
				} else {
					final Pair<T[], int[]> pair = dataAndCounts(cache);
					output = build(previous.revision() + 1, pair.getFirst(), pair.getSecond());
				}
			} else {
				final Pair<T[], int[]> pair = dataAndCounts(cache);
				output = build(0, pair.getFirst(), pair.getSecond());
			}
		}
	}

	private Pair<T[], int[]> dataAndCounts(final ShapeCache cache) {
		final int baseX = pos.getMinX();
		final int baseY = pos.getMinY();
		final int baseZ = pos.getMinZ();
		final T[] data = (T[]) Array.newInstance(type.typeClass(), 16 * 16 * 16);
		final UniverseInfo<T> universeInfo = type.universeInfo();
		final int[] counts = new int[universeInfo.getUniverseSize()];
		final ValidLocationClassifier<T> classifier = type.classifier();
		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				for (int k = 0; k < 16; k++) {
					final T val = classifier.classify(baseX + i, baseY + j, baseZ + k, cache, context);
					final int index = universeInfo.toInt(val);
					data[dataIndex(i, j, k)] = val;
					counts[index]++;
				}
			}
		}
		return Pair.of(data, counts);
	}

	public static int dataIndex(final int x, final int y, final int z) {
		return (((x & 15) * 16) + (y & 15)) * 16 + (z & 15);
	}

	private ValidLocationSet<T> build(final long revision, final T[] data, final int[] counts) {
		int minSize = Integer.MAX_VALUE;
		ValidLocationSetFactory best = FACTORIES[0];
		for (final ValidLocationSetFactory factory : FACTORIES) {
			final int estimateSize = factory.estimateSize(counts, type.universeInfo());
			if (estimateSize < minSize) {
				minSize = estimateSize;
				best = factory;
			}
		}
		return best.build(type, revision, data, counts, context);
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
		} else {
			cancel.run();
		}
		finished = true;
	}

	@Override
	public void cancel() {
		cancel.run();
	}

	@Override
	public Logger logger() {
		return LOGGER;
	}

	public interface ValidLocationSetFactory {
		<T> int estimateSize(int[] counts, UniverseInfo<T> universeInfo);

		<T> ValidLocationSet<T> build(ValidLocationSetType<T> type, long revision, T[] data, int[] counts, @Nullable AITaskExecutionContext executionContext);
	}

	static {
		FACTORIES = new ValidLocationSetFactory[2];
		FACTORIES[0] = DenseValidLocationSetImpl.FACTORY;
		FACTORIES[1] = UniformValidLocationSetImpl.FACTORY;
	}
}
