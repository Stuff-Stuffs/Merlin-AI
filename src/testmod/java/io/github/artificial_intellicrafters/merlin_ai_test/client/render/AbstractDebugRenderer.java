package io.github.artificial_intellicrafters.merlin_ai_test.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.option.KeyBind;

public abstract class AbstractDebugRenderer {
	private final KeyBind keyBind;
	private final int timeOnPress;
	private int timeRemaining;

	protected AbstractDebugRenderer(final KeyBind bind, final int press) {
		keyBind = bind;
		timeOnPress = press;
	}

	public void tick() {
		if (timeRemaining > 0) {
			timeRemaining--;
			if (timeRemaining == 0) {
				clearState();
			} else {
				renderTick();
			}
		}
		if (keyBind.wasPressed()) {
			if (timeRemaining > 0) {
				timeRemaining = 0;
				clearState();
			} else {
				timeRemaining = timeOnPress;
				clearState();
				setup();
			}
		}
	}

	public void render(final WorldRenderContext context) {
		if (timeRemaining > 0) {
			renderDebug(context);
		}
	}

	protected void renderTick() {
	}

	protected abstract void renderDebug(WorldRenderContext context);

	protected abstract void clearState();

	protected abstract void setup();
}
