package io.github.artificial_intellicrafters.merlin_ai_test.client.render;

import io.github.artificial_intellicrafters.merlin_ai_test.client.LocationCacheTest;
import io.github.artificial_intellicrafters.merlin_ai_test.common.BasicAIPathNode;
import io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test.AIPath;
import io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test.AIPather;
import io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test.PathTarget;
import io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test.TestNodeProducer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBind;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DustParticleEffect;
import org.joml.Vector3f;

public final class PathDebugRenderer extends AbstractDebugRenderer {
	private AIPath<Entity, BasicAIPathNode> lastPath = null;

	public PathDebugRenderer(final KeyBind bind, final int time) {
		super(bind, time);
	}

	@Override
	protected void renderDebug(final WorldRenderContext context) {
		if (lastPath != null) {
			final DustParticleEffect effect = new DustParticleEffect(new Vector3f(1, 0, 0), 1);
			if (context.world().getTime() % 10 == 0) {
				for (final Object o : lastPath.getNodes()) {
					final BasicAIPathNode node = (BasicAIPathNode) o;
					context.world().addParticle(effect, node.x + 0.5, node.y + 0.5, node.z + 0.5, 0, 0, 0);
				}
			}
		}
	}

	@Override
	protected void clearState() {
		lastPath = null;
	}

	@Override
	protected void setup() {
		final MinecraftClient client = MinecraftClient.getInstance();
		final AIPather<Entity, BasicAIPathNode> pather = new AIPather<>(client.world, new TestNodeProducer(LocationCacheTest.ONE_X_TWO_BASIC_LOCATION_SET_TYPE), Entity::getBlockPos);
		lastPath = pather.calculatePath(PathTarget.yLevel(-64), 256, true, client.cameraEntity);
	}
}
