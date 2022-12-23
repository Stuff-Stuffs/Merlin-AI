package io.github.artificial_intellicrafters.merlin_ai.impl.common.task;

import com.mojang.datafixers.util.Pair;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITask;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.api.util.UniverseInfo;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.MerlinAI;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.PathingChunkSection;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.location_caching.DenseValidLocationSetImpl;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.location_caching.SparseValidLocationSetImpl;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ValidLocationAnalysisChunkSectionAITTask<T> implements AITask {
	private final BooleanSupplier shouldContinue;
	private final long[] currentModCounts;
	private final long prevRevision;
	private @Nullable ValidLocationSet<T> previous;
	private final ValidLocationSetType<T> type;
	private final ChunkSectionPos pos;
	private final Supplier<ShapeCache> cacheFactory;
	private final Consumer<ValidLocationSet<T>> completionConsumer;
	private ValidLocationSet<T> output = null;
	private boolean finished = false;

	public ValidLocationAnalysisChunkSectionAITTask(final BooleanSupplier shouldContinue, final long[] currentModCounts, @Nullable final ValidLocationSet<T> previous, final ValidLocationSetType<T> type, final ChunkSectionPos pos, final Supplier<ShapeCache> cacheFactory, final Consumer<ValidLocationSet<T>> completionConsumer) {
		this.shouldContinue = shouldContinue;
		this.currentModCounts = currentModCounts;
		this.previous = previous;
		this.type = type;
		this.pos = pos;
		this.cacheFactory = cacheFactory;
		this.completionConsumer = completionConsumer;
		if (previous != null) {
			prevRevision = previous.revision();
		} else {
			prevRevision = -1;
		}
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
					output = previous.rebuild(pos, cache, region, currentModCounts).map(Function.identity(), this::create);
				}
			} else {
				final Pair<T[], int[]> pair = dataCube(cache);
				output = create(pair);
			}
		}
	}

	private Pair<T[], int[]> dataCube(final ShapeCache cache) {
		final var classifier = type.classifier();
		final int baseX = pos.getMinX();
		final int baseY = pos.getMinY();
		final int baseZ = pos.getMinZ();
		final Optional<T> fill = classifier.fill(pos, cache);
		final T[] data = (T[]) Array.newInstance(type.typeClass(), 16 * 16 * 16);
		final UniverseInfo<T> info = type.universeInfo();
		final int[] counts = new int[info.getUniverseSize()];
		if (fill.isPresent()) {
			final T defaultVal = fill.get();
			Arrays.fill(data, defaultVal);
			for (int x = 0; x < 16; x++) {
				for (int y = 0; y < 16; y++) {
					for (int z = 0; z < 16; z++) {
						final T val = classifier.postProcess(defaultVal, baseX + x, baseY + y, baseZ + z, cache);
						if (val != defaultVal) {
							counts[info.toInt(defaultVal)]--;
							counts[info.toInt(val)]++;
							data[SparseValidLocationSetImpl.packLocal(x, y, z)] = val;
						}
					}
				}
			}
		} else {
			for (int x = 0; x < 16; x++) {
				for (int y = 0; y < 16; y++) {
					for (int z = 0; z < 16; z++) {
						counts[info.toInt(data[SparseValidLocationSetImpl.packLocal(x, y, z)] = classifier.classify(baseX + x, baseY + y, baseZ + z, cache))]++;
					}
				}
			}
		}
		return Pair.of(data, counts);
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
					output = previous.rebuild(pos, cache, region, currentModCounts).map(Function.identity(), this::create);
				}
			} else {
				final Pair<T[], int[]> pair = dataCube(cache);
				output = create(pair);
			}
		}
	}

	private ValidLocationSet<T> create(Pair<T[], int[]> pair) {
		int[] counts = pair.getSecond();
		if (SparseValidLocationSetImpl.bits(counts, type.universeInfo()) < DenseValidLocationSetImpl.bits(type.universeInfo())) {
			return new SparseValidLocationSetImpl<>(prevRevision + 1, type, pair.getFirst(), counts);
		} else {
			return new DenseValidLocationSetImpl<>(pair.getFirst(), type, prevRevision + 1);
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
