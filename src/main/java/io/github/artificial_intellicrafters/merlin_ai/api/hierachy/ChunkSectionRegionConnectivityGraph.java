package io.github.artificial_intellicrafters.merlin_ai.api.hierachy;

import io.github.artificial_intellicrafters.merlin_ai.api.util.OrablePredicate;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.hierarchy.ChunkSectionRegionConnectivityGraphImpl;
import it.unimi.dsi.fastutil.longs.LongIterator;

public interface ChunkSectionRegionConnectivityGraph<N> {
	LongIterator unconditionalLinks(long id);

	LongIterator conditionalLinks(long id, N pathContext);

	static <N, O extends OrablePredicate<N, O>> Builder<N, O> builder(final HierarchyInfo<?, N, ?, O> info, final ChunkSectionRegions regions) {
		return new ChunkSectionRegionConnectivityGraphImpl.BuilderImpl<>(regions);
	}

	interface Builder<N, O extends OrablePredicate<N, O>> {
		void addLink(long from, long to);

		void addConditionalLink(long from, long to, O contextPredicate);

		ChunkSectionRegionConnectivityGraph<N> build();
	}
}
