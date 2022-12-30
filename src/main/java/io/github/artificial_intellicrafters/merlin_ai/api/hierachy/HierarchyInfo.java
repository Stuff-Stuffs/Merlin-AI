package io.github.artificial_intellicrafters.merlin_ai.api.hierachy;

import com.mojang.datafixers.util.Pair;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutionContext;
import io.github.artificial_intellicrafters.merlin_ai.api.util.OrablePredicate;
import io.github.artificial_intellicrafters.merlin_ai.api.util.ShapeCache;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;

public interface HierarchyInfo<T, N, C, O extends OrablePredicate<N, O>> {
	ValidLocationSetType<T> validLocationSetType();

	Class<N> pathContextClass();

	Pair<ChunkSectionRegions, C> regionify(ShapeCache shapeCache, ChunkSectionPos pos, ValidLocationSetType<T> type, HeightLimitView limitView, AITaskExecutionContext executionContext);

	ChunkSectionRegionConnectivityGraph<N> link(C precomputed, ShapeCache shapeCache, ChunkSectionPos pos, ChunkSectionRegions regions, AITaskExecutionContext executionContext);
}
