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
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

final class DefaultEntityMetadataFactory<I, O> implements EntityMetadataFactory<I, O> {

  private final int baseIndex;
  private final int[] indexShitVersions;

  private final Type type;
  private final Function<I, O> inputConverter;

  private final Collection<EntityMetadataFactory<I, Object>> relatedMetadata;
  private final Function<PlatformVersionAccessor, Boolean> availabilityChecker;

  public DefaultEntityMetadataFactory(
    int baseIndex,
    int[] indexShitVersions,
    @NotNull Type type,
    @NotNull Function<I, O> inputConverter,
    @NotNull Collection<EntityMetadataFactory<I, Object>> relatedMetadata,
    @NotNull Function<PlatformVersionAccessor, Boolean> availabilityChecker
  ) {
    this.baseIndex = baseIndex;
    this.indexShitVersions = indexShitVersions;
    this.type = type;
    this.inputConverter = inputConverter;
    this.relatedMetadata = Collections.unmodifiableCollection(relatedMetadata);
    this.availabilityChecker = availabilityChecker;
  }

  @Override
  @Unmodifiable
  public @NotNull Collection<EntityMetadataFactory<I, Object>> relatedMetadata() {
    return this.relatedMetadata;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @NotNull EntityMetadata<O> create(@NotNull I input, @NotNull PlatformVersionAccessor versionAccessor) {
    // check if the meta is available
    if (this.availabilityChecker.apply(versionAccessor)) {
      // try to convert the given input value
      O value = this.inputConverter.apply(input);
      if (value != null) {
        // calculate the index & create the meta
        int index = this.baseIndex + this.calcIndexShift(versionAccessor);
        return new AvailableEntityMetadata<>(index, value, this.type);
      }
    }

    // not available
    return (EntityMetadata<O>) UnavailableEntityMetadata.INSTANCE;
  }

  private int calcIndexShift(@NotNull PlatformVersionAccessor versionAccessor) {
    int shift = 0;
    for (int version : this.indexShitVersions) {
      if (versionAccessor.minor() >= version) {
        shift++;
      }
    }
    return shift;
  }

  private static final class AvailableEntityMetadata<O> implements EntityMetadata<O> {

    private final int index;

    private final O value;
    private final Type type;

    private AvailableEntityMetadata(int index, @NotNull O value, @NotNull Type type) {
      this.index = index;
      this.value = value;
      this.type = type;
    }

    @Override
    public int index() {
      return this.index;
    }

    @Override
    public boolean available() {
      return true;
    }

    @Override
    public @NotNull O value() {
      return this.value;
    }

    @Override
    public @NotNull Type type() {
      return this.type;
    }
  }

  private static final class UnavailableEntityMetadata implements EntityMetadata<Object> {

    private static final UnavailableEntityMetadata INSTANCE = new UnavailableEntityMetadata();

    @Override
    public int index() {
      throw new UnsupportedOperationException("Unavailable entity metadata cannot be accessed");
    }

    @Override
    public boolean available() {
      return false;
    }

    @Override
    public @NotNull Object value() {
      throw new UnsupportedOperationException("Unavailable entity metadata cannot be accessed");
    }

    @Override
    public @NotNull Class<Object> type() {
      throw new UnsupportedOperationException("Unavailable entity metadata cannot be accessed");
    }
  }
}
