package io.github.artificial_intellicrafters.merlin_ai.impl.common.hierarchy;

import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegion;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegions;
import it.unimi.dsi.fastutil.shorts.ShortArrays;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ChunkSectionRegionsImpl implements ChunkSectionRegions {
	private static final long X_BITS = 22;
	private static final long Y_BITS = 8;
	private static final long Z_BITS = X_BITS;
	private static final long CUSTOM_BITS = Long.SIZE - X_BITS - Y_BITS - Z_BITS;
	private static final long X_SHIFT = Long.SIZE - X_BITS;
	private static final long Y_SHIFT = X_SHIFT - Y_BITS;
	private static final long Z_SHIFT = Y_SHIFT - Z_BITS;
	private static final long CUSTOM_SHIFT = 0;
	private static final long X_MASK = ((1L << X_BITS) - 1L) << X_SHIFT;
	private static final long Y_MASK = ((1L << Y_BITS) - 1L) << Y_SHIFT;
	private static final long Z_MASK = ((1L << Z_BITS) - 1L) << Z_SHIFT;
	private static final long CUSTOM_MASK = ((1L << CUSTOM_BITS) - 1L) << CUSTOM_SHIFT;
	private final long prefix;
	private final ChunkSectionRegion[] regions;

	public ChunkSectionRegionsImpl(final long prefix, final ChunkSectionRegion[] regions) {
		this.prefix = prefix;
		this.regions = regions;
	}

	@Override
	public @Nullable ChunkSectionRegion query(final short pos) {
		for (final ChunkSectionRegion region : regions) {
			if (region.contains(pos)) {
				return region;
			}
		}
		return null;
	}

	@Override
	public @Nullable ChunkSectionRegion byId(final long id) {
		if ((id & ~CUSTOM_SHIFT) == prefix) {
			final int i = unpackCustomPosCompact(id);
			if (i < regions.length) {
				return regions[i];
			} else {
				return null;
			}
		}
		throw new RuntimeException();
	}

	public static long packChunkSectionPosCompact(final ChunkSectionPos pos, final HeightLimitView view) {
		final int yIndex = view.sectionCoordToIndex(pos.getSectionY());
		if (yIndex > 255 || yIndex < 0) {
			throw new RuntimeException();
		}
		return ((long) pos.getSectionX() & 0xFFFF_FFFFL) << X_SHIFT | ((long) yIndex) << Y_SHIFT | ((long) pos.getSectionZ() & 0xFFFF_FFFFL) << Z_SHIFT;
	}

	public static ChunkSectionPos unpackChunkSectionPosCompact(final long packed, final HeightLimitView view) {
		final int x = (int) ((packed & X_MASK) >>> X_SHIFT);
		final int yIndex = (int) ((packed & Y_MASK) >>> Y_SHIFT);
		final int z = (int) ((packed & Z_MASK) >>> Z_SHIFT);
		final int y = view.sectionIndexToCoord(yIndex);
		return ChunkSectionPos.from(x, y, z);
	}

	public static int unpackCustomPosCompact(final long packed) {
		return (int) ((packed & CUSTOM_MASK) >>> CUSTOM_SHIFT);
	}

	public static final class BuilderImpl implements Builder {
		private static final int SIZE_THRESHOLD = 10;
		private final ShortSet set = new ShortOpenHashSet();
		private final List<PartialRegion> partialRegions = new ArrayList<>(32);
		private final long pos;

		public BuilderImpl(final ChunkSectionPos pos, final HeightLimitView view) {
			this.pos = packChunkSectionPosCompact(pos, view);
		}

		@Override
		public RegionKey newRegion() {
			partialRegions.add(new PartialRegion());
			return new RegionKeyImpl(this, partialRegions.size() - 1);
		}

		@Override
		public boolean contains(final short pos) {
			return set.contains(pos);
		}

		@Override
		public void expand(final RegionKey key, final short pos) {
			if (contains(pos)) {
				throw new RuntimeException();
			} else {
				set.add(pos);
				final RegionKeyImpl regionKey = (RegionKeyImpl) key;
				if (regionKey.parent != this) {
					throw new RuntimeException();
				}
				partialRegions.get(regionKey.id).expand(pos);
			}
		}

		@Override
		public ChunkSectionRegions build() {
			final List<ChunkSectionRegion> regions = new ArrayList<>(partialRegions.size());
			int count = 0;
			for (final PartialRegion region : partialRegions) {
				if (region.set != null && region.set.size() >= SIZE_THRESHOLD) {
					final short[] set = region.set.toShortArray();
					ShortArrays.quickSort(set);
					if (count > CUSTOM_MASK) {
						throw new RuntimeException("Too many regions!");
					}
					regions.add(new ChunkSectionRegionImpl(pos | count++, set));
				}
			}
			return new ChunkSectionRegionsImpl(pos, regions.toArray(new ChunkSectionRegion[0]));
		}
	}

	private static final class PartialRegion {
		private ShortSet set = null;

		public void expand(final short pos) {
			if (set == null) {
				set = new ShortOpenHashSet(8);
			}
			set.add(pos);
		}
	}

	private record RegionKeyImpl(BuilderImpl parent, int id) implements RegionKey {
	}
}
