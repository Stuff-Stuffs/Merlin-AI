package io.github.artificial_intellicrafters.merlin_ai_test.client;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;

public class MerlinAITestClient implements ClientModInitializer {
	@Override
	public void onInitializeClient(final ModContainer mod) {
		LocationCacheTest.init();
	}
}
