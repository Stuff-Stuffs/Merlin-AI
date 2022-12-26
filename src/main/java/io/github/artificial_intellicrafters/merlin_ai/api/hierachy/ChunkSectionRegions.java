package io.github.artificial_intellicrafters.merlin_ai.api.hierachy;

import io.github.artificial_intellicrafters.merlin_ai.impl.common.hierarchy.ChunkSectionRegionsImpl;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;
import org.jetbrains.annotations.Nullable;

public interface ChunkSectionRegions {
	@Nullable ChunkSectionRegion query(short pos);

	@Nullable ChunkSectionRegion byId(long id);

	static Builder builder(final ChunkSectionPos pos, final HeightLimitView view) {
		return new ChunkSectionRegionsImpl.BuilderImpl(pos, view);
	}

	interface Builder {
		RegionKey newRegion();

		boolean contains(short pos);

		void expand(RegionKey key, short pos);

		ChunkSectionRegions build();
	}

	interface RegionKey {
	}
}
