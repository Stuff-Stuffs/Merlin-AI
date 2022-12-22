package io.github.artificial_intellicrafters.merlin_ai.impl.mixin;

import io.github.artificial_intellicrafters.merlin_ai.api.block_flag.BlockFlags;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.ExtendedBlockState;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractBlock.AbstractBlockState.class)
public class MixinBlockState implements ExtendedBlockState {
	private boolean[] flags = null;

	@Override
	public boolean[] flags() {
		if (flags == null) {
			flags = BlockFlags.flag((BlockState) (Object) this);
		}
		return flags;
	}
}
