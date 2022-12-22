package io.github.artificial_intellicrafters.merlin_ai.api.block_flag;

import io.github.artificial_intellicrafters.merlin_ai.impl.common.MerlinAI;
import net.minecraft.block.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class BlockFlags {
	private static final List<BlockFlag> FLAGS = new ArrayList<>();

	public static BlockFlag create(final Predicate<BlockState> predicate) {
		if (MerlinAI.FROZEN) {
			throw new RuntimeException();
		}
		final int id = FLAGS.size();
		final BlockFlag flag = new BlockFlag() {
			@Override
			public boolean test(final BlockState state) {
				return predicate.test(state);
			}

			@Override
			public int id() {
				return id;
			}
		};
		FLAGS.add(flag);
		return flag;
	}

	public static int count() {
		return FLAGS.size();
	}

	public static boolean[] flag(final BlockState state) {
		final boolean[] flags = new boolean[FLAGS.size()];
		for (int i = 0; i < flags.length; i++) {
			flags[i] = FLAGS.get(i).test(state);
		}
		return flags;
	}

	private BlockFlags() {
	}
}
