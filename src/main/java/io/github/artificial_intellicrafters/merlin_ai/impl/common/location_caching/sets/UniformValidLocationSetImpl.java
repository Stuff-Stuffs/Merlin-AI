package io.github.artificial_intellicrafters.merlin_ai.impl.common.location_caching.sets;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationClassifier;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutionContext;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.api.util.UniverseInfo;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.MerlinAI;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.PathingChunkSection;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.task.ValidLocationAnalysisChunkSectionAITTask;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.ChunkSectionPos;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.Arrays;

public final class UniformValidLocationSetImpl<T> implements ValidLocationSet<T> {
	public static final ValidLocationAnalysisChunkSectionAITTask.ValidLocationSetFactory FACTORY = new ValidLocationAnalysisChunkSectionAITTask.ValidLocationSetFactory() {
		@Override
		public <T> int estimateSize(final int[] counts, final UniverseInfo<T> universeInfo) {
			for (final int count : counts) {
				if (count == 16 * 16 * 16) {
					return 64;
				}
			}
			return Integer.MAX_VALUE;
		}

		@Override
		public <T> ValidLocationSet<T> build(final ValidLocationSetType<T> type, final long revision, final T[] data, final int[] counts, @Nullable final AITaskExecutionContext executionContext) {
			return new UniformValidLocationSetImpl<>(type, revision, data[0]);
		}
	};
	private final ValidLocationSetType<T> type;
	private final long revision;
	private final T value;

	public UniformValidLocationSetImpl(final ValidLocationSetType<T> type, final long revision, final T value) {
		this.type = type;
		this.revision = revision;
		this.value = value;
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
		return value;
	}

	@Override
	public Either<ValidLocationSet<T>, Pair<T[], int[]>> rebuild(final ChunkSectionPos sectionPos, final ShapeCache cache, final PathingChunkSection[] region, final long[] modCounts, final AITaskExecutionContext executionContext) {
		final BlockState[] updatedBlockStates = new BlockState[MerlinAI.PATHING_CHUNK_REMEMBERED_CHANGES];
		final short[] updatedPositions = new short[MerlinAI.PATHING_CHUNK_REMEMBERED_CHANGES];
		final short[] updatedPositionPacked = new short[MerlinAI.PATHING_CHUNK_CHANGES_BEFORE_RESET];
		final T[] updatedValues = (T[]) Array.newInstance(type.typeClass(), MerlinAI.PATHING_CHUNK_REMEMBERED_CHANGES);
		final int[] indexHolder = new int[]{0};
		final ValidLocationClassifier.RebuildConsumer<T> rebuildConsumer = (val, x, y, z) -> {
			if (val != value) {
				updatedPositionPacked[indexHolder[0]] = PathingChunkSection.packLocal(x, y, z);
				updatedValues[indexHolder[0]++] = val;
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
		if (indexHolder[0] == 0) {
			return Either.left(this);
		} else {
			final T[] data = (T[]) Array.newInstance(type.typeClass(), 16 * 16 * 16);
			Arrays.fill(data, value);
			final UniverseInfo<T> universeInfo = type().universeInfo();
			final int[] counts = new int[universeInfo.getUniverseSize()];
			counts[universeInfo.toInt(value)] = 16 * 16 * 16 - indexHolder[0];
			for (int i = 0; i < indexHolder[0]; i++) {
				final short packed = updatedPositionPacked[i];
				final int x = PathingChunkSection.unpackLocalX(packed);
				final int y = PathingChunkSection.unpackLocalY(packed);
				final int z = PathingChunkSection.unpackLocalZ(packed);
				final T value = updatedValues[i];
				counts[universeInfo.toInt(value)]++;
				data[ValidLocationAnalysisChunkSectionAITTask.dataIndex(x, y, z)] = value;
			}
			return Either.right(Pair.of(data, counts));
		}
	}
}
