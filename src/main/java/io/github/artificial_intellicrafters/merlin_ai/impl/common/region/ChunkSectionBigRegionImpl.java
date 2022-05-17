package io.github.artificial_intellicrafters.merlin_ai.impl.common.region;

import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegion;
import it.unimi.dsi.fastutil.shorts.ShortIterator;
import it.unimi.dsi.fastutil.shorts.ShortSet;

public class ChunkSectionBigRegionImpl implements ChunkSectionRegion {
	private final int id;
	private final ShortSet set;

	public ChunkSectionBigRegionImpl(final int id, final ShortSet positions) {
		this.id = id;
		set = positions;
	}

	@Override
	public int id() {
		return id;
	}

	@Override
	public boolean contains(final int x, final int y, final int z) {
		final short local = ChunkSectionRegion.packLocal(x & 15, y & 15, z & 15);
		return set.contains(local);
	}

	@Override
	public void forEach(final ForEach action) {
		final ShortIterator iterator = set.iterator();
		while (iterator.hasNext()) {
			final short local = iterator.nextShort();
			action.accept(ChunkSectionRegion.unpackLocalX(local), ChunkSectionRegion.unpackLocalY(local), ChunkSectionRegion.unpackLocalZ(local));
		}
	}
}
