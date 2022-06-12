package io.github.artificial_intellicrafters.merlin_ai.impl.common;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.ChunkSectionPos;

public interface PathingChunkSection {
	long merlin_ai$getModCount();

	boolean merlin_ai$copy_updates(long lastModCount, BlockState[] updateStateArray, int updateStateArrayIndex, short[] updatePosArray, int updatePosArrayIndex);

	static short packLocal(final int x, final int y, final int z) {
		final int i = ChunkSectionPos.getLocalCoord(x);
		final int j = ChunkSectionPos.getLocalCoord(y);
		final int k = ChunkSectionPos.getLocalCoord(z);
		return (short) (i << 8 | k << 4 | j << 0);
	}

	static int unpackLocalX(final short packedLocalPos) {
		return packedLocalPos >>> 8 & 15;
	}

	static int unpackLocalY(final short packedLocalPos) {
		return packedLocalPos >>> 0 & 15;
	}

	static int unpackLocalZ(final short packedLocalPos) {
		return packedLocalPos >>> 4 & 15;
	}

	static void wrappingCopy(final short[] source, final int startIndex, final int endIndex, final short[] target, final int offset) {
		if (startIndex < endIndex) {
			System.arraycopy(source, startIndex, target, offset, endIndex - startIndex);
		} else {
			System.arraycopy(source, startIndex, target, offset, source.length - startIndex);
			System.arraycopy(source, 0, target, offset + source.length - startIndex, endIndex);
		}
	}

	static <T> void wrappingCopy(final T[] source, final int startIndex, final int endIndex, final T[] target, final int offset) {
		if (startIndex < endIndex) {
			System.arraycopy(source, startIndex, target, offset, endIndex - startIndex);
		} else {
			System.arraycopy(source, startIndex, target, offset, source.length - startIndex);
			System.arraycopy(source, 0, target, offset + source.length - startIndex, endIndex);
		}
	}
}
