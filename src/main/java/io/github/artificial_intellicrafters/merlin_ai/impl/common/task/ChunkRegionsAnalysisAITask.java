package io.github.artificial_intellicrafters.merlin_ai.impl.common.task;

import io.github.artificial_intellicrafters.merlin_ai.api.PathingChunkSection;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.path.NeighbourGetter;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegion;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITask;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.region.ChunkSectionBigRegionImpl;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.region.ChunkSectionRegionsImpl;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.region.ChunkSectionSmallRegionImpl;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.*;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public class ChunkRegionsAnalysisAITask implements AITask {
	private final long modCount;
	private final LongSupplier currentModCount;
	private final ChunkSectionRegionType<?, ?> type;
	private final ChunkSectionPos pos;
	private final PathingChunkSection section;
	private final ShapeCache cache;
	private final Consumer<ChunkSectionRegions> completionConsumer;
	private ChunkSectionRegions output = null;
	private boolean finished = false;
	private int lastX = 0;
	private int lastY = 0;
	private int lastZ = 0;
	private int count = 1;
	private int order = 1;

	public ChunkRegionsAnalysisAITask(final long modCount, final LongSupplier currentModCount, final ChunkSectionRegionType<?, ?> type, final ChunkSectionPos pos, final PathingChunkSection section, final ShapeCache cache, final Consumer<ChunkSectionRegions> completionConsumer) {
		this.modCount = modCount;
		this.currentModCount = currentModCount;
		this.type = type;
		this.pos = pos;
		this.section = section;
		this.cache = cache;
		this.completionConsumer = completionConsumer;
	}

	@Override
	public int priority() {
		return AITask.super.priority() + 1;
	}

	@Override
	public boolean done() {
		return output != null || modCount != currentModCount.getAsLong();
	}

	@Override
	public void runIteration() {
		final int x = pos.getMinX();
		final int y = pos.getMinY();
		final int z = pos.getMinZ();
		boolean ready = true;
		for (final ValidLocationSetType<?> dependency : type.dependencies()) {
			if (!cache.doesLocationSetExist(x, y, z, dependency)) {
				ready = false;
				break;
			}
		}
		if (ready) {
			final Short2ReferenceMap<List<AIPathNode<?>>> contextSensitiveAndOuterEdges = new Short2ReferenceOpenHashMap<>();
			final Short2ReferenceMap<ShortSet> normalEdges = search(type.neighbourGetter(), contextSensitiveAndOuterEdges);
			if (normalEdges.isEmpty()) {
				output = null;
				return;
			}
			final ShortSet[] components = stronglyConnectedComponents(normalEdges);
			final ChunkSectionRegion[] regions = new ChunkSectionRegion[components.length];
			final int minId = section.getNextRegionId();
			for (int i = 0; i < components.length; i++) {
				if (components[i].size() <= 8) {
					regions[i] = new ChunkSectionSmallRegionImpl(minId + i, components[i].toShortArray());
				} else {
					regions[i] = new ChunkSectionBigRegionImpl(minId + i, components[i]);
				}
			}
			section.setNextRegionId(minId + components.length + 1);
			output = new ChunkSectionRegionsImpl(regions);
		}
	}

	private ShortSet[] stronglyConnectedComponents(final Short2ReferenceMap<ShortSet> edges) {
		final Short2ReferenceMap<Marker> markers = new Short2ReferenceOpenHashMap<>(edges.size());
		final ShortIterator iterator = edges.keySet().iterator();
		while (iterator.hasNext()) {
			final short key = iterator.nextShort();
			markers.put(key, new Marker(key));
		}
		final ObjectArrayList<Marker> s = new ObjectArrayList<>();
		final ObjectArrayList<Marker> p = new ObjectArrayList<>();
		for (final Marker marker : markers.values()) {
			if (marker.order == -1) {
				stronglyConnectedComponentsInner(marker, markers, edges, s, p);
			}
		}
		final Int2ReferenceMap<ShortSet> sets = new Int2ReferenceOpenHashMap<>();
		for (final Marker marker : markers.values()) {
			sets.computeIfAbsent(marker.order, i -> new ShortOpenHashSet()).add(marker.packed);
		}
		return sets.values().toArray(ShortSet[]::new);
	}

	private void stronglyConnectedComponentsInner(final Marker marker, final Short2ReferenceMap<Marker> markers, final Short2ReferenceMap<ShortSet> edges, final ObjectArrayList<Marker> s, final ObjectArrayList<Marker> p) {
		s.push(marker);
		p.push(marker);
		marker.order = count;
		count++;
		final ShortSet shorts = edges.get(marker.packed);
		if (shorts != null) {
			final ShortIterator iterator = shorts.iterator();
			while (iterator.hasNext()) {
				final short packed = iterator.nextShort();
				final Marker nextMarker = markers.get(packed);
				if (nextMarker.order == -1) {
					stronglyConnectedComponentsInner(nextMarker, markers, edges, s, p);
				} else if (!nextMarker.done) {
					while (nextMarker.order < p.top().order) {
						p.pop();
					}
				}
			}
		}
		if (marker == p.top()) {
			if (!marker.done) {
				marker.order = order;
				marker.done = true;
			}
			while (marker != s.top()) {
				final Marker top = s.pop();
				if (!top.done) {
					top.order = order;
					top.done = true;
				}
			}
			order++;
			p.pop();
		}
	}

	private static final class Marker {
		private final short packed;
		private int order = -1;
		private boolean done = false;

		private Marker(final short packed) {
			this.packed = packed;
		}
	}

	private <T, N extends AIPathNode<T>> Short2ReferenceMap<ShortSet> search(final NeighbourGetter<T, N> getter, final Short2ReferenceMap<List<AIPathNode<?>>> contextSensitiveAndOuterEdges) {
		final int x = pos.getMinX();
		final int y = pos.getMinY();
		final int z = pos.getMinZ();
		final Object[] successors = new Object[NeighbourGetter.MAX_SUCCESSORS];
		final Short2ReferenceMap<N> nodes = new Short2ReferenceOpenHashMap<>();
		final ShortPriorityQueue queue = new ShortArrayFIFOQueue();
		final Short2ReferenceMap<ShortSet> edges = new Short2ReferenceOpenHashMap<>();
		while (true) {
			findNextOpen(nodes, queue, getter);
			if (queue.isEmpty()) {
				break;
			}
			while (!queue.isEmpty()) {
				final short key = queue.dequeueShort();
				final N node = nodes.get(key);
				final int neighbours = getter.getNeighbours(cache, node, successors);
				for (int i = 0; i < neighbours; i++) {
					final N successor = (N) successors[i];
					if (successor.contextSensitive || !ChunkSectionRegion.isLocal(successor.x - x, successor.y - y, successor.z - z)) {
						contextSensitiveAndOuterEdges.computeIfAbsent(key, k -> new ArrayList<>()).add(successor);
						continue;
					}
					final short packed = ChunkSectionRegion.packLocal(successor.x - x, successor.y - y, successor.z - z);
					if (nodes.putIfAbsent(packed, successor) == null) {
						queue.enqueue(packed);
					}
					edges.computeIfAbsent(packed, s -> new ShortOpenHashSet());
					edges.computeIfAbsent(key, s -> new ShortOpenHashSet()).add(packed);
				}
			}
		}
		return edges;
	}

	private <T, N extends AIPathNode<T>> void findNextOpen(final Short2ReferenceMap<N> occupied, final ShortPriorityQueue queue, final NeighbourGetter<T, N> neighbourGetter) {
		final int x = pos.getMinX();
		final int y = pos.getMinY();
		final int z = pos.getMinZ();
		for (; lastX < 16; lastX++) {
			for (; lastY < 16; lastY++) {
				for (; lastZ < 16; lastZ++) {
					final short key = ChunkSectionRegion.packLocal(lastX, lastY, lastZ);
					if (!occupied.containsKey(key)) {
						final N startNode = neighbourGetter.createStartNode(cache, x + lastX, y + lastY, z + lastZ);
						if (startNode != null) {
							occupied.put(key, startNode);
							queue.enqueue(key);
							return;
						}
					}
				}
				lastZ = 0;
			}
			lastY = 0;
		}
	}

	@Override
	public void runFinish() {
		if (finished) {
			throw new RuntimeException("Tried to call runFinish twice!");
		}
		if (output != null) {
			if (modCount == currentModCount.getAsLong()) {
				completionConsumer.accept(output);
				finished = true;
			}
		}
	}
}
