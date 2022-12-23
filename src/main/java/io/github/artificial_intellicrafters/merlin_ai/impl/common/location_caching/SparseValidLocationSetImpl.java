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
import it.unimi.dsi.fastutil.shorts.*;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;

import java.lang.reflect.Array;
import java.util.Arrays;

//TODO rebuild properly
public class SparseValidLocationSetImpl<T> implements ValidLocationSet<T> {
	private static final int PACKED_LOC_BITS = 12;
	private static final long BIT_MASK_12 = 0xFFF;
	private final int count;
	private final int[] counts;
	private final long[] contained;
	private final long[] types;
	private final long revision;
	private final int typesPerLong;
	private final T fallback;
	private final int shift;
	private final int mask;
	private final ValidLocationSetType<T> type;
	private final UniverseInfo<T> universeInfo;

	//fixme this could be a lot more efficient by not using an intermediate ShortList
	public SparseValidLocationSetImpl(final long revision, final ValidLocationSetType<T> type, final T[] data, final int[] counts) {
		this.counts = counts;
		this.revision = revision;
		this.type = type;
		universeInfo = type.universeInfo();
		final T mostOften = mostOften(universeInfo, counts);
		fallback = mostOften;
		final ShortList containedUnpacked = new ShortArrayList(16 * 16 * 16);
		for (int i = 0; i < data.length; i++) {
			if (data[i] != mostOften) {
				containedUnpacked.add((short) i);
			}
		}
		count = containedUnpacked.size();
		contained = new long[(count + 4) / 5];
		final int universeSize = MathHelper.smallestEncompassingPowerOfTwo(universeInfo.getUniverseSize());
		shift = Integer.numberOfTrailingZeros(universeSize);
		mask = universeSize - 1;
		typesPerLong = Long.SIZE / universeSize;
		final int typesLength = (count + typesPerLong - 1) / typesPerLong;
		types = new long[typesLength];
		{
			int i = 0;
			final ShortIterator iter = containedUnpacked.iterator();
			int subIndex = 0;
			long progress = 0;
			while (iter.hasNext()) {
				progress |= ((long) universeInfo.toInt(data[iter.nextShort()]) << (subIndex * shift)) & 0xFFFF_FFFFL;
				subIndex++;
				if (subIndex == typesPerLong) {
					subIndex = 0;
					types[i++] = progress;
					progress = 0;
				}
			}
			if (subIndex != 0) {
				types[i++] = progress;
			}
		}
		{
			int i = 0;
			while ((i + 1) * 5 < count) {
				final short a = containedUnpacked.getShort(i * 5 + 0);
				final short b = containedUnpacked.getShort(i * 5 + 1);
				final short c = containedUnpacked.getShort(i * 5 + 2);
				final short d = containedUnpacked.getShort(i * 5 + 3);
				final short e = containedUnpacked.getShort(i * 5 + 4);
				contained[i] = pack(a, b, c, d, e);
				i++;
			}
			final int off = count % 5;
			if (off != 0) {
				final short a = containedUnpacked.getShort(i * 5 + 0);
				final short b = off > 1 ? containedUnpacked.getShort(i * 5 + 1) : (short) 0;
				final short c = off > 2 ? containedUnpacked.getShort(i * 5 + 2) : (short) 0;
				final short d = off > 3 ? containedUnpacked.getShort(i * 5 + 3) : (short) 0;
				contained[i] = pack(a, b, c, d, (short) 0);
			}
		}
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
		final short local = packLocal(x, y, z);
		int low = 0;
		int high = count - 1;
		while (low <= high) {
			final int mid = (low + high) >>> 1;
			final int index = mid / 5;
			final int subIndex = mid % 5;
			final short key = unpack(contained[index], subIndex);
			if (key < local) {
				low = mid + 1;
			} else if (key > local) {
				high = mid - 1;
			} else {
				return getType(mid);
			}
		}
		return fallback;
	}

	@Override
	public long size() {
		return contained.length + types.length;
	}

