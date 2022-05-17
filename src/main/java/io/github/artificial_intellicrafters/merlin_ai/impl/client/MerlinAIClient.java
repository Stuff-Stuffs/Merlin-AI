package io.github.artificial_intellicrafters.merlin_ai.impl.client;

import io.github.artificial_intellicrafters.merlin_ai.api.AIWorld;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientWorldTickEvents;

public class MerlinAIClient implements ClientModInitializer {
	@Override
	public void onInitializeClient(final ModContainer mod) {
		ClientWorldTickEvents.END.register((server, world) -> ((AIWorld) world).merlin_ai$getTaskExecutor().runTasks(20));
	}
}
