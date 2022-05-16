package io.github.artificial_intellicrafters.merlin_ai.api.region;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.NonExtendable
public interface ChunkSectionRegions {
	@Nullable ChunkSectionRegion getRegion(int x, int y, int z);
}
