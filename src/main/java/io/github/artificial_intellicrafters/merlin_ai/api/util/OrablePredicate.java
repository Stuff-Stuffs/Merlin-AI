package io.github.artificial_intellicrafters.merlin_ai.api.util;

public interface OrablePredicate<T, Self extends OrablePredicate<T, Self>> {
	boolean test(T value);

	Self or(Self other);
}
