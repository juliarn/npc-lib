package com.github.juliarn.npc.properties;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A property which cannot be modified.
 *
 * @param <T> The type of the value the property is holding.
 * @since 2.5-SNAPSHOT
 */
public class UnmodifiableProperty<T> extends Property<T> {

  /**
   * {@inheritDoc}
   */
  protected UnmodifiableProperty(@NotNull String name, @Nullable T defaultValue) {
    super(name, defaultValue);
  }

  /**
   * Always throws an {@link UnsupportedOperationException} because it is not allowed
   * to modify an unmodifiable property.
   *
   * @param newValue The new value of this property.
   * @return Not reachable.
   * @throws UnsupportedOperationException as this property is unmodifiable.
   */
  @Override
  public UnmodifiableProperty<T> setCurrentValue(@Nullable T newValue) {
    throw new UnsupportedOperationException("Property is unmodifiable");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull UnmodifiableProperty<T> unmodifiable() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull UnmodifiableProperty<T> shallowClone() {
    return new UnmodifiableProperty<>(this.getName(), this.getValue().orElse(null));
  }
}
