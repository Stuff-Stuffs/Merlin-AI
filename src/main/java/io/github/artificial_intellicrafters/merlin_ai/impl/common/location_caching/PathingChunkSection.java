package io.github.artificial_intellicrafters.merlin_ai.impl.common.location_caching;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import net.minecraft.util.math.ChunkSectionPos;
import org.jetbrains.annotations.Nullable;

public interface PathingChunkSection {
	<T> @Nullable ValidLocationSet<T> merlin_ai$getValidLocationSet(ValidLocationSetType<T> type, ChunkSectionPos pos, ShapeCache world);

	<T> @Nullable ValidLocationSet<T> merlin_ai$getValidLocationSet(ValidLocationSetType<T> type, int x, int y, int z, ShapeCache world);
}
