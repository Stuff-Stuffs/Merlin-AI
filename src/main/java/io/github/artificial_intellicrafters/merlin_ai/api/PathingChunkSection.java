package io.github.artificial_intellicrafters.merlin_ai.api;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.path.AIPathNode;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegionType;
import io.github.artificial_intellicrafters.merlin_ai.api.region.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import net.minecraft.util.math.ChunkSectionPos;
import org.jetbrains.annotations.Nullable;

public interface PathingChunkSection {
	<T> @Nullable ValidLocationSet<T> merlin_ai$getValidLocationSet(ValidLocationSetType<T> type, ChunkSectionPos pos, ShapeCache world);

	<T> @Nullable ValidLocationSet<T> merlin_ai$getValidLocationSet(ValidLocationSetType<T> type, int x, int y, int z, ShapeCache world);

	<T, N extends AIPathNode<T,N>> @Nullable ChunkSectionRegions<T,N> merlin_ai$getChunkSectionRegions(ChunkSectionRegionType<T, N> type, int x, int y, int z, ShapeCache world);

	int getNextRegionId();

	void setNextRegionId(int id);
}
