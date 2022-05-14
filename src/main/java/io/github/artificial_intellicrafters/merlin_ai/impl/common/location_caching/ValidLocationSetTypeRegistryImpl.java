package io.github.artificial_intellicrafters.merlin_ai.impl.common.location_caching;

import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.UniverseInfo;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationClassifier;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetType;
import io.github.artificial_intellicrafters.merlin_ai.api.location_caching.ValidLocationSetTypeRegistry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ValidLocationSetTypeRegistryImpl implements ValidLocationSetTypeRegistry {
	public static final ValidLocationSetTypeRegistryImpl INSTANCE = new ValidLocationSetTypeRegistryImpl();
	private final Map<Identifier, ValidLocationSetTypeImpl<?>> registry = new HashMap<>();

	private ValidLocationSetTypeRegistryImpl() {
	}

	@Override
	public <T> void register(final UniverseInfo<T> universeInfo, final ValidLocationClassifier<T> classifier, final Class<T> typeClass, final Identifier id) {
		if (registry.put(id, new ValidLocationSetTypeImpl<>(universeInfo, classifier, typeClass)) != null) {
			throw new RuntimeException("Duplicate ValidLocationSetType Identifiers");
		}
	}

	@Override
	public @Nullable <T> ValidLocationSetType<T> get(final Class<T> typeClass, final Identifier id) {
		final ValidLocationSetTypeImpl<?> type = registry.get(id);
		if (type.typeClass() != typeClass) {
			return null;
		}
		return (ValidLocationSetType<T>) type;
	}
}
