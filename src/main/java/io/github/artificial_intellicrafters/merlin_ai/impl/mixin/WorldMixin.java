package io.github.artificial_intellicrafters.merlin_ai.impl.mixin;

import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutor;
import io.github.artificial_intellicrafters.merlin_ai.api.AIWorld;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.MerlinAI;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(World.class)
public class WorldMixin implements AIWorld {
	@Unique
	private final AITaskExecutor taskExecutor = MerlinAI.createExecutor();

	@Override
	public AITaskExecutor merlin_ai$getTaskExecutor() {
		return taskExecutor;
	}
}
