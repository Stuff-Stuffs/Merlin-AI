package io.github.artificial_intellicrafters.merlin_ai.impl.common.location_caching;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.UniverseInfo;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationClassifier;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.MerlinAI;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.PathingChunkSection;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.task.ValidLocationAnalysisChunkSectionAITTask;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.Arrays;

public final class ValidLocationSetImpl<T> implements ValidLocationSet<T> {
	private final int mask;
	private final int bitCount;
	private final UniverseInfo<T> universeInfo;
	private final long[] data;
	private final ValidLocationSetType<T> type;

	public ValidLocationSetImpl(final ChunkSectionPos sectionPos, final ShapeCache cache, final ValidLocationSetType<T> setType) {
		final ValidLocationClassifier<T> classifier = setType.classifier();
		universeInfo = setType.universeInfo();
		int universeSize = universeInfo.getUniverseSize();
		if ((universeSize & (universeSize - 1)) != 0) {
			universeSize = universeSize | universeSize >> 1;
			universeSize = universeSize | universeSize >> 4;
			universeSize = universeSize | universeSize >> 8;
			universeSize = universeSize | universeSize >> 16;
			universeSize = universeSize + 1;
		}
		mask = universeSize - 1;
		bitCount = Integer.highestOneBit(mask);
		if(bitCount>64) {
			throw new RuntimeException();
		}

		data = new long[(16 * 16 * 16 * bitCount + 63) / 64];

		final int baseX = sectionPos.getMinX();
		final int baseY = sectionPos.getMinY();
		final int baseZ = sectionPos.getMinZ();
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					final T val = classifier.classify(baseX + x, baseY + y, baseZ + z, cache);
					final int index = byteIndex(x & 15, y & 15, z & 15);
					final int subIndex = subIndex(x & 15, y & 15, z & 15);
					long datum = data[index];
					datum = datum | (long) (universeInfo.toInt(val) & mask) << subIndex;
					data[index] = datum;
				}
			}
		}
		type = setType;
	}

	public ValidLocationSetImpl(final ChunkSectionPos sectionPos, final ShapeCache cache, final ValidLocationSetImpl<T> previous, final PathingChunkSection[] region, final long[] modCounts) {
		mask = previous.mask;
		bitCount = previous.bitCount;
		if(bitCount>64) {
			throw new RuntimeException();
		}
		universeInfo = previous.universeInfo;
		data = Arrays.copyOf(previous.data, previous.data.length);
		type = previous.type();
		final BlockState[] updatedBlockStates = new BlockState[MerlinAI.PATHING_CHUNK_REMEMBERED_CHANGES];
		final short[] updatedPositions = new short[MerlinAI.PATHING_CHUNK_REMEMBERED_CHANGES];
		final UniverseInfo<T> universeInfo = type().universeInfo();
		final ValidLocationClassifier.RebuildConsumer<T> rebuildConsumer = (val, x, y, z) -> {
			final int index = byteIndex(x & 15, y & 15, z & 15);
			final int subIndex = subIndex(x & 15, y & 15, z & 15);
			long datum = data[index];
			datum = (datum & ~((long) mask << subIndex)) | (long) (universeInfo.toInt(val) & mask) << subIndex;
			data[index] = datum;
		};
		final ValidLocationClassifier<T> classifier = type.classifier();
		if (type.columnar()) {
			for (int j = -1; j <= 1; j++) {
				final int index = ValidLocationAnalysisChunkSectionAITTask.indexColumnar(j);
				final long modCount = modCounts[index];
				final PathingChunkSection section = region[index];
				final long diff = section.merlin_ai$getModCount() - modCount;
				if (diff > 0) {
					final boolean b = section.merlin_ai$copy_updates(modCount, updatedBlockStates, 0, updatedPositions, 0);
					assert b;
					classifier.rebuild(updatedBlockStates, updatedPositions, (int) diff, sectionPos.getSectionX(), sectionPos.getSectionY(), sectionPos.getSectionZ(), 0, j, 0, rebuildConsumer, cache);
				}
			}
		} else {
			for (int i = -1; i <= 1; i++) {
				for (int j = -1; j <= 1; j++) {
					for (int k = -1; k <= 1; k++) {
						final int index = ValidLocationAnalysisChunkSectionAITTask.index(i, j, k);
						final long modCount = modCounts[index];
						final PathingChunkSection section = region[index];
						final long diff = section.merlin_ai$getModCount() - modCount;
						if (diff > 0) {
							final boolean b = section.merlin_ai$copy_updates(modCount, updatedBlockStates, 0, updatedPositions, 0);
							assert b;
							classifier.rebuild(updatedBlockStates, updatedPositions, (int) diff, sectionPos.getSectionX(), sectionPos.getSectionY(), sectionPos.getSectionZ(), i, j, k, rebuildConsumer, cache);
						}
					}
				}
			}
		}
	}

	@Override
	public ValidLocationSetType<T> type() {
		return type;
	}

	@Override
	public T get(final int x, final int y, final int z) {
		final int index = byteIndex(x & 15, y & 15, z & 15);
		final int subIndex = subIndex(x & 15, y & 15, z & 15);
		final long datum = data[index];
		return universeInfo.fromInt((int) ((datum >> subIndex) & mask));
	}

	private int subIndex(final int x, final int y, final int z) {
		return (x * 16 * 16 + y * 16 + z) * bitCount % 64;
	}

	private int byteIndex(final int x, final int y, final int z) {
		return (x * 16 * 16 + y * 16 + z) * bitCount / 64;
	}
}
