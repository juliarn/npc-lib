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

package com.github.juliarn.npclib.api.protocol.meta;

import com.github.juliarn.npclib.api.PlatformVersionAccessor;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

final class DefaultEntityMetadataFactoryBuilder<I, O> implements EntityMetadataFactory.Builder<I, O> {

  private int baseIndex = 0;
  private int[] indexShitVersions = new int[0];

  private Type type;
  private Function<I, O> inputConverter;

  private Collection<EntityMetadataFactory<I, Object>> relatedMetadata;
  private Function<PlatformVersionAccessor, Boolean> availabilityChecker;

  @Override
  public @NotNull EntityMetadataFactory.Builder<I, O> baseIndex(int index) {
    this.baseIndex = index;
    return this;
  }

  @Override
  public @NotNull EntityMetadataFactory.Builder<I, O> indexShiftVersions(int... versions) {
    this.indexShitVersions = versions;
    return this;
  }

  @Override
  public @NotNull EntityMetadataFactory.Builder<I, O> type(@NotNull Type type) {
    this.type = Objects.requireNonNull(type, "type");
    return this;
  }

  @Override
  public @NotNull EntityMetadataFactory.Builder<I, O> inputConverter(@NotNull Function<I, O> mapper) {
    this.inputConverter = Objects.requireNonNull(mapper, "mapper");
    return this;
  }

  @Override
  public @NotNull EntityMetadataFactory.Builder<I, O> addRelatedMetadata(
    @NotNull EntityMetadataFactory<I, Object> relatedMetadata
  ) {
    if (this.relatedMetadata == null) {
      this.relatedMetadata = new HashSet<>();
    }

    this.relatedMetadata.add(relatedMetadata);
    return this;
  }

  @Override
  public @NotNull EntityMetadataFactory.Builder<I, O> availabilityChecker(
    @NotNull Function<PlatformVersionAccessor, Boolean> checker
  ) {
    this.availabilityChecker = Objects.requireNonNull(checker, "checker");
    return this;
  }

  @Override
  public @NotNull EntityMetadataFactory<I, O> build() {
    // fill in default empty values
    if (this.relatedMetadata == null) {
      this.relatedMetadata = Collections.emptySet();
    }

    if (this.availabilityChecker == null) {
      this.availabilityChecker = accessor -> true;
    }

    return new DefaultEntityMetadataFactory<>(
      this.baseIndex,
      this.indexShitVersions,
      Objects.requireNonNull(this.type, "type"),
      Objects.requireNonNull(this.inputConverter, "inputConverter"),
      this.relatedMetadata,
      this.availabilityChecker
    );
  }
}
