package io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test;

public class AIPathNode {
	public final int x;
	public final int y;
	public final int z;
	public int nodeCount = 0;
	public double distToTarget = Float.MAX_VALUE;
	public double distance;
	public final Type type;
	public AIPathNode previous;
	public AIPathNode next;
	public AIPathNode sibling;
	public final boolean walkable;

	public AIPathNode(final int x, final int y, final int z, final double distance, final Type type, final AIPathNode previous, final boolean walkable) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.distance = distance;
		this.type = type;
		this.previous = previous;
		this.walkable = walkable;
	}

	public enum Type {
		LAND,
		AIR,
		LIQUID
	}
}
