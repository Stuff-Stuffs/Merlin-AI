package io.github.artificial_intellicrafters.merlin_ai.api.location_caching;

import io.github.artificial_intellicrafters.merlin_ai.impl.common.location_caching.ValidLocationSetTypeRegistryImpl;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.NonExtendable
public interface ValidLocationSetTypeRegistry {
	ValidLocationSetTypeRegistry INSTANCE = ValidLocationSetTypeRegistryImpl.INSTANCE;

	<T> void register(UniverseInfo<T> universeInfo, ValidLocationClassifier<T> classifier, Class<T> typeClass, boolean columnar, Identifier id);

	<T> @Nullable ValidLocationSetType<T> get(Class<T> typeClass, Identifier id);
}
