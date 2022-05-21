package io.github.artificial_intellicrafters.merlin_ai.impl.mixin;

import io.github.artificial_intellicrafters.merlin_ai.api.util.SubChunkSectionUtil;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.PathingChunkSection;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//TODO clear when adjacent chunk sections change
@Mixin(ChunkSection.class)
public class ChunkSectionMixin implements PathingChunkSection {
	@Unique
	private final int[] modCounts = new int[8];

	@Inject(at = @At("RETURN"), method = "setBlockState(IIILnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;")
	private void updateModCount(final int x, final int y, final int z, final BlockState state, final boolean lock, final CallbackInfoReturnable<BlockState> cir) {
		final BlockState value = cir.getReturnValue();
		boolean flag = false;
		if (value != state) {
			final boolean b0 = value.getBlock().hasDynamicBounds();
			if (b0) {
				flag = true;
			} else {
				final boolean b1 = state.getBlock().hasDynamicBounds();
				if (b1) {
					flag = true;
				} else {
					if (value.getCollisionShape(null, null) != state.getCollisionShape(null, null)) {
						flag = true;
					}
				}
			}
		}
		if (flag) {
			final int index = SubChunkSectionUtil.subSectionIndex(SubChunkSectionUtil.blockToSubSection(x), SubChunkSectionUtil.blockToSubSection(y), SubChunkSectionUtil.blockToSubSection(z));
			modCounts[index]++;
		}
	}

	@Override
	public int merlin_ai$getModCount(final int index) {
		return modCounts[index];
	}
}
