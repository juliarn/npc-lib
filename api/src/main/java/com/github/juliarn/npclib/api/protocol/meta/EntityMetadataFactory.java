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
import com.github.juliarn.npclib.api.protocol.enums.EntityStatus;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public interface EntityMetadataFactory<I, O> {

  static @NotNull <I, O> EntityMetadataFactory.Builder<I, O> metaFactoryBuilder() {
    return new DefaultEntityMetadataFactoryBuilder<>();
  }

  static @NotNull EntityMetadataFactory<Boolean, Byte> sneakingMetaFactory() {
    return DefaultEntityMetadata.SNEAKING;
  }

  static @NotNull EntityMetadataFactory<Boolean, Byte> skinLayerMetaFactory() {
    return DefaultEntityMetadata.SKIN_LAYERS;
  }

  static @NotNull EntityMetadataFactory<Collection<EntityStatus>, Byte> entityStatusMetaFactory() {
    return DefaultEntityMetadata.ENTITY_STATUS;
  }

  @Unmodifiable
  @NotNull Collection<EntityMetadataFactory<I, Object>> relatedMetadata();

  @NotNull EntityMetadata<O> create(@NotNull I input, @NotNull PlatformVersionAccessor versionAccessor);

  interface Builder<I, O> {

    @NotNull Builder<I, O> baseIndex(int index);

    @NotNull Builder<I, O> indexShiftVersions(int... versions);

    @NotNull Builder<I, O> type(@NotNull Type type);

    @NotNull Builder<I, O> inputConverter(@NotNull Function<I, O> mapper);

    @NotNull Builder<I, O> addRelatedMetadata(@NotNull EntityMetadataFactory<I, Object> relatedMetadata);

    @NotNull Builder<I, O> availabilityChecker(@NotNull Function<PlatformVersionAccessor, Boolean> checker);

    @NotNull EntityMetadataFactory<I, O> build();
  }
}
