package io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test;

import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.time.StopWatch;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;

public class AIPather {
	private final AIPathNode[] successors = new AIPathNode[64];
	private final Entity aiEntity;
	private final World world;
	private final NodeProducer nodeProducer;

	public AIPather(final Entity aiEntity, final World world, final NodeProducer nodeProducer) {
		this.aiEntity = aiEntity;
		this.world = world;
		this.nodeProducer = nodeProducer;
	}

	public NodeProducer getNodeProducer() {
		return nodeProducer;
	}

	public AIPath calculatePath(final PathTarget pathTarget, final double max, final boolean partial) {
		final ShapeCache cache = ShapeCache.create(world, aiEntity.getBlockPos().add(-256, -256, -256), aiEntity.getBlockPos().add(256, 256, 256));
		final AIPathNode start = nodeProducer.getStart(cache);
		final StopWatch stopWatch = StopWatch.createStarted();
		final double err = pathTarget.getRadius();
		//TODO specialized heap implementation
		final ObjectHeapPriorityQueue<AIPathNode> queue = new ObjectHeapPriorityQueue<>(Comparator.comparingDouble(i -> i.distToTarget + i.distance));
		final Long2ReferenceMap<AIPathNode> visited = new Long2ReferenceOpenHashMap<>();
		double bestDist = Double.POSITIVE_INFINITY;
		AIPathNode best = null;
		start.distToTarget = pathTarget.heuristic(start.x, start.y, start.z);
		queue.enqueue(start);
		visited.put(BlockPos.asLong(start.x, start.y, start.z), start);
		while (!queue.isEmpty()) {
			final AIPathNode current = queue.dequeue();
			if (current.distance > max) {
				continue;
			}
			if (current.distToTarget < bestDist) {
				bestDist = current.distToTarget;
				best = current;
			}
			if (current.previous != null) {
				current.nodeCount = current.previous.nodeCount + 1;
			} else {
				current.nodeCount = 1;
			}
			if (pathTarget.heuristic(current.x, current.y, current.z) < err) {
				stopWatch.stop();
				final double v = stopWatch.getTime(TimeUnit.NANOSECONDS) / 1_000_000D;
				System.out.println("Time: " + v);
				System.out.println("Nodes considered: " + visited.size());
				System.out.println("Nodes/Second: " + (visited.size() / (v / 1000)));
				return toPath(current);
			}
			final int count = nodeProducer.getNeighbours(current, successors);
			AIPathNode prevSibling = current.next;
			while (prevSibling != null) {
				if (prevSibling.sibling != null) {
					prevSibling = prevSibling.sibling;
				} else {
					break;
				}
			}
			for (int i = 0; i < count; i++) {
				final AIPathNode next = successors[i];
				final long pos = BlockPos.asLong(next.x, next.y, next.z);
				final AIPathNode node = visited.putIfAbsent(pos, next);
				if (node == null) {
					if (prevSibling == null) {
						current.next = next;
					} else {
						prevSibling.sibling = next;
					}
					prevSibling = next;
					next.distToTarget = pathTarget.heuristic(next.x, next.y, next.z);
					queue.enqueue(next);
				} else {
					if (next.distance + 0.1 < node.distance) {
						visited.put(pos, next);
						if (prevSibling == null) {
							current.next = next;
						} else {
							prevSibling.sibling = next;
						}
						prevSibling = next;
						final AIPathNode previous = node.previous;
						if (previous != null) {
							if (previous.next == node) {
								previous.next = node.sibling;
							} else {
								AIPathNode cursor = previous.next;
								while (cursor.sibling != node) {
									cursor = cursor.sibling;
								}
								cursor.sibling = cursor.sibling.sibling;
							}
						}
						next.distToTarget = pathTarget.heuristic(next.x, next.y, next.z);
						queue.enqueue(next);
					}
				}
			}
		}
		stopWatch.stop();
		final double v = stopWatch.getTime(TimeUnit.NANOSECONDS) / 1_000_000D;
		System.out.println("Time: " + v);
		System.out.println("Nodes considered: " + visited.size());
		System.out.println("Nodes/Second: " + (visited.size() / (v / 1000)));
		if (partial && best != null) {
			return toPath(best);
		}
		return null;
	}

	private static AIPath toPath(AIPathNode node) {
		final AIPathNode[] nodes = new AIPathNode[node.nodeCount];
		for (int i = nodes.length - 1; i >= 0; i--) {
			nodes[i] = node;
			node = node.previous;
		}
		return new AIPath(nodes);
	}
}
