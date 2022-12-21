package io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.UniverseInfo;

public enum BasicLocationType {
	CLOSED(0),
	OPEN(1),
	GROUND(2);
	public static final UniverseInfo<BasicLocationType> UNIVERSE_INFO = UniverseInfo.ofEnum(CLOSED, val -> val.tag);
	public final int tag;

	BasicLocationType(final int tag) {
		this.tag = tag;
	}
}
