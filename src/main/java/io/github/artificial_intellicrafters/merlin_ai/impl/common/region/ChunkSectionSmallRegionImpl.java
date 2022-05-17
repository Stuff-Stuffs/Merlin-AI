package io.github.artificial_intellicrafters.merlin_ai.impl.common.region;

import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegion;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class ChunkSectionSmallRegionImpl<T, N extends AIPathNode<T, N>> implements ChunkSectionRegion<T, N> {
	private final int id;
	private final short[] packed;
	private final LongSet normalOutgoingEdges;
	private final AIPathNode<T, N>[] contextSensitiveEdges;

	public ChunkSectionSmallRegionImpl(final int id, final short[] packed, final LongSet normalOutgoingEdges, final AIPathNode<T, N>[] contextSensitiveEdges) {
		this.id = id;
		this.packed = packed;
		this.normalOutgoingEdges = normalOutgoingEdges;
		this.contextSensitiveEdges = contextSensitiveEdges;
	}

	@Override
	public int id() {
		return id;
	}

	@Override
	public boolean contains(final int x, final int y, final int z) {
		final short pack = ChunkSectionRegion.packLocal(x & 15, y & 15, z & 15);
		for (final short i : packed) {
			if (pack == i) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void forEach(final ForEach action) {
		for (final short i : packed) {
			action.accept(ChunkSectionRegion.unpackLocalX(i), ChunkSectionRegion.unpackLocalY(i), ChunkSectionRegion.unpackLocalZ(i));
		}
	}

	@Override
	public LongSet getOutgoingEdges(final T context, final List<N> previousNodes) {
		final LongSet set = new LongOpenHashSet(normalOutgoingEdges);
		for (final AIPathNode<T, N> contextSensitiveEdge : contextSensitiveEdges) {
			if (contextSensitiveEdge.linkPredicate == null || contextSensitiveEdge.linkPredicate.test(context, previousNodes)) {
				set.add(BlockPos.asLong(contextSensitiveEdge.x, contextSensitiveEdge.y, contextSensitiveEdge.z));
			}
		}
		return set;
	}
}
