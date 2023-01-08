package io.github.artificial_intellicrafters.merlin_ai_test.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.option.KeyBind;

public abstract class AbstractDebugRenderer {
	private final KeyBind keyBind;
	private boolean enabled;

	protected AbstractDebugRenderer(final KeyBind bind) {
		keyBind = bind;
	}

	public final void tick() {
		if (keyBind.wasPressed()) {
			enabled = !enabled;
			clearState();
			if(enabled) {
				setup();
			}
		}
	}

	public final void render(final WorldRenderContext context) {
		if (enabled) {
			renderDebug(context);
		}
	}

	protected void renderTick() {
	}

	protected abstract void renderDebug(WorldRenderContext context);

	protected abstract void clearState();

	protected abstract void setup();
}
