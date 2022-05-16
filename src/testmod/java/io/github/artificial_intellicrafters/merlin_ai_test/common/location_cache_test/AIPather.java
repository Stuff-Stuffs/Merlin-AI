package io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test;

import io.github.artificial_intellicrafters.merlin_ai.api.util.PathingHeapQueue;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.QuiltLoader;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;

public class AIPather {
	private static final boolean DEBUG = QuiltLoader.isDevelopmentEnvironment();
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
		if (DEBUG) {
			final StopWatch stopWatch = StopWatch.createStarted();
			final PathInfo info = find(pathTarget, max, partial);
			stopWatch.stop();
			final double v = stopWatch.getTime(TimeUnit.NANOSECONDS) / 1_000_000D;
			System.out.println("Time: " + v);
			System.out.println("Nodes considered: " + info.nodesConsidered());
			System.out.println("Nodes/Second: " + (info.nodesConsidered() / (v / 1000)));
			return info.path;
		} else {
			return find(pathTarget, max, partial).path();
		}
	}

	private PathInfo find(final PathTarget pathTarget, final double max, final boolean partial) {
		final ShapeCache cache = ShapeCache.create(world, aiEntity.getBlockPos().add(-256, -256, -256), aiEntity.getBlockPos().add(256, 256, 256));
		final AIPathNode start = nodeProducer.getStart(cache);
		//Heuristic must be below this value to be considered the end
		final double err = pathTarget.getRadius();
		//We need a specialized heap implementation so that we can remove object in the heap, not just the top
		final PathingHeapQueue<AIPathNode> queue = new PathingHeapQueue<>(Comparator.comparingDouble(i -> i.distToTarget + i.distance));
		final Long2ReferenceMap<AIPathNode> visited = new Long2ReferenceOpenHashMap<>();
		double bestDist = Double.POSITIVE_INFINITY;
		AIPathNode best = null;
		start.distToTarget = pathTarget.heuristic(start.x, start.y, start.z);
		queue.enqueue(start);
		visited.put(BlockPos.asLong(start.x, start.y, start.z), start);
		//While there is more nodes to visit
		while (!queue.isEmpty()) {
			final AIPathNode current = queue.dequeue();
			//Check if the node is too far away
			if (current.distance > max) {
				continue;
			}
			//Is the node the best node so far
			if (current.distToTarget < bestDist) {
				bestDist = current.distToTarget;
				best = current;
			}
			if (current.previous != null) {
				current.nodeCount = current.previous.nodeCount + 1;
			} else {
				current.nodeCount = 1;
			}
			//Is the node at the goal
			if (pathTarget.heuristic(current.x, current.y, current.z) < err) {
				return new PathInfo(visited.size(), toPath(current));
			}
			//Get adjacent nodes, fill the array with them, return how many neighbours were found
			final int count = nodeProducer.getNeighbours(current, successors);
			//The last node in the linked list formed by AIPathNode.sibling, this is the list of nodes directly after the current one
			AIPathNode sibling = current.next;
			//If sibling is null we don't need to find the end of the linked list, as it doesn't exist
			while (sibling != null) {
				if (sibling.sibling != null) {
					sibling = sibling.sibling;
				} else {
					break;
				}
			}
			//For each neighbour found
			for (int i = 0; i < count; i++) {
				final AIPathNode next = successors[i];
				final long pos = BlockPos.asLong(next.x, next.y, next.z);
				//Will return null if this is the first time we see it
				final AIPathNode node = visited.putIfAbsent(pos, next);
				if (node == null) {
					if (sibling == null) {
						//If the current node has no next node, put the current neighbour as the first in the linked list
						current.next = next;
					} else {
						//If the current node has a next node, put the current neighbour at the end of the linked list of next nodes
						sibling.sibling = next;
					}
					//The last node in the linked list is now the current neighbour
					sibling = next;
					//Update the heuristic
					next.distToTarget = pathTarget.heuristic(next.x, next.y, next.z);
					queue.enqueue(next);
				} else {
					//We check if this node faster to get to than the currently existing one,  I add a small constant because sometimes a path is every so slightly short(example 0.0001 shorter path).
					//This is not worth the computation time to consider
					if (next.distance + 0.1 < node.distance) {
						//This node is better, replace the current one at this position
						visited.put(pos, next);
						if (sibling == null) {
							//If the current node has no next node, put the current neighbour as the first in the linked list
							current.next = next;
						} else {
							//If the current node has a next node, put the current neighbour at the end of the linked list of next nodes
							sibling.sibling = next;
						}
						sibling = next;
						//Find the node before the old node that we replaced
						final AIPathNode previous = node.previous;
						//This should only be null if we are replacing the root node, which shouldn't happen.
						if (previous != null) {
							//if the node getting replaced is the head of the linked list, simply replace it
							if (previous.next == node) {
								previous.next = node.sibling;
							} else {
								//Otherwise, walk the linked list until the node getting replaced is found
								AIPathNode cursor = previous.next;
								while (cursor.sibling != node) {
									cursor = cursor.sibling;
								}
								//Remove the node
								cursor.sibling = cursor.sibling.sibling;
							}
						}
						//update the heuristic
						next.distToTarget = pathTarget.heuristic(next.x, next.y, next.z);
						//Remove the old node from the heap
						queue.removeFirstReference(node);
						//Re-queue the node as its distance has changed;
						queue.enqueue(next);
					}
				}
			}
		}
		return new PathInfo(visited.size(), partial && best != null ? toPath(best) : null);
	}

	private static AIPath toPath(AIPathNode node) {
		final AIPathNode[] nodes = new AIPathNode[node.nodeCount];
		for (int i = nodes.length - 1; i >= 0; i--) {
			nodes[i] = node;
			node = node.previous;
		}
		return new AIPath(nodes);
	}

	private record PathInfo(int nodesConsidered, @Nullable AIPath path) {
	}
}
