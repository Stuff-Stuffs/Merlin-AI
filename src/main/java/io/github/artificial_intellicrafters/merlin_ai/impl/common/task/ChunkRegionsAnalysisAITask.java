package io.github.artificial_intellicrafters.merlin_ai.impl.common.task;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.path.NeighbourGetter;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegion;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITask;
import io.github.artificial_intellicrafters.merlin_ai.api.util.AStar;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.region.ChunkSectionBigRegionImpl;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.region.ChunkSectionRegionsImpl;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.region.graph.ChunkRegionGraphImpl;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.Arrays;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ChunkRegionsAnalysisAITask implements AITask {
	private final BooleanSupplier shouldContinue;
	private final ChunkSectionRegionType<?, ?> type;
	private final ChunkSectionPos pos;
	private final ChunkRegionGraphImpl.EntryImpl entry;
	private final Supplier<ShapeCache> cacheFactory;
	private final Consumer<ChunkSectionRegions<?, ?>> completionConsumer;
	private ChunkSectionRegions<?, ?> output = null;
	private boolean finished = false;
	private int lastX = 0;
	private int lastY = 0;
	private int lastZ = 0;
	private int count = 1;
	private int order = 1;

	public ChunkRegionsAnalysisAITask(final BooleanSupplier shouldContinue, final ChunkSectionRegionType<?, ?> type, final ChunkSectionPos pos, final ChunkRegionGraphImpl.EntryImpl entry, final Supplier<ShapeCache> cacheFactory, final Consumer<ChunkSectionRegions<?, ?>> completionConsumer) {
		this.shouldContinue = shouldContinue;
		this.type = type;
		this.pos = pos;
		this.entry = entry;
		this.cacheFactory = cacheFactory;
		this.completionConsumer = completionConsumer;
	}

	@Override
	public int priority() {
		return AITask.super.priority() + 1;
	}

	@Override
	public boolean done() {
		return output != null || !shouldContinue.getAsBoolean();
	}

	@Override
	public void runIteration() {
		final int x = pos.getMinX();
		final int y = pos.getMinY();
		final int z = pos.getMinZ();
		boolean ready = true;
		final ShapeCache cache = cacheFactory.get();
		for (final ValidLocationSetType<?> dependency : type.dependencies()) {
			if (!cache.doesLocationSetExist(x, y, z, dependency)) {
				ready = false;
			}
		}
		if (ready) {
			regionify(type.neighbourGetter(), cache);
		}
	}

	private <T, N extends AIPathNode<T, N>> void regionify(final NeighbourGetter<T, N> neighbourGetter, final ShapeCache cache) {
		final int x = pos.getMinX();
		final int y = pos.getMinY();
		final int z = pos.getMinZ();
		final Short2ReferenceMap<LongSet> outerEdges = new Short2ReferenceOpenHashMap<>();
		final Short2ReferenceMap<ShortSet> normalEdges = search(neighbourGetter, outerEdges, cache);
		if (normalEdges.isEmpty()) {
			output = new ChunkSectionRegionsImpl<T, N>(type, new ChunkSectionRegion[0]);
			return;
		}
		final Short2IntMap categorized = new Short2IntOpenHashMap(normalEdges.size());
		final Int2ReferenceMap<ShortSet> components = stronglyConnectedComponents(normalEdges, categorized);

		final Int2ReferenceMap<LongSet> normalOutgoingRegionEdges = new Int2ReferenceOpenHashMap<>(components.size());
		for (final Short2ReferenceMap.Entry<ShortSet> entry : normalEdges.short2ReferenceEntrySet()) {
			final short packed = entry.getShortKey();
			final int containingIndex = categorized.get(packed);
			final ShortIterator iterator = entry.getValue().iterator();
			LongSet longs = normalOutgoingRegionEdges.get(containingIndex);
			while (iterator.hasNext()) {
				final short edge = iterator.nextShort();
				if (categorized.get(edge) != containingIndex) {
					if (longs == null) {
						longs = new LongOpenHashSet();
						normalOutgoingRegionEdges.put(containingIndex, longs);
					}
					longs.add(BlockPos.asLong(x + ChunkSectionRegion.unpackLocalX(edge), y + ChunkSectionRegion.unpackLocalY(edge), z + ChunkSectionRegion.unpackLocalZ(edge)));
				}
			}
		}

		for (final Short2ReferenceMap.Entry<LongSet> entry : outerEdges.short2ReferenceEntrySet()) {
			final int containingIndex = categorized.get(entry.getShortKey());
			normalOutgoingRegionEdges.computeIfAbsent(containingIndex, i -> new LongOpenHashSet()).addAll(entry.getValue());
		}

		final ChunkSectionRegion<T, N>[] regions = new ChunkSectionRegion[components.size()];
		final int minId = entry.getNextRegionId();
		int i = 0;
		for (final Int2ReferenceMap.Entry<ShortSet> entry : components.int2ReferenceEntrySet()) {
			final int key = entry.getIntKey();
			final ShortSet shorts = entry.getValue();
			LongSet normalOutgoingEdges = normalOutgoingRegionEdges.get(key);
			if (normalOutgoingEdges == null) {
				normalOutgoingEdges = LongSets.emptySet();
			}
			final short[] positions = shorts.toShortArray();
			Arrays.sort(positions);
			regions[i++] = new ChunkSectionBigRegionImpl<>(minId + i, positions, normalOutgoingEdges);
		}
		entry.setNextRegionId(minId + components.size() + 1);
		output = new ChunkSectionRegionsImpl<>(type, regions);
	}

	private Int2ReferenceMap<ShortSet> stronglyConnectedComponents(final Short2ReferenceMap<ShortSet> edges, final Short2IntMap categorized) {
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
			categorized.put(marker.packed, marker.order);
		}
		return sets;
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

	private <T, N extends AIPathNode<T, N>> Short2ReferenceMap<ShortSet> search(final NeighbourGetter<T, N> getter, final Short2ReferenceMap<LongSet> outerEdges, final ShapeCache cache) {
		final int x = pos.getMinX();
		final int y = pos.getMinY();
		final int z = pos.getMinZ();
		final Object[] successors = new Object[AStar.MAX_SUCCESSORS];
		final Short2ReferenceMap<N> nodes = new Short2ReferenceOpenHashMap<>();
		final ShortPriorityQueue queue = new ShortArrayFIFOQueue();
		final Short2ReferenceMap<ShortSet> edges = new Short2ReferenceOpenHashMap<>();
		while (true) {
			findNextOpen(nodes, queue, getter, cache);
			if (queue.isEmpty()) {
				break;
			}
			while (!queue.isEmpty()) {
				final short key = queue.dequeueShort();
				final N node = nodes.get(key);
				final int neighbours = getter.getNeighbours(cache, node, i -> Double.POSITIVE_INFINITY, successors);
				ShortSet shorts = edges.get(key);
				for (int i = 0; i < neighbours; i++) {
					final N successor = (N) successors[i];
					if (!ChunkSectionRegion.isLocal(successor.x - x, successor.y - y, successor.z - z)) {
						outerEdges.computeIfAbsent(key, k -> new LongOpenHashSet()).add(BlockPos.asLong(successor.x, successor.y, successor.z));
						continue;
					}
					final short packed = ChunkSectionRegion.packLocal(successor.x - x, successor.y - y, successor.z - z);
					if (nodes.putIfAbsent(packed, successor) == null) {
						queue.enqueue(packed);
					}
					edges.computeIfAbsent(packed, s -> new ShortOpenHashSet());
					if (shorts == null) {
						shorts = new ShortOpenHashSet();
						edges.put(key, shorts);
					}
					shorts.add(packed);
				}
			}
		}
		return edges;
	}

	private <T, N extends AIPathNode<T, N>> void findNextOpen(final Short2ReferenceMap<N> occupied, final ShortPriorityQueue queue, final NeighbourGetter<T, N> neighbourGetter, final ShapeCache cache) {
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
			completionConsumer.accept(output);
			finished = true;
		}
	}

	@Override
	public String toString() {
		return "ChunkRegionsAnalysisAITask{" + "pos=" + pos + '}';
	}
}
