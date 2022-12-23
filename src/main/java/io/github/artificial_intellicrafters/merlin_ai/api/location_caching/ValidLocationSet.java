package io.github.artificial_intellicrafters.merlin_ai.api.location_caching;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import io.github.artificial_intellicrafters.merlin_ai.impl.common.PathingChunkSection;
import net.minecraft.util.math.ChunkSectionPos;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface ValidLocationSet<T> {
	ValidLocationSetType<T> type();

	long revision();

	T get(final int x, final int y, final int z);

	long size();

	Either<ValidLocationSet<T>, Pair<T[], int[]>> rebuild(final ChunkSectionPos sectionPos, final ShapeCache cache, final PathingChunkSection[] region, final long[] modCounts);
}
