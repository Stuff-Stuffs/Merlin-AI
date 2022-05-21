package io.github.artificial_intellicrafters.merlin_ai.api.region;

import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import it.unimi.dsi.fastutil.longs.LongSet;

public interface ChunkSectionRegions<T, N extends AIPathNode<T, N>> {
	int getContainingRegion(int x, int y, int z);

	boolean isValidRegion(int regionId);

	LongSet getOutgoingEdges(int regionId, T context, N previousNode);

	ChunkSectionRegionType<T, N> type();

	static boolean isLocal(final int x, final int y, final int z) {
		return x >= 0 && x < 8 && y >= 0 && y < 8 && z >= 0 && z < 8;
	}

	static short packLocal(final int x, final int y, final int z) {
		if (!isLocal(x, y, z)) {
			throw new RuntimeException("Local coordinates must be between 0 and 15 inclusive");
		}
		return (short) ((x << 6) | (y << 3) | z);
	}

	static int unpackLocalX(final short packed) {
		return (packed >> 6) & 0x7;
	}

	static int unpackLocalY(final short packed) {
		return (packed >> 3) & 0x7;
	}

	static int unpackLocalZ(final short packed) {
		return packed & 0x7;
	}
}
