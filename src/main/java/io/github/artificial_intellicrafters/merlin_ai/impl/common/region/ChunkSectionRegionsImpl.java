package io.github.artificial_intellicrafters.merlin_ai.impl.common.region;

import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.util.SubChunkSectionUtil;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.shorts.ShortIterator;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

public abstract class ChunkSectionRegionsImpl<T, N extends AIPathNode<T, N>> implements ChunkSectionRegions<T, N> {
	private final ChunkSectionRegionType<T, N> type;
	private final LongSet[] outEdges;
	private final AIPathNode<T, N>[][] contextSensitiveOutEdges;

	private ChunkSectionRegionsImpl(final ChunkSectionRegionType<T, N> type, final LongSet[] outEdges, final AIPathNode<T, N>[][] contextSensitiveOutEdges) {
		this.type = type;
		this.outEdges = outEdges;
		this.contextSensitiveOutEdges = contextSensitiveOutEdges;
		for (final AIPathNode<T, N>[] nodes : contextSensitiveOutEdges) {
			for (final AIPathNode<T, N> node : nodes) {
				if (node.linkPredicate == null) {
					throw new RuntimeException("Context sensitive link node with null predicate!");
				}
			}
		}
	}

	@Override
	public boolean isValidRegion(final int regionId) {
		return regionId >= 0 && regionId < outEdges.length;
	}

	@Override
	public LongSet getOutgoingEdges(final int regionId, final T context, final N previousNode) {
		if (!isValidRegion(regionId)) {
			return new LongOpenHashSet();
		}
		final LongSet edges = new LongOpenHashSet(outEdges[regionId]);
		for (final AIPathNode<T, N> node : contextSensitiveOutEdges[regionId]) {
			if (node.linkPredicate.test(context, previousNode)) {
				edges.add(BlockPos.asLong(node.x, node.y, node.z));
			}
		}
		return edges;
	}

	@Override
	public ChunkSectionRegionType<T, N> type() {
		return type;
	}

	public static <T, N extends AIPathNode<T, N>> ChunkSectionRegionsImpl<T, N> create(final ChunkSectionRegionType<T, N> type, final ShortSet[] regions, final LongSet[] outEdges, final AIPathNode<T, N>[][] contextSensitiveOutEdges) {
		if (regions.length == 0) {
			return new Empty<>(type);
		} else if (regions.length < (int) Byte.MAX_VALUE - (int) Byte.MIN_VALUE) {
			final byte[] ids = new byte[SubChunkSectionUtil.SUB_SECTION_SIZE * SubChunkSectionUtil.SUB_SECTION_SIZE * SubChunkSectionUtil.SUB_SECTION_SIZE];
			Arrays.fill(ids, Byte.MAX_VALUE);
			for (int i = 0; i < regions.length; i++) {
				final ShortSet region = regions[i];
				final ShortIterator iterator = region.iterator();
				while (iterator.hasNext()) {
					ids[iterator.nextShort()] = (byte) (i + Byte.MIN_VALUE);
				}
			}
			return new Small<>(type, outEdges, contextSensitiveOutEdges, ids);
		} else if (regions.length < SubChunkSectionUtil.SUB_SECTION_SIZE * SubChunkSectionUtil.SUB_SECTION_SIZE * SubChunkSectionUtil.SUB_SECTION_SIZE) {
			final short[] ids = new short[SubChunkSectionUtil.SUB_SECTION_SIZE * SubChunkSectionUtil.SUB_SECTION_SIZE * SubChunkSectionUtil.SUB_SECTION_SIZE];
			Arrays.fill(ids, (short) regions.length);
			for (int i = 0; i < regions.length; i++) {
				final ShortSet region = regions[i];
				final ShortIterator iterator = region.iterator();
				while (iterator.hasNext()) {
					ids[iterator.nextShort()] = (short) i;
				}
			}
			return new Large<>(type, outEdges, contextSensitiveOutEdges, ids);
		} else {
			final LongSet[] outEdgesReordered = new LongSet[outEdges.length];
			final AIPathNode<T, N>[][] contextSensitiveOutEdgesReordered = new AIPathNode[outEdges.length][];
			for (int i = 0; i < regions.length; i++) {
				final ShortSet region = regions[i];
				final short key = region.iterator().nextShort();
				outEdgesReordered[key] = outEdges[i];
				contextSensitiveOutEdgesReordered[key] = contextSensitiveOutEdges[i];
			}
			return new Full<>(type, outEdgesReordered, contextSensitiveOutEdgesReordered);
		}
	}

	private static final class Empty<T, N extends AIPathNode<T, N>> extends ChunkSectionRegionsImpl<T, N> {
		private Empty(final ChunkSectionRegionType<T, N> type) {
			super(type, new LongSet[]{LongSets.emptySet()}, new AIPathNode[1][0]);
		}

		@Override
		public int getContainingRegion(final int x, final int y, final int z) {
			return -1;
		}
	}


	private static final class Small<T, N extends AIPathNode<T, N>> extends ChunkSectionRegionsImpl<T, N> {
		private final byte[] ids;

		private Small(final ChunkSectionRegionType<T, N> type, final LongSet[] outEdges, final AIPathNode<T, N>[][] contextSensitiveOutEdges, final byte[] ids) {
			super(type, outEdges, contextSensitiveOutEdges);
			this.ids = ids;
		}

		@Override
		public int getContainingRegion(final int x, final int y, final int z) {
			return (int) ids[SubChunkSectionUtil.packLocal(x, y, z)] - (int) Byte.MIN_VALUE;
		}
	}

	private static final class Large<T, N extends AIPathNode<T, N>> extends ChunkSectionRegionsImpl<T, N> {
		private final short[] ids;

		private Large(final ChunkSectionRegionType<T, N> type, final LongSet[] outEdges, final AIPathNode<T, N>[][] contextSensitiveOutEdges, final short[] ids) {
			super(type, outEdges, contextSensitiveOutEdges);
			this.ids = ids;
		}

		@Override
		public int getContainingRegion(final int x, final int y, final int z) {
			return ids[SubChunkSectionUtil.packLocal(x, y, z)];
		}
	}

	private static final class Full<T, N extends AIPathNode<T, N>> extends ChunkSectionRegionsImpl<T, N> {
		private Full(final ChunkSectionRegionType<T, N> type, final LongSet[] outEdges, final AIPathNode<T, N>[][] contextSensitiveOutEdges) {
			super(type, outEdges, contextSensitiveOutEdges);
		}

		@Override
		public int getContainingRegion(final int x, final int y, final int z) {
			return SubChunkSectionUtil.packLocal(x, y, z);
		}
	}
}
