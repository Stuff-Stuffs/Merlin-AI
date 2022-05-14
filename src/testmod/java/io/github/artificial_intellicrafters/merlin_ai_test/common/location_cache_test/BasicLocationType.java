package io.github.artificial_intellicrafters.merlin_ai_test.common.location_cache_test;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.UniverseInfo;

public enum BasicLocationType {
	CLOSED(0),
	OPEN(1),
	GROUND(2);
	public static final UniverseInfo<BasicLocationType> UNIVERSE_INFO = new UniverseInfo<>() {
		private final BasicLocationType[] byTag;

		{
			final BasicLocationType[] values = BasicLocationType.values();
			byTag = new BasicLocationType[values.length];
			for (final BasicLocationType value : values) {
				byTag[value.tag] = value;
			}
		}

		@Override
		public BasicLocationType getDefaultValue() {
			return CLOSED;
		}

		@Override
		public int toInt(final BasicLocationType value) {
			return value.tag;
		}

		@Override
		public BasicLocationType fromInt(final int value) {
			return byTag[value];
		}

		@Override
		public int getUniverseSize() {
			return byTag.length;
		}
	};
	public final int tag;

	BasicLocationType(final int tag) {
		this.tag = tag;
	}
}
