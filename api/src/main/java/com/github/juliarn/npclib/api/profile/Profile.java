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

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Profile {

  static @NotNull Unresolved unresolved(@NotNull String name) {
    Objects.requireNonNull(name, "name");
    return new DefaultUnresolvedProfile(name, null);
  }

  static @NotNull Unresolved unresolved(@NotNull UUID uniqueId) {
    Objects.requireNonNull(uniqueId, "unique id");
    return new DefaultUnresolvedProfile(null, uniqueId);
  }

  static @NotNull Resolved resolved(@NotNull String name, @NotNull UUID uniqueId) {
    return resolved(name, uniqueId, Collections.emptySet());
  }

  static @NotNull Resolved resolved(
    @NotNull String name,
    @NotNull UUID uniqueId,
    @NotNull Set<ProfileProperty> properties
  ) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(uniqueId, "unique id");
    Objects.requireNonNull(properties, "properties");

    return new DefaultResolvedProfile(name, uniqueId, properties);
  }

  boolean resolved();

  @Nullable UUID uniqueId();

  @Nullable String name();

  @NotNull Set<ProfileProperty> properties();

  interface Unresolved extends Profile {

    @Override
    default boolean resolved() {
      return false;
    }

    @Override
    default @NotNull Set<ProfileProperty> properties() {
      return Collections.emptySet();
    }
  }

  interface Resolved extends Profile {

    @Override
    @NotNull UUID uniqueId();

    @Override
    @NotNull String name();

    @NotNull Resolved withName(@NotNull String name);

    @NotNull Resolved withUniqueId(@NotNull UUID uniqueId);

    @NotNull Resolved withoutProperties();

    @NotNull Resolved withProperty(@NotNull ProfileProperty property);

    @NotNull Resolved withProperties(@NotNull Set<ProfileProperty> properties);

    @Override
    default boolean resolved() {
      return true;
    }
  }
}
