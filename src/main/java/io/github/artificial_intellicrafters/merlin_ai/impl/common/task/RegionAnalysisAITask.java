package io.github.artificial_intellicrafters.merlin_ai.impl.common.task;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.path.NeighbourGetter;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITask;
import io.github.artificial_intellicrafters.merlin_ai.api.util.AStar;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.api.util.SubChunkSectionUtil;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.region.ChunkSectionRegionsImpl;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.*;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RegionAnalysisAITask implements AITask {
	private final BooleanSupplier shouldContinue;
	private final ChunkSectionRegionType<?, ?> type;
	private final long pos;
	private final Supplier<ShapeCache> cacheFactory;
	private final Consumer<ChunkSectionRegions<?, ?>> completionConsumer;
	private ChunkSectionRegions<?, ?> output = null;
	private boolean finished = false;
	private int lastX = 0;
	private int lastY = 0;
	private int lastZ = 0;
	private int count = 1;
	private int order = 0;

	public RegionAnalysisAITask(final BooleanSupplier shouldContinue, final ChunkSectionRegionType<?, ?> type, final long pos, final Supplier<ShapeCache> cacheFactory, final Consumer<ChunkSectionRegions<?, ?>> completionConsumer) {
		this.shouldContinue = shouldContinue;
		this.type = type;
		this.pos = pos;
		this.cacheFactory = cacheFactory;
		this.completionConsumer = completionConsumer;
	}

	@Override
	public int priority() {
		return AITask.super.priority() + 1;
	}

	@Override
	public boolean done() {
		//done or chunk has changed since task submission
		return output != null || !shouldContinue.getAsBoolean();
	}

	@Override
	public void runIteration() {
		final int x = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackX(pos));
		final int y = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackY(pos));
		final int z = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackZ(pos));
		boolean ready = true;
		final ShapeCache cache = cacheFactory.get();
		for (final ValidLocationSetType<?> dependency : type.dependencies()) {
			if (!cache.doesLocationSetExist(x, y, z, dependency)) {
				ready = false;
			}
		}
		if (ready) {
			regionify(type, cache);
		}
	}

	private <T, N extends AIPathNode<T, N>> void regionify(final ChunkSectionRegionType<T, N> type, final ShapeCache cache) {
		final int x = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackX(pos));
		final int y = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackY(pos));
		final int z = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackZ(pos));
		final Short2ReferenceMap<List<N>> contextSensitiveAndOuterEdges = new Short2ReferenceOpenHashMap<>();
		final Short2ReferenceMap<ShortSet> normalEdges = search(type.neighbourGetter(), contextSensitiveAndOuterEdges, cache);

		if (normalEdges.isEmpty()) {
			output = ChunkSectionRegionsImpl.create(type, new ShortSet[0], null, null);
			return;
		}

		final Short2IntMap categorized = new Short2IntOpenHashMap(normalEdges.size());
		final Int2ReferenceMap<ShortSet> map = stronglyConnectedComponents(normalEdges, categorized);
		final ShortSet[] regions = new ShortSet[map.size()];

		for (final Int2ReferenceMap.Entry<ShortSet> entry : map.int2ReferenceEntrySet()) {
			regions[entry.getIntKey()] = entry.getValue();
		}

		final LongSet[] outEdges = new LongSet[map.size()];
		final AIPathNode<T, N>[][] contextSensitiveOutEdges = new AIPathNode[map.size()][];

		for (final Int2ReferenceMap.Entry<ShortSet> entry : map.int2ReferenceEntrySet()) {
			final LongSet set = new LongOpenHashSet();
			final ShortIterator iterator = entry.getValue().iterator();
			final List<N> l = new ArrayList<>();
			while (iterator.hasNext()) {
				final short key = iterator.nextShort();

				final List<N> contextSensitiveOuterEdgeList = contextSensitiveAndOuterEdges.get(key);
				if (contextSensitiveOuterEdgeList != null) {
					for (final N node : contextSensitiveOuterEdgeList) {
						if (node.linkPredicate == null) {
							set.add(
									BlockPos.asLong(
											node.x,
											node.y,
											node.z
									)
							);
						} else {
							l.add(node);
						}
					}
				}

				final ShortSet shorts = normalEdges.get(key);
				if (shorts != null) {
					final int containedIndex = categorized.get(key);
					final ShortIterator edgeIterator = shorts.iterator();
					while (edgeIterator.hasNext()) {
						final short out = edgeIterator.nextShort();
						if (containedIndex != categorized.get(out)) {
							set.add(
									BlockPos.asLong(
											x + SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackX(out)),
											y + SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackY(out)),
											z + SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackZ(out))
									)
							);
						}
					}
				}
			}
			outEdges[entry.getIntKey()] = set;
			contextSensitiveOutEdges[entry.getIntKey()] = l.toArray(AIPathNode[]::new);
		}
		output = ChunkSectionRegionsImpl.create(type, regions, outEdges, contextSensitiveOutEdges);
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

	private <T, N extends AIPathNode<T, N>> Short2ReferenceMap<ShortSet> search(final NeighbourGetter<T, N> getter, final Short2ReferenceMap<List<N>> contextSensitiveAndOuterEdges, final ShapeCache cache) {
		final int x = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackX(pos));
		final int y = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackY(pos));
		final int z = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackZ(pos));
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
					if (successor.linkPredicate != null || !SubChunkSectionUtil.isLocal(successor.x - x, successor.y - y, successor.z - z)) {
						contextSensitiveAndOuterEdges.computeIfAbsent(key, k -> new ArrayList<>()).add(successor);
						continue;
					}
					final short packed = SubChunkSectionUtil.packLocal(successor.x - x, successor.y - y, successor.z - z);
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
		final int x = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackX(pos));
		final int y = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackY(pos));
		final int z = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.unpackZ(pos));
		for (; lastX < SubChunkSectionUtil.SUB_SECTION_SIZE; lastX++) {
			for (; lastY < SubChunkSectionUtil.SUB_SECTION_SIZE; lastY++) {
				for (; lastZ < SubChunkSectionUtil.SUB_SECTION_SIZE; lastZ++) {
					final short key = SubChunkSectionUtil.packLocal(lastX, lastY, lastZ);
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
}
