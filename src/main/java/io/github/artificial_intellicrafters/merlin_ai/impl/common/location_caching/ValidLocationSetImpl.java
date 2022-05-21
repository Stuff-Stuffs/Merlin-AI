package io.github.artificial_intellicrafters.merlin_ai.impl.common.location_caching;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.UniverseInfo;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationClassifier;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.api.util.SubChunkSectionUtil;

public final class ValidLocationSetImpl<T> implements ValidLocationSet<T> {
	private final int mask;
	private final int bitCount;
	private final UniverseInfo<T> universeInfo;
	private final long[] data;
	private final ValidLocationSetType<T> type;

	public ValidLocationSetImpl(final long subSectionPos, final ShapeCache cache, final ValidLocationSetType<T> setType) {
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

		data = new long[(SubChunkSectionUtil.SUB_SECTION_SIZE * SubChunkSectionUtil.SUB_SECTION_SIZE * SubChunkSectionUtil.SUB_SECTION_SIZE * bitCount + 63) / 64];

		final int baseX = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackX(subSectionPos));
		final int baseY = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackY(subSectionPos));
		final int baseZ = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackZ(subSectionPos));
		for (int x = 0; x < SubChunkSectionUtil.SUB_SECTION_SIZE; x++) {
			for (int y = 0; y < SubChunkSectionUtil.SUB_SECTION_SIZE; y++) {
				for (int z = 0; z < SubChunkSectionUtil.SUB_SECTION_SIZE; z++) {
					final T val = classifier.validate(baseX + x, baseY + y, baseZ + z, cache);
					final int index = byteIndex(x & 7, y & 7, z & 7);
					final int subIndex = subIndex(x & 7, y & 7, z & 7);
					long datum = data[index];
					datum = datum | (long) (universeInfo.toInt(val) & mask) << subIndex;
					data[index] = datum;
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
	public T get(final int x, final int y, final int z) {
		final int index = byteIndex(x & 7, y & 7, z & 7);
		final int subIndex = subIndex(x & 7, y & 7, z & 7);
		final long datum = data[index];
		return universeInfo.fromInt((int) ((datum >> subIndex) & mask));
	}

	private int subIndex(final int x, final int y, final int z) {
		return (x * SubChunkSectionUtil.SUB_SECTION_SIZE * SubChunkSectionUtil.SUB_SECTION_SIZE + y * SubChunkSectionUtil.SUB_SECTION_SIZE + z) * bitCount % 64;
	}

	private int byteIndex(final int x, final int y, final int z) {
		return (x * SubChunkSectionUtil.SUB_SECTION_SIZE * SubChunkSectionUtil.SUB_SECTION_SIZE + y * SubChunkSectionUtil.SUB_SECTION_SIZE + z) * bitCount / 64;
	}
}