	@Override
	public Either<ValidLocationSet<T>, Pair<T[], int[]>> rebuild(final ChunkSectionPos sectionPos, final ShapeCache cache, final PathingChunkSection[] region, final long[] modCounts) {
		final BlockState[] updatedBlockStates = new BlockState[MerlinAI.PATHING_CHUNK_REMEMBERED_CHANGES];
		final short[] updatedPositions = new short[MerlinAI.PATHING_CHUNK_REMEMBERED_CHANGES];
		final boolean[] modified = new boolean[1];
		final int[] modifiedCounts = Arrays.copyOf(counts, counts.length);
		final Short2IntMap modifiedPositions = new Short2IntOpenHashMap();
		final ValidLocationClassifier.RebuildConsumer<T> rebuildConsumer = (val, x, y, z) -> {
			final T prev = get(x, y, z);
			if (val != prev) {
				modifiedCounts[universeInfo.toInt(prev)]--;
				final int i = universeInfo.toInt(val);
				modifiedCounts[i]++;
				modifiedPositions.put(packLocal(x, y, z), i);
				modified[0] = true;
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
		if (!modified[0]) {
			return Either.left(this);
		}
		final T[] dataCube = (T[]) Array.newInstance(type.typeClass(), 16 * 16 * 16);
		Arrays.fill(dataCube, fallback);
		for (int i = 0; i < count; i++) {
			dataCube[getIndex(i)] = getType(i);
		}
		for (final Short2IntMap.Entry entry : modifiedPositions.short2IntEntrySet()) {
			dataCube[entry.getShortKey()] = universeInfo.fromInt(entry.getIntValue());
		}
		return Either.right(Pair.of(dataCube, modifiedCounts));
	}

	private int getIndex(final int index) {
		final int lIndex = index / 5;
		final int sub = index % 5;
		return (int) ((contained[lIndex] >>> (PACKED_LOC_BITS * sub)) & BIT_MASK_12);
	}

	private T getType(final int index) {
		final int lIndex = index / typesPerLong;
		final int sub = index % typesPerLong;
		return universeInfo.fromInt((int) (types[lIndex] >>> (shift * sub)) & mask);
	}

	private static short unpack(final long packed, final int index) {
		return (short) ((packed >>> (PACKED_LOC_BITS * index)) & BIT_MASK_12);
	}

	private static long pack(final short a, final short b, final short c, final short d, final short e) {
		return ((a & BIT_MASK_12) << (0 * PACKED_LOC_BITS)) | ((b & BIT_MASK_12) << (1 * PACKED_LOC_BITS)) | ((c & BIT_MASK_12) << (2 * PACKED_LOC_BITS)) | ((d & BIT_MASK_12) << (3 * PACKED_LOC_BITS)) | ((e & BIT_MASK_12) << (4 * 12));
	}

	private static <T> T mostOften(final UniverseInfo<T> universeInfo, final int[] counts) {
		if (universeInfo.getUniverseSize() != counts.length || universeInfo.getUniverseSize() == 0) {
			throw new RuntimeException();
		}
		int mostOftenCount = -1;
		int mostOftenIndex = -1;
		for (int i = 0; i < counts.length; i++) {
			if (counts[i] > mostOftenCount) {
				mostOftenCount = counts[i];
				mostOftenIndex = i;
			}
		}
		if (mostOftenIndex == -1) {
			throw new RuntimeException();
		}
		return universeInfo.fromInt(mostOftenIndex);
	}

	public static int bits(final int[] counts, final UniverseInfo<?> info) {
		int nonMostOften = 0;
		int mostOftenCount = -1;
		for (final int count : counts) {
			if (count > mostOftenCount) {
				nonMostOften += mostOftenCount;
				mostOftenCount = count;
			} else {
				nonMostOften += count;
			}
		}
		return MathHelper.roundUpToMultiple(MathHelper.ceil(5.33333333333 * nonMostOften), 64) + MathHelper.smallestEncompassingPowerOfTwo(info.getUniverseSize()) * nonMostOften;
	}

	public static short packLocal(final int x, final int y, final int z) {
		final int lx = ChunkSectionPos.getLocalCoord(x);
		final int ly = ChunkSectionPos.getLocalCoord(y);
		final int lz = ChunkSectionPos.getLocalCoord(z);
		return (short) (lx << 8 | lz << 4 | ly << 0);
	}
}
