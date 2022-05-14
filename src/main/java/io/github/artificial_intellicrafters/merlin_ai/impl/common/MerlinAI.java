package io.github.artificial_intellicrafters.merlin_ai.impl.common;

import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MerlinAI implements ModInitializer {
	public static final String MOD_ID = "merlin_ai";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize(final ModContainer mod) {
	}

	public static Identifier createId(final String path) {
		return new Identifier(MOD_ID, path);
	}
}
