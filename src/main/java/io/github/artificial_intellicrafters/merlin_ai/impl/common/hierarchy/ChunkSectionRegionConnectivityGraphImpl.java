package io.github.artificial_intellicrafters.merlin_ai.impl.common.hierarchy;

import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegionConnectivityGraph;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.util.OrablePredicate;
import it.unimi.dsi.fastutil.longs.*;

public class ChunkSectionRegionConnectivityGraphImpl<N, T extends OrablePredicate<N, T>> implements ChunkSectionRegionConnectivityGraph<N> {
	private static final long[] EMPTY = new long[0];
	private final Long2ObjectMap<long[]> links;
	private final Long2ObjectMap<ConditionalLinks<T>> conditionalLinks;

	public ChunkSectionRegionConnectivityGraphImpl(final Long2ObjectMap<long[]> links, final Long2ObjectMap<ConditionalLinks<T>> conditionalLinks) {
		this.links = links;
		this.conditionalLinks = conditionalLinks;
	}

	@Override
	public LongIterator unconditionalLinks(final long id) {
		return LongIterators.wrap(links.getOrDefault(id, EMPTY));
	}

	@Override
	public LongIterator conditionalLinks(final long id, final N pathContext) {
		final ConditionalLinks<T> conditionalLink = conditionalLinks.get(id);
		if (conditionalLink == null) {
			return LongIterators.EMPTY_ITERATOR;
		}
		final long[] scratch = new long[conditionalLink.links.length];
		int length = 0;
		for (int i = 0; i < conditionalLink.links.length; i++) {
			if (((T) conditionalLink.conditions[i]).test(pathContext)) {
				scratch[length] = conditionalLink.links[i];
				length++;
			}
		}
		if (length == 0) {
			return LongIterators.EMPTY_ITERATOR;
		}
		return LongIterators.wrap(scratch, 0, length);
	}

	private record ConditionalLinks<T>(long[] links, Object[] conditions) {
	}

	private static final class PartialConditionalLinks<N, T extends OrablePredicate<N, T>> {
		private final Long2ObjectMap<T> conditionalLinks = new Long2ObjectOpenHashMap<>();
	}

	public static final class BuilderImpl<N, T extends OrablePredicate<N, T>> implements Builder<N, T> {
		private final ChunkSectionRegions regions;
		private final Long2ObjectMap<LongSet> links = new Long2ObjectOpenHashMap<>();
		private final Long2ObjectMap<PartialConditionalLinks<N, T>> conditionalLinks = new Long2ObjectOpenHashMap<>();

		public BuilderImpl(final ChunkSectionRegions regions) {
			this.regions = regions;
		}

		@Override
		public void addLink(final long from, final long to) {
			if (regions.byId(from) != null) {
				links.computeIfAbsent(from, s -> new LongOpenHashSet(8)).add(to);
			} else {
				throw new RuntimeException();
			}
		}

		@Override
		public void addConditionalLink(final long from, final long to, final T contextPredicate) {
			if (regions.byId(from) != null) {
				final T old = conditionalLinks.computeIfAbsent(from, s -> new PartialConditionalLinks<>()).conditionalLinks.put(to, contextPredicate);
				if (old != null) {
					conditionalLinks.get(from).conditionalLinks.put(to, contextPredicate.or(old));
				}
			} else {
				throw new RuntimeException();
			}
		}

		@Override
		public ChunkSectionRegionConnectivityGraph<N> build() {
			final Long2ObjectMap<long[]> links = new Long2ObjectOpenHashMap<>(this.links.size());
			final Long2ObjectMap<ConditionalLinks<T>> conditionalLinks = new Long2ObjectOpenHashMap<>(this.conditionalLinks.size());
			for (final Long2ObjectMap.Entry<LongSet> entry : this.links.long2ObjectEntrySet()) {
				links.put(entry.getLongKey(), entry.getValue().toLongArray());
			}
			for (final Long2ObjectMap.Entry<PartialConditionalLinks<N, T>> entry : this.conditionalLinks.long2ObjectEntrySet()) {
				final int size = entry.getValue().conditionalLinks.size();
				final long[] condLinks = new long[size];
				final Object[] condPreds = new Object[size];
				int i = 0;
				for (final Long2ObjectMap.Entry<T> en : entry.getValue().conditionalLinks.long2ObjectEntrySet()) {
					condLinks[i] = en.getLongKey();
					condPreds[i++] = en.getValue();
				}
				conditionalLinks.put(entry.getLongKey(), new ConditionalLinks<>(condLinks, condPreds));
			}
			return new ChunkSectionRegionConnectivityGraphImpl<>(links, conditionalLinks);
		}
	}
}
