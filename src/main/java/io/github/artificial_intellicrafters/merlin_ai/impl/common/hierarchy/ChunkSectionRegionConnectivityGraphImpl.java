package io.github.artificial_intellicrafters.merlin_ai.impl.common.hierarchy;

import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegionConnectivityGraph;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.util.OrablePredicate;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

import java.util.Arrays;

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

	@Override
	public boolean unconditionalAdjacencyTo(final long start, final long query) {
		return Arrays.binarySearch(links.getOrDefault(start, EMPTY), query) >= 0;
	}

	@Override
	public boolean conditionalAdjacencyTo(final long start, final long query, final N pathContext) {
		final ConditionalLinks<T> links = conditionalLinks.get(start);
		if (links == null) {
			return false;
		}
		for (int i = 0; i < links.links.length; i++) {
			if (links.links[i] == query && ((T) links.conditions[i]).test(pathContext)) {
				return true;
			}
		}
		return false;
	}

	private record ConditionalLinks<T>(long[] links, OrablePredicate<?, ?>[] conditions) {
	}

	public static final class BuilderImpl<N, T extends OrablePredicate<N, T>> implements Builder<N, T> {
		private final long prefix;
		private final Short2ObjectMap<RegionBuilderImpl<N, T>> regions;

		public BuilderImpl(final ChunkSectionRegions regions) {
			prefix = regions.prefix();
			this.regions = new Short2ObjectOpenHashMap<>();
		}

		@Override
		public RegionBuilder<N, T> region(final long region) {
			if (region >>> 16 != prefix >>> 16) {
				throw new RuntimeException();
			}
			final short key = (short) (region & 0xFFFFL);
			return regions.computeIfAbsent(key, i -> new RegionBuilderImpl<>());
		}

		@Override
		public ChunkSectionRegionConnectivityGraph<N> build() {
			final Long2ObjectMap<long[]> links = new Long2ObjectOpenHashMap<>(regions.size());
			final Long2ObjectMap<ConditionalLinks<T>> conditionalLinks = new Long2ObjectOpenHashMap<>(regions.size());
			for (final Short2ObjectMap.Entry<RegionBuilderImpl<N, T>> entry : regions.short2ObjectEntrySet()) {
				final long key = (((int) entry.getShortKey()) & 0xFFFF) | prefix;
				links.put(key, entry.getValue().links.toLongArray());
				final int size = entry.getValue().conditionalLinks.size();
				final long[] condLinks = new long[size];
				final OrablePredicate<?, ?>[] condPreds = new OrablePredicate[size];
				int i = 0;
				for (final Long2ObjectMap.Entry<T> en : entry.getValue().conditionalLinks.long2ObjectEntrySet()) {
					condLinks[i] = en.getLongKey();
					condPreds[i++] = en.getValue();
				}
				conditionalLinks.put(key, new ConditionalLinks<>(condLinks, condPreds));
			}
			return new ChunkSectionRegionConnectivityGraphImpl<>(links, conditionalLinks);
		}
	}


	private static final class RegionBuilderImpl<N, T extends OrablePredicate<N, T>> implements RegionBuilder<N, T> {
		private final LongSet links = new LongOpenHashSet();
		private final Long2ObjectMap<T> conditionalLinks = new Long2ObjectOpenHashMap<>();

		@Override
		public void addLink(final long to) {
			links.add(to);
		}

		@Override
		public void addConditionalLink(final long to, final T contextPredicate) {
			final T current = conditionalLinks.put(to, contextPredicate);
			if (current != null) {
				conditionalLinks.put(to, current.or(contextPredicate));
			}
		}
	}
}
