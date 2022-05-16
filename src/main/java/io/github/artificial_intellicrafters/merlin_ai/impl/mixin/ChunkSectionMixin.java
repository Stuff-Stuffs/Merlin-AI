package io.github.artificial_intellicrafters.merlin_ai.impl.mixin;

import io.github.artificial_intellicrafters.merlin_ai.api.PathingChunkSection;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutor;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutorWorld;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.MerlinAI;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.task.ChunkRegionsAnalysisAITask;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.task.ValidLocationAnalysisChunkSectionAITTask;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

//TODO clear when adjacent chunk sections change
@Mixin(ChunkSection.class)
public class ChunkSectionMixin implements PathingChunkSection {
	@Unique
	private int nextRegionId = Integer.MIN_VALUE;
	@Unique
	private long modCount = 0;
	@Unique
	private boolean cleared = true;
	@Unique
	private final Map<ValidLocationSetType<?>, Object> validLocationSetCache = new Reference2ReferenceOpenHashMap<>();
	@Unique
	private final Map<ChunkSectionRegionType, Object> regionsCache = new Reference2ReferenceOpenHashMap<>();

	@Inject(at = @At("RETURN"), method = "setBlockState(IIILnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;")
	private void clearCache(final int x, final int y, final int z, final BlockState state, final boolean lock, final CallbackInfoReturnable<BlockState> cir) {
		if (!cleared) {
			if (cir.getReturnValue() != state) {
				validLocationSetCache.clear();
				regionsCache.clear();
				cleared = true;
			}
		}
		modCount++;
	}

	@Override
	public <T> ValidLocationSet<T> merlin_ai$getValidLocationSet(final ValidLocationSetType<T> type, final ChunkSectionPos pos, final ShapeCache world) {
		final Object locationSet = validLocationSetCache.get(type);
		if (locationSet == null) {
			final AITaskExecutor executor = ((AITaskExecutorWorld) world.getDelegate()).merlin_ai$getTaskExecutor();
			final long l = modCount;
			if (executor.submitTask(new ValidLocationAnalysisChunkSectionAITTask(l, this::getModCount, type, pos, world, i -> {
				if (l == modCount) {
					validLocationSetCache.put(type, i);
				}
			}))) {
				validLocationSetCache.put(type, MerlinAI.PLACEHOLDER_OBJECT);
				cleared = false;
			}
			return null;
		}
		return (ValidLocationSet<T>) locationSet;
	}

	private long getModCount() {
		return modCount;
	}

	@Override
	public <T> ValidLocationSet<T> merlin_ai$getValidLocationSet(final ValidLocationSetType<T> type, final int x, final int y, final int z, final ShapeCache world) {
		final Object locationSet = validLocationSetCache.get(type);
		if (locationSet == MerlinAI.PLACEHOLDER_OBJECT) {
			return null;
		}
		if (locationSet == null) {
			final ChunkSectionPos pos = ChunkSectionPos.from(new BlockPos(x, y, z));
			final AITaskExecutor executor = ((AITaskExecutorWorld) world.getDelegate()).merlin_ai$getTaskExecutor();
			final long l = modCount;
			if (executor.submitTask(new ValidLocationAnalysisChunkSectionAITTask(l, this::getModCount, type, pos, world, i -> {
				if (l == modCount) {
					validLocationSetCache.put(type, i);
				}
			}))) {
				validLocationSetCache.put(type, MerlinAI.PLACEHOLDER_OBJECT);
				cleared = false;
			}
			return null;
		}
		return (ValidLocationSet<T>) locationSet;
	}

	@Override
	public @Nullable ChunkSectionRegions merlin_ai$getChunkSectionRegions(final ChunkSectionRegionType type, final int x, final int y, final int z, final ShapeCache world) {
		final Object regions = regionsCache.get(type);
		if (regions == MerlinAI.PLACEHOLDER_OBJECT) {
			return null;
		}
		if (regions == null) {
			final ChunkSectionPos pos = ChunkSectionPos.from(new BlockPos(x, y, z));
			final AITaskExecutor executor = ((AITaskExecutorWorld) world.getDelegate()).merlin_ai$getTaskExecutor();
			final long l = modCount;
			if (executor.submitTask(new ChunkRegionsAnalysisAITask(l, this::getModCount, type, pos, this, world, i -> {
				if (l == modCount) {
					regionsCache.put(type, i);
				}
			}))) {
				cleared = false;
				regionsCache.put(type, MerlinAI.PLACEHOLDER_OBJECT);
			}
			return null;
		}
		return (ChunkSectionRegions) regions;
	}

	@Override
	public int getNextRegionId() {
		return nextRegionId;
	}

	@Override
	public void setNextRegionId(final int id) {
		nextRegionId = id;
	}
}
