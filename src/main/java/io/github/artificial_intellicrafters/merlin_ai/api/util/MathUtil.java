package io.github.artificial_intellicrafters.merlin_ai.api.util;

public final class MathUtil {

	public static int roundUpToPowerOf2(int v) {
		if (v <= 0) {
			return 0;
		}
		v = v - 1;
		v |= v >>> 1;
		v |= v >>> 2;
		v |= v >>> 4;
		v |= v >>> 8;
		v |= v >>> 16;
		return v + 1;
	}

	private MathUtil() {
	}
}
