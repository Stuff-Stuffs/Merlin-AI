package io.github.artificial_intellicrafters.merlin_ai.impl.common;

import io.github.artificial_intellicrafters.merlin_ai.api.block_flag.BlockFlags;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.palette.PalettedContainer;

public class BlockStateFlagCounter implements PalettedContainer.CountConsumer<BlockState> {
	public final int[] flags = new int[BlockFlags.count()];

	@Override
	public void accept(final BlockState object, final int i) {
		final boolean[] flags = ((ExtendedBlockState) object).flags();
		for (int j = 0; j < flags.length; j++) {
			if (flags[j]) {
				this.flags[j] += i;
			}
		}
	}
}
