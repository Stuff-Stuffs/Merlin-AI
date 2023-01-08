package io.github.artificial_intellicrafters.merlin_ai.impl.common.location_caching.sets;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationClassifier;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutionContext;
import io.github.artificial_intellicrafters.merlin_ai.api.util.MathUtil;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.api.util.UniverseInfo;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.MerlinAI;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.PathingChunkSection;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.task.ValidLocationAnalysisChunkSectionAITTask;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.ChunkSectionPos;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;

public final class DenseValidLocationSetImpl<T> implements ValidLocationSet<T> {
	public static final ValidLocationAnalysisChunkSectionAITTask.ValidLocationSetFactory FACTORY = new ValidLocationAnalysisChunkSectionAITTask.ValidLocationSetFactory() {
		@Override
		public <T> int estimateSize(final int[] counts, final UniverseInfo<T> universeInfo) {
			final int universeSize = MathUtil.roundUpToPowerOf2(universeInfo.getUniverseSize());
			final int bitCount = MathUtil.roundUpToPowerOf2(Integer.highestOneBit(universeSize - 1));
			final int dataPerLong = Long.SIZE / bitCount;
			return (16 * 16 * 16 / dataPerLong) * 64;
		}

		@Override
		public <T> ValidLocationSet<T> build(final ValidLocationSetType<T> type, final long revision, final T[] data, final int[] counts, @Nullable final AITaskExecutionContext executionContext) {
			return new DenseValidLocationSetImpl<>(type, revision, data);
		}
	};
	private final int mask;
	private final int bitCount;
	private final UniverseInfo<T> universeInfo;
	private final long[] data;
	private final long revision;
	private final ValidLocationSetType<T> type;

	public DenseValidLocationSetImpl(final ValidLocationSetType<T> setType, final long revision, final T[] data) {
		this.revision = revision;
		universeInfo = setType.universeInfo();
		final int universeSize = MathUtil.roundUpToPowerOf2(universeInfo.getUniverseSize());
		mask = universeSize - 1;
		bitCount = Integer.highestOneBit(mask);
		if (bitCount > 64) {
			throw new RuntimeException();
		}
		this.data = new long[(16 * 16 * 16 * bitCount + 63) / 64];
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					final T val = data[ValidLocationAnalysisChunkSectionAITTask.dataIndex(x, y, z)];
					final int index = byteIndex(x & 15, y & 15, z & 15);
					final int subIndex = subIndex(x & 15, y & 15, z & 15);
					long datum = this.data[index];
					datum = datum | (long) (universeInfo.toInt(val) & mask) << subIndex;
					this.data[index] = datum;
				}
			}
		}
		type = setType;
	}

	@Override
	public ValidLocationSetType<T> type() {
		return type;
	}

	@Override
	public long revision() {
		return revision;
	}

	@Override
	public T get(final int x, final int y, final int z) {
		final int index = byteIndex(x & 15, y & 15, z & 15);
		final int subIndex = subIndex(x & 15, y & 15, z & 15);
		final long datum = data[index];
		return universeInfo.fromInt((int) ((datum >> subIndex) & mask));
	}

	@Override
	public Either<ValidLocationSet<T>, Pair<T[], int[]>> rebuild(final ChunkSectionPos sectionPos, final ShapeCache cache, final PathingChunkSection[] region, final long[] modCounts, final AITaskExecutionContext executionContext) {
		final BlockState[] updatedBlockStates = new BlockState[MerlinAI.PATHING_CHUNK_REMEMBERED_CHANGES];
		final short[] updatedPositions = new short[MerlinAI.PATHING_CHUNK_REMEMBERED_CHANGES];
		final UniverseInfo<T> universeInfo = type().universeInfo();
		final boolean[] modified = new boolean[1];
		final ValidLocationClassifier.RebuildConsumer<T> rebuildConsumer = (val, x, y, z) -> {
			final int index = byteIndex(x & 15, y & 15, z & 15);
			final int subIndex = subIndex(x & 15, y & 15, z & 15);
			long datum = data[index];
			final long l = (universeInfo.toInt(val) & mask);
			if (((datum >> subIndex) & mask) != l) {
				modified[0] = true;
				datum = (datum & ~((long) mask << subIndex)) | l << subIndex;
				data[index] = datum;
			}
		};
		final ValidLocationClassifier<T> classifier = type.classifier();
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				for (int k = -1; k <= 1; k++) {
					final int index = ValidLocationAnalysisChunkSectionAITTask.index(i, j, k);
					final long modCount = modCounts[index];
					final PathingChunkSection section = region[index];
					if (section != null) {
						final long diff = section.merlin_ai$getModCount() - modCount;
						if (diff > 0) {
							final boolean b = section.merlin_ai$copy_updates(modCount, updatedBlockStates, 0, updatedPositions, 0);
							assert b;
							classifier.rebuild(updatedBlockStates, updatedPositions, (int) diff, sectionPos.getSectionX(), sectionPos.getSectionY(), sectionPos.getSectionZ(), i, j, k, rebuildConsumer, cache, executionContext);
						}
					}
				}
			}
		}
		final T[] unpacked = (T[]) Array.newInstance(type.typeClass(), 16 * 16 * 16);
		final int[] counts = new int[universeInfo.getUniverseSize()];
		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				for (int k = 0; k < 16; k++) {
					final T val = get(i, j, k);
					unpacked[ValidLocationAnalysisChunkSectionAITTask.dataIndex(i, j, k)] = val;
					counts[universeInfo.toInt(val)]++;
				}
			}
		}
		if (modified[0]) {
			return Either.right(Pair.of(unpacked, counts));
		} else {
			return Either.left(this);
		}
	}

	private int subIndex(final int x, final int y, final int z) {
		return (x * 16 * 16 + y * 16 + z) * bitCount % 64;
	}

	private int byteIndex(final int x, final int y, final int z) {
		return (x * 16 * 16 + y * 16 + z) * bitCount / 64;
	}
}
