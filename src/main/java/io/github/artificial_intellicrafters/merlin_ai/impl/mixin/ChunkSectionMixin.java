package io.github.artificial_intellicrafters.merlin_ai.impl.mixin;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutor;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutorWorld;
import io.github.artificial_intellicrafters.merlin_ai.api.util.WorldCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.location_caching.PathingChunkSection;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.task.AnalyseChunkSectionAITask;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(ChunkSection.class)
public class ChunkSectionMixin implements PathingChunkSection {
	private @Unique
	long modCount = 0;
	private boolean cleared = true;
	@Unique
	private final Map<ValidLocationSetType<?>, ValidLocationSet<?>> validLocationSetCache = new Reference2ReferenceOpenHashMap<>();

	@Inject(at = @At("RETURN"), method = "setBlockState(IIILnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;")
	private void clearCache(final int x, final int y, final int z, final BlockState state, final boolean lock, final CallbackInfoReturnable<BlockState> cir) {
		if (!cleared) {
			if (cir.getReturnValue() != state) {
				validLocationSetCache.clear();
				cleared = true;
			}
		}
		modCount++;
	}

	@Override
	public <T> ValidLocationSet<T> merlin_ai$getValidLocationSet(final ValidLocationSetType<T> type, final ChunkSectionPos pos, final WorldCache world) {
		final ValidLocationSet<T> locationSet = (ValidLocationSet<T>) validLocationSetCache.get(type);
		if (locationSet == null) {
			final AITaskExecutor executor = ((AITaskExecutorWorld) world.getDelegate()).merlin_ai$getTaskExecutor();
			final long l = modCount;
			executor.submitTask(new AnalyseChunkSectionAITask(l, this::getModCount, type, pos, world, i -> {
				if (l == modCount) {
					validLocationSetCache.put(type, i);
				}
			}));
			cleared = false;
			return null;
		}
		return locationSet;
	}

	private long getModCount() {
		return modCount;
	}

	@Override
	public <T> ValidLocationSet<T> merlin_ai$getValidLocationSet(final ValidLocationSetType<T> type, final int x, final int y, final int z, final WorldCache world) {
		final ValidLocationSet<T> locationSet = (ValidLocationSet<T>) validLocationSetCache.get(type);
		if (locationSet == null) {
			final ChunkSectionPos pos = ChunkSectionPos.from(new BlockPos(x, y, z));
			final AITaskExecutor executor = ((AITaskExecutorWorld) world.getDelegate()).merlin_ai$getTaskExecutor();
			final long l = modCount;
			executor.submitTask(new AnalyseChunkSectionAITask(l, this::getModCount, type, pos, world, i -> {
				if (l == modCount) {
					validLocationSetCache.put(type, i);
				}
			}));
			cleared = false;
			return null;
		}
		return locationSet;
	}
}
