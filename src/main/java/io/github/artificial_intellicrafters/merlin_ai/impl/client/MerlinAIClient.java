package io.github.artificial_intellicrafters.merlin_ai.impl.client;

import io.github.artificial_intellicrafters.merlin_ai.api.AIWorld;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.ChunkRegionGraphImpl;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientWorldTickEvents;

public class MerlinAIClient implements ClientModInitializer {
	@Override
	public void onInitializeClient(final ModContainer mod) {
		ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> ((ChunkRegionGraphImpl) ((AIWorld) world).merlin_ai$getChunkGraph()).load(chunk));
		ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> ((ChunkRegionGraphImpl) ((AIWorld) world).merlin_ai$getChunkGraph()).unload(chunk));
		ClientWorldTickEvents.END.register((server, world) -> ((AIWorld) world).merlin_ai$getTaskExecutor().runTasks(20));
	}
}
