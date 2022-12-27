package io.github.artificial_intellicrafters.merlin_ai_test.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tessellator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import org.joml.Matrix4f;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class BakeableDebugRenderers {
	private static final ReferenceQueue<KeyImpl> CLEANUP_QUEUE = new ReferenceQueue<>();
	private static final Map<KeyHolder, Rendered[]> RENDERED_OBJECTS = new Reference2ReferenceOpenHashMap<>();
	private static final Tessellator TESSELLATOR = new Tessellator();
	private static long NEXT_ID = 0;

	public static Key render(final Consumer<VertexConsumerProvider> renderer) {
		synchronized (TESSELLATOR) {
			final KeyImpl impl = new KeyImpl(NEXT_ID++);
			final KeyHolder holder = new KeyHolder(impl, CLEANUP_QUEUE);
			final VertexConsumerProviderImpl consumerProvider = new VertexConsumerProviderImpl(TESSELLATOR.getBufferBuilder(), (layer, rendered) -> {
				final VertexBuffer buffer = new VertexBuffer();
				buffer.bind();
				buffer.upload(rendered);
				final Rendered[] rendereds = RENDERED_OBJECTS.get(holder);
				if (rendereds == null) {
					RENDERED_OBJECTS.put(holder, new Rendered[]{new Rendered(buffer, layer)});
				} else {
					final Rendered[] copy = Arrays.copyOf(rendereds, rendereds.length + 1);
					copy[copy.length - 1] = new Rendered(buffer, layer);
					RENDERED_OBJECTS.put(holder, copy);
				}
			});
			renderer.accept(consumerProvider);

			consumerProvider.end();
			return impl;
		}
	}

	private static void remove(final long index) {
		final Iterator<Map.Entry<KeyHolder, Rendered[]>> iterator = RENDERED_OBJECTS.entrySet().iterator();
		while (iterator.hasNext()) {
			final KeyHolder key = iterator.next().getKey();
			final KeyImpl k = key.get();
			if (k != null && k.index == index) {
				iterator.remove();
			}
		}
	}

	private static boolean contains(final long index) {
		for (final Map.Entry<KeyHolder, Rendered[]> entry : RENDERED_OBJECTS.entrySet()) {
			final KeyHolder key = entry.getKey();
			final KeyImpl k = key.get();
			if (k != null && k.index == index) {
				return true;
			}
		}
		return false;
	}

	public static void tick(final Matrix4f modelView, final Matrix4f proj) {
		for (Object x; (x = CLEANUP_QUEUE.poll()) != null; ) {
			final KeyHolder holder = (KeyHolder) x;
			final Rendered[] remove = RENDERED_OBJECTS.remove(holder);
			if (remove != null) {
				for (final Rendered rendered : remove) {
					rendered.buffer.close();
				}
			}
		}
		for (final Rendered[] rendereds : RENDERED_OBJECTS.values()) {
			for (final Rendered rendered : rendereds) {
				if(!rendered.buffer().invalid()) {
					rendered.buffer().bind();
					rendered.layer().startDrawing();
					rendered.buffer().draw(modelView, proj, RenderSystem.getShader());
					rendered.layer().endDrawing();
				}
			}
		}
		VertexBuffer.unbind();
	}

	private static final class VertexConsumerProviderImpl implements VertexConsumerProvider {
		private final BufferBuilder builder;
		private final BiConsumer<RenderLayer, BufferBuilder.RenderedBuffer> consumer;
		private RenderLayer layer;

		private VertexConsumerProviderImpl(final BufferBuilder builder, final BiConsumer<RenderLayer, BufferBuilder.RenderedBuffer> consumer) {
			this.builder = builder;
			this.consumer = consumer;
		}

		@Override
		public VertexConsumer getBuffer(final RenderLayer renderLayer) {
			if (layer == renderLayer) {
				return builder;
			}
			if (layer != null) {
				consumer.accept(layer, builder.end());
			}
			layer = renderLayer;
			builder.begin(renderLayer.getDrawMode(), renderLayer.getVertexFormat());
			return builder;
		}

		private void end() {
			if(layer!=null) {
				consumer.accept(layer, builder.end());
			}
		}
	}

	private record Rendered(VertexBuffer buffer, RenderLayer layer) {
	}

	public interface Key {
		boolean deleted();

		void delete();
	}

	private static final class KeyImpl implements Key {
		private final long index;

		private KeyImpl(final long index) {
			this.index = index;
		}

		@Override
		public boolean deleted() {
			return contains(index);
		}

		@Override
		public void delete() {
			remove(index);
		}
	}

	private static final class KeyHolder extends WeakReference<KeyImpl> {
		private final long index;

		public KeyHolder(final KeyImpl referent, final ReferenceQueue<? super KeyImpl> q) {
			super(referent, q);
			index = referent.index;
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof KeyHolder holder)) {
				return false;
			}

			return index == holder.index;
		}

		@Override
		public int hashCode() {
			return (int) (index ^ (index >>> 32));
		}
	}
}
