package io.github.artificial_intellicrafters.merlin_ai.api.region;

import it.unimi.dsi.fastutil.longs.LongSet;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface ChunkSectionRegion<T> {
	int id();

	boolean contains(int x, int y, int z);

	void forEach(ForEach action);

	LongSet getOutgoingEdges(T context);

	interface ForEach {
		void accept(int x, int y, int z);
	}

	static boolean isLocal(final int x, final int y, final int z) {
		return x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 && z < 16;
	}

	static short packLocal(final int x, final int y, final int z) {
		if (!isLocal(x, y, z)) {
			throw new RuntimeException("Local coordinates must be between 0 and 15 inclusive");
		}
		return (short) ((x << 8) | (y << 4) | z);
	}

	static int unpackLocalX(final short packed) {
		return (packed >> 8) & 0xF;
	}

	static int unpackLocalY(final short packed) {
		return (packed >> 4) & 0xF;
	}

	static int unpackLocalZ(final short packed) {
		return packed & 0xF;
	}
}
