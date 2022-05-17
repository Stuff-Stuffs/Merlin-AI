package io.github.artificial_intellicrafters.merlin_ai.impl.common.region;

import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegion;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortIterator;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.util.math.BlockPos;

public class ChunkSectionBigRegionImpl<T> implements ChunkSectionRegion<T> {
	private final int id;
	private final ShortSet set;
	private final LongSet normalOutgoingEdges;
	private final AIPathNode<T>[] contextSensitiveEdges;

	public ChunkSectionBigRegionImpl(final int id, final ShortSet positions, final LongSet normalOutgoingEdges, final AIPathNode<T>[] contextSensitiveEdges) {
		this.id = id;
		set = positions;
		this.normalOutgoingEdges = normalOutgoingEdges;
		this.contextSensitiveEdges = contextSensitiveEdges;
	}

	@Override
	public int id() {
		return id;
	}

	@Override
	public boolean contains(final int x, final int y, final int z) {
		final short local = ChunkSectionRegion.packLocal(x & 15, y & 15, z & 15);
		return set.contains(local);
	}

	@Override
	public void forEach(final ForEach action) {
		final ShortIterator iterator = set.iterator();
		while (iterator.hasNext()) {
			final short local = iterator.nextShort();
			action.accept(ChunkSectionRegion.unpackLocalX(local), ChunkSectionRegion.unpackLocalY(local), ChunkSectionRegion.unpackLocalZ(local));
		}
	}

	@Override
	public LongSet getOutgoingEdges(final T context) {
		final LongSet set = new LongOpenHashSet(normalOutgoingEdges);
		for (final AIPathNode<T> contextSensitiveEdge : contextSensitiveEdges) {
			if (contextSensitiveEdge.linkPredicate.test(context)) {
				set.add(BlockPos.asLong(contextSensitiveEdge.x, contextSensitiveEdge.y, contextSensitiveEdge.z));
			}
		}
		return set;
	}
}
