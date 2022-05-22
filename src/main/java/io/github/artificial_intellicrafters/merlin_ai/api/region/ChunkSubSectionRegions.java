package io.github.artificial_intellicrafters.merlin_ai.api.region;

import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.util.SubChunkSectionUtil;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.BlockPos;

public interface ChunkSubSectionRegions<T, N extends AIPathNode<T, N>> {
	int getContainingRegion(int x, int y, int z);

	boolean isValidRegion(int regionId);

	LongSet getOutgoingEdges(int regionId, T context, N previousNode);

	ChunkSectionRegionType<T, N> type();

	static long getRepresentativeBlockPos(int x, int y, int z) {
		final int offX = (SubChunkSectionUtil.blockToSubSection(x) & 1) == 0 ? 1 : 2;
		final int offY = (SubChunkSectionUtil.blockToSubSection(y) & 1) == 0 ? 1 : 2;
		final int offZ = (SubChunkSectionUtil.blockToSubSection(z) & 1) == 0 ? 1 : 2;
		x = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.blockToSubSection(x));
		y = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.blockToSubSection(y));
		z = SubChunkSectionUtil.subSectionToBlock(SubChunkSectionUtil.blockToSubSection(z));
		return BlockPos.asLong(x + offX, y + offY, z + offZ);
	}

	static boolean isLocal(final int x, final int y, final int z) {
		return x >= 0 && x < 8 && y >= 0 && y < 8 && z >= 0 && z < 8;
	}

	static short packLocal(final int x, final int y, final int z) {
		if (!isLocal(x, y, z)) {
			throw new RuntimeException("Local coordinates must be between 0 and 15 inclusive");
		}
		return (short) ((x << 6) | (y << 3) | z);
	}

	static int unpackLocalX(final short packed) {
		return (packed >> 6) & 0x7;
	}

	static int unpackLocalY(final short packed) {
		return (packed >> 3) & 0x7;
	}

	static int unpackLocalZ(final short packed) {
		return packed & 0x7;
	}
}
