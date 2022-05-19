package io.github.artificial_intellicrafters.merlin_ai.impl.common;

import io.github.artificial_intellicrafters.merlin_ai.api.AIWorld;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutor;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.region.graph.ChunkRegionGraphImpl;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.task.SingleThreadedAITaskExecutor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.lifecycle.api.event.ServerWorldTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class MerlinAI implements ModInitializer {
	//Object to be used in to represent a full but not complete object
	public static final Object PLACEHOLDER_OBJECT = new Object();
	private static final AtomicLong NEXT_SECTION_ID = new AtomicLong(Long.MIN_VALUE);
	public static final String MOD_ID = "merlin_ai";
	public static final boolean DEBUG = QuiltLoader.isDevelopmentEnvironment();
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize(final ModContainer mod) {
		ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> ((ChunkRegionGraphImpl) ((AIWorld) world).merlin_ai$getChunkGraph()).load(chunk));
		ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> ((ChunkRegionGraphImpl) ((AIWorld) world).merlin_ai$getChunkGraph()).unload(chunk));
		ServerWorldTickEvents.END.register((server, world) -> ((ChunkRegionGraphImpl) ((AIWorld) world).merlin_ai$getChunkGraph()).tick());

		ServerWorldTickEvents.END.register((server, world) -> ((AIWorld) world).merlin_ai$getTaskExecutor().runTasks(20));
	}

	public static Identifier createId(final String path) {
		return new Identifier(MOD_ID, path);
	}

	//TODO config
	public static AITaskExecutor createExecutor() {
		return new SingleThreadedAITaskExecutor(255);
	}

	public static long getNextSectionId() {
		return NEXT_SECTION_ID.getAndIncrement();
	}
}
