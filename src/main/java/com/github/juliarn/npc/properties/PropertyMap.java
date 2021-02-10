package com.github.juliarn.npc.properties;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A map which associates a property name with the property instance.
 *
 * @since 2.5-SNAPSHOT
 */
public class PropertyMap {

  private final Map<String, Property<?>> properties = new ConcurrentHashMap<>();

  /**
   * Checks if this property map holds an associated property for the given {@code name}.
   *
   * @param name The name of the property to check.
   * @return If this map has an associated property with the given {@code name}.
   */
  public boolean hasProperty(@NotNull String name) {
    return this.properties.containsKey(name);
  }

  /**
   * Checks if this property map holds an associated property for the given {@code property}.
   *
   * @param property The property to check.
   * @return If this map has an associated property with the given {@code property}.
   */
  public boolean hasProperty(@NotNull Property<?> property) {
    return this.properties.containsKey(property.getName());
  }

  /**
   * Gets a specific property with the given {@code name}.
   *
   * @param name The name of the property to get.
   * @param <T>  The type of the property value to get.
   * @return The property associated with the given {@code name}.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public <T> Optional<Property<T>> getProperty(@NotNull String name) {
    Property<?> property = this.properties.get(name);
    return Optional.ofNullable(property == null ? null : (Property<T>) property);
  }

  /**
   * Gets a specific property with the given {@code property} name.
   *
   * @param property The property to get.
   * @param <T>      The type of the property value to get.
   * @return The property associated with the given {@code name}.
   * @see #getProperty(String)
   */
  @NotNull
  public <T> Optional<Property<T>> getProperty(@NotNull Property<?> property) {
    return this.getProperty(property.getName());
  }

  /**
   * Gets a property from this map without checking if the property is actually present.
   *
   * @param name The name of the property to get.
   * @param <T>  The type of the property value to get.
   * @return The property associated with the given {@code name}.
   * @throws NullPointerException If there is no property associated with the given {@code name}.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public <T> Property<T> getPropertyUnchecked(@NotNull String name) {
    Property<?> property = this.properties.get(name);
    Preconditions.checkNotNull(property, "property");
    return (Property<T>) property;
  }

  /**
   * Gets a property from this map without checking if the property is actually present.
   *
   * @param property The property to get.
   * @param <T>      The type of the property value to get.
   * @return The property associated with the given {@code name}.
   * @throws NullPointerException If there is no property associated with the given {@code name}.
   * @see #getProperty(String)
   */
  @NotNull
  public <T> Property<T> getPropertyUnchecked(@NotNull Property<T> property) {
    return this.getPropertyUnchecked(property.getName());
  }

  /**
   * Sets a property into this map if there is no previous association with the given {@code property} name.
   *
   * @param property The property to set into this map.
   */
  public void setProperty(@NotNull Property<?> property) {
    this.properties.putIfAbsent(property.getName(), property);
  }

  /**
   * Removes a specific property from this map.
   *
   * @param property The property to remove.
   */
  public void removeProperty(@NotNull Property<?> property) {
    this.properties.remove(property.getName());
  }

  /**
   * Removes a specific property from this map.
   *
   * @param name The name of the property to remove.
   */
  public void removeProperty(@NotNull String name) {
    this.properties.remove(name);
  }

  /**
   * Get an unmodifiable copy of all properties added to this map.
   *
   * @return an unmodifiable copy of all properties added to this map.
   */
  @NotNull
  @Unmodifiable
  public Collection<Property<?>> getProperties() {
    return Collections.unmodifiableCollection(this.properties.values());
  }
}
