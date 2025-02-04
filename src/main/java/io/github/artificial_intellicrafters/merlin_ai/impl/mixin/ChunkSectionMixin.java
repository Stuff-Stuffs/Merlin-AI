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

@Mixin(ChunkSection.class)
public class ChunkSectionMixin implements PathingChunkSection {
	@Unique
	private long modCount = 1;
	@Unique
	private final short[] updatedPositions = new short[MerlinAI.PATHING_CHUNK_REMEMBERED_CHANGES];
	@Unique
	private final BlockState[] updatedStates = new BlockState[MerlinAI.PATHING_CHUNK_REMEMBERED_CHANGES];

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
			final int index = ((int) modCount) % MerlinAI.PATHING_CHUNK_REMEMBERED_CHANGES;
			updatedPositions[index] = PathingChunkSection.packLocal(x, y, z);
			updatedStates[index] = state;
			modCount++;
		}
	}

	@Override
	public long merlin_ai$getModCount() {
		return modCount;
	}

	@Override
	public boolean merlin_ai$copy_updates(final long lastModCount, final BlockState[] updateStateArray, final int updateStateArrayIndex, final short[] updatePosArray, final int updatePosArrayIndex) {
		final long diff = modCount - lastModCount;
		if (diff >= MerlinAI.PATHING_CHUNK_REMEMBERED_CHANGES) {
			return false;
		}
		final int startIndex = ((int) lastModCount) % MerlinAI.PATHING_CHUNK_REMEMBERED_CHANGES;
		final int endIndex = ((int) modCount) % MerlinAI.PATHING_CHUNK_REMEMBERED_CHANGES;
		PathingChunkSection.wrappingCopy(updatedStates, startIndex, endIndex, updateStateArray, updateStateArrayIndex);
		PathingChunkSection.wrappingCopy(updatedPositions, startIndex, endIndex, updatePosArray, updatePosArrayIndex);
		return true;
	}
}
