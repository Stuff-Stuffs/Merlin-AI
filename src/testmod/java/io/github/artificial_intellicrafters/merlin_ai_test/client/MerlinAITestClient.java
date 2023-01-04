package io.github.artificial_intellicrafters.merlin_ai_test.client;

import io.github.artificial_intellicrafters.merlin_ai_test.client.render.*;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBind;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientTickEvents;

import java.util.ArrayList;
import java.util.List;

public class MerlinAITestClient implements ClientModInitializer {
	public static final KeyBind PATH_KEYBIND = new KeyBind("merlin_ai.location_cache_test", GLFW.GLFW_KEY_F7, "misc");
	public static final KeyBind REGION_KEYBIND = new KeyBind("merlin_ai.region_test", GLFW.GLFW_KEY_F8, "misc");
	public static final KeyBind LINK_KEYBIND = new KeyBind("merlin_ai.link_test", GLFW.GLFW_KEY_F9, "misc");
	public static final KeyBind REGION_PATH_KEYBIND = new KeyBind("merlin_ai.region_path", GLFW.GLFW_KEY_F10, "misc");
	private static final List<AbstractDebugRenderer> DEBUG_RENDERERS = new ArrayList<>();

	@Override
	public void onInitializeClient(final ModContainer mod) {
		KeyBindingHelper.registerKeyBinding(PATH_KEYBIND);
		KeyBindingHelper.registerKeyBinding(REGION_KEYBIND);
		KeyBindingHelper.registerKeyBinding(LINK_KEYBIND);
		KeyBindingHelper.registerKeyBinding(REGION_PATH_KEYBIND);
		LocationCacheTest.init();
		ClientTickEvents.START.register(client -> DEBUG_RENDERERS.forEach(AbstractDebugRenderer::tick));
		WorldRenderEvents.AFTER_ENTITIES.register(context -> {
			DEBUG_RENDERERS.forEach(renderer -> renderer.render(context));
			final MatrixStack stack = context.matrixStack();
			stack.push();
			final Vec3d pos = context.camera().getPos();
			stack.translate(-pos.x, -pos.y, -pos.z);
			BakeableDebugRenderers.tick(stack.peek().getModel(), context.projectionMatrix());
			stack.pop();
		});
		renderDebugRenderer(new PathDebugRenderer(PATH_KEYBIND, 20 * 60));
		renderDebugRenderer(new NearbyRegionDebugRenderer(REGION_KEYBIND, 20 * 60));
		renderDebugRenderer(new AdjacentRegionsDebugRenderer(LINK_KEYBIND, 20 * 15));
		renderDebugRenderer(new RegionPathDebugRenderer(REGION_PATH_KEYBIND, 20 * 150));
	}

	public static void renderDebugRenderer(final AbstractDebugRenderer renderer) {
		DEBUG_RENDERERS.add(renderer);
	}
}
