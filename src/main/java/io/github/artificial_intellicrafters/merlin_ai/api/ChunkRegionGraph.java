package io.github.artificial_intellicrafters.merlin_ai.api;

import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegionConnectivityGraph;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.ChunkSectionRegions;
import io.github.artificial_intellicrafters.merlin_ai.api.hierachy.HierarchyInfo;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSet;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.task.AITaskExecutionContext;
import net.minecraft.util.math.ChunkSectionPos;
import org.jetbrains.annotations.Nullable;

public interface ChunkRegionGraph {
	@Nullable Entry getEntry(ChunkSectionPos pos);

	@Nullable Entry getEntry(int x, int y, int z);

	interface Entry {
		<T> @Nullable ValidLocationSet<T> getValidLocationSet(ValidLocationSetType<T> type, long tick, @Nullable AITaskExecutionContext executionContext);

		@Nullable ChunkSectionRegions getRegions(HierarchyInfo<?, ?, ?, ?> info, long tick, @Nullable AITaskExecutionContext executionContext);

		<N> ChunkSectionRegionConnectivityGraph<N> getGraph(HierarchyInfo<?, N, ?, ?> info, long tick, @Nullable AITaskExecutionContext executionContext);
	}
}
