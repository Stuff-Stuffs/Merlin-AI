package io.github.artificial_intellicrafters.merlin_ai.impl.common.region;

import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegion;

public class ChunkSectionSmallRegionImpl implements ChunkSectionRegion {
	private final int id;
	private final short[] packed;

	public ChunkSectionSmallRegionImpl(final int id, final short[] packed) {
		this.id = id;
		this.packed = packed;
	}

	@Override
	public int id() {
		return id;
	}

	@Override
	public boolean contains(final int x, final int y, final int z) {
		final short pack = ChunkSectionRegion.packLocal(x & 15, y & 15, z & 15);
		for (final short i : packed) {
			if (pack == i) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void forEach(final ForEach action) {
		for (final short i : packed) {
			action.accept(ChunkSectionRegion.unpackLocalX(i), ChunkSectionRegion.unpackLocalY(i), ChunkSectionRegion.unpackLocalZ(i));
		}
	}
}
