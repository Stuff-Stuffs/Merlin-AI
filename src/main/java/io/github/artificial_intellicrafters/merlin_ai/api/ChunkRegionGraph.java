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
		default <T> @Nullable ValidLocationSet<T> getValidLocationSet(final ValidLocationSetType<T> type, final long tick, @Nullable final AITaskExecutionContext executionContext) {
			return getValidLocationSet(type, tick, executionContext, true);
		}

		<T> @Nullable ValidLocationSet<T> getValidLocationSet(ValidLocationSetType<T> type, long tick, @Nullable AITaskExecutionContext executionContext, boolean enqueue);

		default @Nullable ChunkSectionRegions getRegions(final HierarchyInfo<?, ?, ?, ?> info, final long tick, @Nullable final AITaskExecutionContext executionContext) {
			return getRegions(info, tick, executionContext, true);
		}

		@Nullable ChunkSectionRegions getRegions(HierarchyInfo<?, ?, ?, ?> info, long tick, @Nullable AITaskExecutionContext executionContext, boolean enqueue);

		default <N> @Nullable ChunkSectionRegionConnectivityGraph<N> getGraph(final HierarchyInfo<?, N, ?, ?> info, final long tick, @Nullable final AITaskExecutionContext executionContext) {
			return getGraph(info, tick, executionContext, true);
		}

		<N> @Nullable ChunkSectionRegionConnectivityGraph<N> getGraph(HierarchyInfo<?, N, ?, ?> info, long tick, @Nullable AITaskExecutionContext executionContext, boolean enqueue);
	}
}
