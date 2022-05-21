package io.github.artificial_intellicrafters.merlin_ai.api.util;

import net.minecraft.util.math.MathHelper;

public final class SubChunkSectionUtil {
	public static int SUB_SECTION_SIZE = 8;
	private static final int SIZE_BITS_FLAG = 9;
	private static final int SIZE_BITS_X = (1 + MathHelper.log2(MathHelper.smallestEncompassingPowerOfTwo(30000000))) - 3;
	private static final int SIZE_BITS_Z = SIZE_BITS_X;
	private static final int SIZE_BITS_Y = 64 - SIZE_BITS_FLAG - SIZE_BITS_X - SIZE_BITS_Z;
	private static final long BITS_X = (1L << SIZE_BITS_X) - 1L;
	private static final long BITS_Y = (1L << SIZE_BITS_Y) - 1L;
	private static final long BITS_Z = (1L << SIZE_BITS_Z) - 1L;
	private static final long BITS_FLAG = (1L << SIZE_BITS_FLAG) - 1L;
	private static final int BIT_SHIFT_Y = 0;
	private static final int BIT_SHIFT_Z = SIZE_BITS_Y;
	private static final int BIT_SHIFT_X = SIZE_BITS_Y + SIZE_BITS_Z;
	private static final int BIT_SHIFT_FLAG = SIZE_BITS_Y + SIZE_BITS_X + SIZE_BITS_Z;

	public static boolean isLocal(final int x, final int y, final int z) {
		return 0 <= x && x < SUB_SECTION_SIZE && 0 <= y && y < SUB_SECTION_SIZE && 0 <= z && z < SUB_SECTION_SIZE;
	}

	public static short packLocal(final int x, final int y, final int z) {
		short s = (short) ((x & 7) << 6);
		s |= (y & 7) << 3;
		s |= (z & 7);
		return s;
	}

	public static int unpackLocalX(final short packed) {
		return (packed >> 6) & 7;
	}

	public static int unpackLocalY(final short packed) {
		return (packed >> 3) & 7;
	}

	public static int unpackLocalZ(final short packed) {
		return packed & 7;
	}

	public static long pack(final int subSectionX, final int subSectionY, final int subSectionZ, final int flag) {
		long l = ((long) subSectionX & BITS_X) << BIT_SHIFT_X;
		l |= ((long) subSectionY & BITS_Y);
		l |= ((long) subSectionZ & BITS_Z) << BIT_SHIFT_Z;
		l |= ((long) flag & BITS_FLAG) << BIT_SHIFT_FLAG;
		return l;
	}

	public static int unpackX(final long packedPos) {
		return (int) (packedPos << 64 - BIT_SHIFT_X - SIZE_BITS_X >> 64 - SIZE_BITS_X);
	}

	public static int unpackY(final long packedPos) {
		return (int) (packedPos << 64 - BIT_SHIFT_Y - SIZE_BITS_Y >> 64 - SIZE_BITS_Y);
	}

	public static int unpackZ(final long packedPos) {
		return (int) (packedPos << 64 - BIT_SHIFT_Z - SIZE_BITS_Z >> 64 - SIZE_BITS_Z);
	}

	public static int unpackFlag(final long packedPos) {
		return (int) (packedPos << 64 - BIT_SHIFT_FLAG - SIZE_BITS_FLAG >> 64 - SIZE_BITS_FLAG);
	}

	public static int subSectionToBlock(final int subSectionCoordinate) {
		return subSectionCoordinate << 3;
	}

	public static int blockToSubSection(final int blockCoordinate) {
		return blockCoordinate >> 3;
	}

	private SubChunkSectionUtil() {
	}

	public static int subSectionIndex(final long packed) {
		return subSectionIndex(unpackX(packed), unpackY(packed), unpackZ(packed));
	}

	public static int subSectionIndex(final int subSectionX, final int subSectionY, final int subSectionZ) {
		return ((subSectionX & 1) * 2 + (subSectionY & 1)) * 2 + (subSectionZ & 1);
	}
}
