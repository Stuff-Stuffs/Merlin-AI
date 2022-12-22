package io.github.artificial_intellicrafters.merlin_ai_test.common;

import io.github.artificial_intellicrafters.merlin_ai.api.block_flag.BlockFlag;
import io.github.artificial_intellicrafters.merlin_ai.api.block_flag.BlockFlags;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.shape.VoxelShapes;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;

public class MerlinAITest implements ModInitializer {
	public static final String MOD_ID = "merlin_ai_test";
	public static final BlockFlag FULL_BLOCK = BlockFlags.create(state -> !state.getBlock().hasDynamicBounds() && !VoxelShapes.matchesAnywhere(state.getCollisionShape(null, null), VoxelShapes.fullCube(), BooleanBiFunction.NOT_SAME));
	public static final BlockFlag AIR_BLOCK = BlockFlags.create(state -> state.isAir() || (!state.getBlock().hasDynamicBounds() && state.getCollisionShape(null, null).isEmpty()));

	@Override
	public void onInitialize(final ModContainer mod) {
	}
}
