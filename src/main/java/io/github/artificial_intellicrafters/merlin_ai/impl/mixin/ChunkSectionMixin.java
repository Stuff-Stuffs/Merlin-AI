package io.github.artificial_intellicrafters.merlin_ai.impl.mixin;

import io.github.artificial_intellicrafters.merlin_ai.impl.common.MerlinAI;
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
	private long modCount = 0;
	@Unique
	private final long sectionId = MerlinAI.getNextSectionId();

	@Inject(at = @At("RETURN"), method = "setBlockState(IIILnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;")
	private void updateModCount(final int x, final int y, final int z, final BlockState state, final boolean lock, final CallbackInfoReturnable<BlockState> cir) {
		if (cir.getReturnValue() != state) {
			modCount++;
		}
	}

	@Override
	public long merlin_ai$getSectionId() {
		return sectionId;
	}

	@Override
	public long merlin_ai$getModCount() {
		return modCount;
	}
}
