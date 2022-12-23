package io.github.artificial_intellicrafters.merlin_ai.impl.common.location_caching;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationClassifier;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.api.util.UniverseInfo;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.MerlinAI;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.PathingChunkSection;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.task.ValidLocationAnalysisChunkSectionAITTask;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;

public final class DenseValidLocationSetImpl<T> implements ValidLocationSet<T> {
	private final int mask;
	private final int bitCount;
	private final int perLong;
	private final UniverseInfo<T> universeInfo;
	private final long[] data;
	private final long revision;
	private final ValidLocationSetType<T> type;

	public DenseValidLocationSetImpl(final T[] data, final ValidLocationSetType<T> setType, final long revision) {
		this.revision = revision;
		universeInfo = setType.universeInfo();
		int universeSize = universeInfo.getUniverseSize();
		if (universeSize == 0) {
			throw new RuntimeException();
		}
		if ((universeSize & (universeSize - 1)) != 0) {
			universeSize = MathHelper.smallestEncompassingPowerOfTwo(universeSize);
		}
		mask = universeSize - 1;
		bitCount = Integer.highestOneBit(mask);
		if (bitCount > 64) {
			throw new RuntimeException();
		}
		type = setType;
		this.data = new long[(16 * 16 * 16 * bitCount + 63) / 64];
		perLong = 64 / bitCount;
		for (int i = 0; i < this.data.length; i++) {
			long d = 0;
			for (int j = 0; j < perLong && j * perLong < data.length; j++) {
				d |= (long) (universeInfo.toInt(data[j + i * perLong]) & mask) << (j * bitCount);
			}
			this.data[i] = d;
		}
	}

	private DenseValidLocationSetImpl(final int mask, final int bitCount, final int perLong, final UniverseInfo<T> universeInfo, final long[] data, final long revision, final ValidLocationSetType<T> type) {
		this.mask = mask;
		this.bitCount = bitCount;
		this.perLong = perLong;
		this.universeInfo = universeInfo;
		this.data = data;
		this.revision = revision;
		this.type = type;
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
		final int index = SparseValidLocationSetImpl.packLocal(x, y, z);
		final int i = index / perLong;
		final long datum = data[i];
		final long selected = (datum >>> (index % perLong * bitCount)) & mask;
		return universeInfo.fromInt((int) selected);
	}

	@Override
	public long size() {
		return data.length * (long) Long.BYTES;
	}

	@Override
	public Either<ValidLocationSet<T>, Pair<T[], int[]>> rebuild(final ChunkSectionPos sectionPos, final ShapeCache cache, final PathingChunkSection[] region, final long[] modCounts) {
		final BlockState[] updatedBlockStates = new BlockState[MerlinAI.PATHING_CHUNK_REMEMBERED_CHANGES];
		final short[] updatedPositions = new short[MerlinAI.PATHING_CHUNK_REMEMBERED_CHANGES];
		final UniverseInfo<T> universeInfo = type().universeInfo();
		final boolean[] modified = new boolean[1];
		final ValidLocationClassifier.RebuildConsumer<T> rebuildConsumer = (val, x, y, z) -> {
			final int index = SparseValidLocationSetImpl.packLocal(x, y, z);
			final int i = index / perLong;
			final int shift = index % perLong * bitCount;
			long datum = data[i];
			final long l = (universeInfo.toInt(val) & mask);
			if (((datum >> shift) & mask) != l) {
				modified[0] = true;
				datum = (datum & ~((long) mask << shift)) | l << shift;
				data[index] = datum;
			}
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
		if (modified[0]) {
			return Either.left(new DenseValidLocationSetImpl<>(mask, bitCount, perLong, universeInfo, data, revision + 1, type));
		} else {
			return Either.left(this);
		}
	}

	public static int bits(final UniverseInfo<?> info) {
		int universeSize = info.getUniverseSize();
		if (universeSize == 0) {
			throw new RuntimeException();
		}
		if ((universeSize & (universeSize - 1)) != 0) {
			universeSize = MathHelper.smallestEncompassingPowerOfTwo(universeSize);
		}
		int mask = universeSize - 1;
		int bitCount = Integer.highestOneBit(mask);
		if (bitCount > 64) {
			throw new RuntimeException();
		}
		return (16 * 16 * 16 * bitCount + 63) / 64;
	}
}
