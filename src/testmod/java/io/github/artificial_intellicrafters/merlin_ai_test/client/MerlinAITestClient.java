package io.github.artificial_intellicrafters.merlin_ai_test.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;

public class MerlinAITestClient implements ClientModInitializer {
	@Override
	public void onInitializeClient(final ModContainer mod) {
		LocationCacheTest.init();
		WorldRenderEvents.AFTER_ENTITIES.register(context -> {
			final MatrixStack stack = context.matrixStack();
			stack.push();
			final Vec3d pos = context.camera().getPos();
			stack.translate(-pos.x, -pos.y, -pos.z);
			BakeableDebugRenderers.tick(stack.peek().getModel(), context.projectionMatrix());
			stack.pop();
		});
	}
}
