package com.github.juliarn.npc.properties;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A property which can be set into a {@link PropertyMap}.
 *
 * @param <T> The type of the value the property is holding.
 * @since 2.5-SNAPSHOT
 */
public class Property<T> implements Cloneable {

  private final String name;
  private final AtomicReference<T> currentValue = new AtomicReference<>();

  /**
   * Creates a new property instance.
   *
   * @param name the name of the property to identify it.
   */
  protected Property(@NotNull String name) {
    this.name = Preconditions.checkNotNull(name);
  }

  /**
   * Creates a new property instance.
   *
   * @param name         the name of the property to identify it.
   * @param defaultValue the default value of the property after creation.
   */
  protected Property(@NotNull String name, @Nullable T defaultValue) {
    this.name = Preconditions.checkNotNull(name);
    this.currentValue.set(defaultValue);
  }

  /**
   * Creates a new property with the given name. The initial value is {@code null}.
   *
   * @param name The name of the property.
   * @param <V>  The type of the value the property is holding.
   * @return The created property.
   */
  @NotNull
  public static <V> Property<V> named(@NotNull String name) {
    return new Property<>(name);
  }

  /**
   * Creates a new property with the given name and initial value.
   *
   * @param name         The name of the property.
   * @param defaultValue The default value of the property.
   * @param <V>          The type of the value the property is holding.
   * @return The created property.
   */
  @NotNull
  public static <V> Property<V> named(@NotNull String name, @NotNull V defaultValue) {
    return new Property<>(name, defaultValue);
  }

  /**
   * Get if this property has a value set.
   *
   * @return If this property has a value set.
   */
  public boolean hasValue() {
    return this.currentValue.get() != null;
  }

  /**
   * Get the current value of this property.
   *
   * @return the current value of this property.
   */
  @NotNull
  public Optional<T> getValue() {
    return Optional.ofNullable(this.currentValue.get());
  }

  /**
   * Sets the current value of this property.
   *
   * @param newValue The new value of this property.
   * @return The same instance of this class.
   */
  public Property<T> setCurrentValue(@Nullable T newValue) {
    this.currentValue.set(newValue);
    return this;
  }

  /**
   * Gets the value without checking if the value is present.
   *
   * @return the value without checking if the value is present.
   * @throws NullPointerException if the value is not present.
   */
  @NotNull
  public T getValueUnchecked() {
    return Preconditions.checkNotNull(this.currentValue.get(), "value");
  }

  /**
   * Gets the current value of this property.
   *
   * @param def The value to get when this property has no value set.
   * @return The value of this property or {@code def} if this property has no value.
   */
  @NotNull
  public T getValue(@NotNull T def) {
    T current = this.currentValue.get();
    return current == null ? def : current;
  }

  /**
   * Get the name of this property. A name is unique in a {@link PropertyMap}.
   *
   * @return the name of this property.
   */
  @NotNull
  public String getName() {
    return this.name;
  }

  /**
   * Converts this property to an unmodifiable property, returns the same instance if the
   * property is already unmodifiable.
   *
   * @return this property as an unmodifiable property.
   */
  @NotNull
  public UnmodifiableProperty<T> unmodifiable() {
    return new UnmodifiableProperty<>(this.name, this.currentValue.get());
  }

  /**
   * Creates a clone of this property without cloning the current value of it.
   *
   * @return a clone of this property without cloning the current value of it.
   */
  @NotNull
  public Property<T> shallowClone() {
    return new Property<>(this.name, this.currentValue.get());
  }
}
