package io.github.artificial_intellicrafters.merlin_ai.api.block_flag;

import net.minecraft.block.BlockState;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface BlockFlag {
	boolean test(BlockState state);

	int id();
}
