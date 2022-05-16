package io.github.artificial_intellicrafters.merlin_ai.impl.common;

import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutor;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutorWorld;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.task.SingleThreadedAITaskExecutor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.lifecycle.api.event.ServerWorldTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MerlinAI implements ModInitializer {
	//Object to be used in to represent a full but not complete object
	public static final Object PLACEHOLDER_OBJECT = new Object();
	public static final String MOD_ID = "merlin_ai";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize(final ModContainer mod) {
		ServerWorldTickEvents.END.register((server, world) -> ((AITaskExecutorWorld) world).merlin_ai$getTaskExecutor().runTasks(20));
	}

	public static Identifier createId(final String path) {
		return new Identifier(MOD_ID, path);
	}

	//TODO config
	public static AITaskExecutor createExecutor() {
		return new SingleThreadedAITaskExecutor(32);
	}
}
