/*
 * This file is part of npc-lib, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022-2023 Julian M., Pasqual K. and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.juliarn.npclib.api.profile;

import com.github.juliarn.npclib.api.util.Util;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DefaultResolvedProfile implements Profile.Resolved {

  private final String name;
  private final UUID uniqueId;
  private final Set<ProfileProperty> properties;

  DefaultResolvedProfile(
    @NotNull String name,
    @NotNull UUID uniqueId,
    @NotNull Set<ProfileProperty> properties
  ) {
    this.name = name;
    this.uniqueId = uniqueId;
    this.properties = properties.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(properties);
  }

  private DefaultResolvedProfile(
    @NotNull String name,
    @NotNull UUID uniqueId,
    @NotNull Set<ProfileProperty> properties,
    @Nullable Void ignored
  ) {
    this.name = name;
    this.uniqueId = uniqueId;
    this.properties = properties;
  }

  @Override
  public @NotNull Set<ProfileProperty> properties() {
    return this.properties;
  }

  @Override
  public @NotNull UUID uniqueId() {
    return this.uniqueId;
  }

  @Override
  public @NotNull String name() {
    return this.name;
  }

  @Override
  public @NotNull Resolved withName(@NotNull String name) {
    return new DefaultResolvedProfile(name, this.uniqueId, this.properties, null);
  }

  @Override
  public @NotNull Resolved withUniqueId(@NotNull UUID uniqueId) {
    return new DefaultResolvedProfile(this.name, uniqueId, this.properties, null);
  }

  @Override
  public @NotNull Resolved withoutProperties() {
    return new DefaultResolvedProfile(this.name, this.uniqueId, Collections.emptySet());
  }

  @Override
  public @NotNull Resolved withProperty(@NotNull ProfileProperty property) {
    Objects.requireNonNull(property, "property");

    Set<ProfileProperty> propertySet = new HashSet<>(this.properties);
    propertySet.add(property);
    return new DefaultResolvedProfile(this.name, this.uniqueId, propertySet);
  }

  @Override
  public @NotNull Resolved withProperties(@NotNull Set<ProfileProperty> properties) {
    return new DefaultResolvedProfile(this.name, this.uniqueId, new HashSet<>(properties));
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.name, this.uniqueId, this.properties);
  }

  @Override
  public boolean equals(Object obj) {
    return Util.equals(Profile.Resolved.class, this, obj, (orig, comp) -> orig.name().equals(comp.name())
      && orig.uniqueId().equals(comp.uniqueId())
      && orig.properties().equals(comp.properties()));
  }
}
