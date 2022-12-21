package io.github.artificial_intellicrafters.merlin_ai.impl.common;

import io.github.artificial_intellicrafters.merlin_ai.api.AIWorld;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutor;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.task.SingleThreadedAITaskExecutor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.lifecycle.api.event.ServerLifecycleEvents;
import org.quiltmc.qsl.lifecycle.api.event.ServerWorldTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MerlinAI implements ModInitializer {
	public static final boolean DEBUG = QuiltLoader.isDevelopmentEnvironment();
	//Object to be used in to represent a full but not complete object
	public static final Object PLACEHOLDER_OBJECT = new Object();
	public static final int PATHING_CHUNK_REMEMBERED_CHANGES = 16;
	public static final boolean REMEMBERED_CHANGES_POWER_OF_TWO = (PATHING_CHUNK_REMEMBERED_CHANGES & (PATHING_CHUNK_REMEMBERED_CHANGES - 1)) == 0;
	public static final int PATHING_CHUNK_CHANGES_BEFORE_RESET = 48;
	public static final String MOD_ID = "merlin_ai";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static boolean FROZEN = false;

	@Override
	public void onInitialize(final ModContainer mod) {
		ServerLifecycleEvents.STARTING.register(server -> FROZEN = true);
		ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> ((ChunkRegionGraphImpl) ((AIWorld) world).merlin_ai$getChunkGraph()).load(chunk));
		ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> ((ChunkRegionGraphImpl) ((AIWorld) world).merlin_ai$getChunkGraph()).unload(chunk));

		ServerWorldTickEvents.END.register((server, world) -> ((AIWorld) world).merlin_ai$getTaskExecutor().runTasks(20));
	}

	public static Identifier createId(final String path) {
		return new Identifier(MOD_ID, path);
	}

	//TODO config
	public static AITaskExecutor createExecutor() {
		return new SingleThreadedAITaskExecutor(32);
	}
}
