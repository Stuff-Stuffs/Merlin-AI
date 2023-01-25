package io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test;

import java.util.List;

public final class AIPath<T, N extends AIPathNode<T, N>> {
	private final Object[] nodes;
	private int index = 0;

	public AIPath(final List<N> nodes) {
		this.nodes = nodes.toArray();
	}

	public boolean isFinished() {
		return index >= nodes.length;
	}

	public void next() {
		index++;
		while (!isFinished() && nodes[index] == null) {
			index++;
		}
	}

	public N getCurrent() {
		if (!isFinished()) {
			return (N) nodes[index];
		}
		return (N) nodes[nodes.length - 1];
	}

	@Deprecated
	public Object[] getNodes() {
		return nodes;
	}
}
