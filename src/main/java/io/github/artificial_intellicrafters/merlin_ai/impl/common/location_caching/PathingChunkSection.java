package io.github.artificial_intellicrafters.merlin_ai.impl.common.location_caching;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.util.WorldCache;
import net.minecraft.util.math.ChunkSectionPos;

public interface PathingChunkSection {
	<T> ValidLocationSet<T> vaa$getValidLocationSet(ValidLocationSetType<T> type, ChunkSectionPos pos, WorldCache world);

	<T> ValidLocationSet<T> vaa$getValidLocationSet(ValidLocationSetType<T> type, int x, int y, int z, WorldCache world);
}
