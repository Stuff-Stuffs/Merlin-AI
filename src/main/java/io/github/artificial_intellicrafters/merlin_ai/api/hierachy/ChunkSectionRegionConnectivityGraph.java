package io.github.artificial_intellicrafters.merlin_ai.api.hierachy;

import io.github.artificial_intellicrafters.merlin_ai.api.util.OrablePredicate;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.hierarchy.ChunkSectionRegionConnectivityGraphImpl;
import it.unimi.dsi.fastutil.longs.LongIterator;

public interface ChunkSectionRegionConnectivityGraph<N> {
	LongIterator unconditionalLinks(long id);

	LongIterator conditionalLinks(long id, N pathContext);

	boolean unconditionalAdjacencyTo(long start, long query);

	boolean conditionalAdjacencyTo(long start, long query, N pathContext);

	static <N, O extends OrablePredicate<N, O>> Builder<N, O> builder(final HierarchyInfo<?, N, ?, O> info, final ChunkSectionRegions regions) {
		return new ChunkSectionRegionConnectivityGraphImpl.BuilderImpl<>(regions);
	}

	interface Builder<N, O extends OrablePredicate<N, O>> {
		RegionBuilder<N, O> region(long region);

		ChunkSectionRegionConnectivityGraph<N> build();
	}

	interface RegionBuilder<N, O extends OrablePredicate<N, O>> {
		void addLink(long to);

		void addConditionalLink(long to, O contextPredicate);
	}
}
