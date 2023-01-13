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

package com.github.juliarn.npclib.api.util;

import java.util.concurrent.Callable;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public final class Util {

  private Util() {
    throw new UnsupportedOperationException();
  }

  public static int floor(double in) {
    int casted = (int) in;
    return in < casted ? casted - 1 : casted;
  }

  public static @NotNull <T> Supplier<T> callableToSupplier(@NotNull Callable<T> callable) {
    return () -> {
      try {
        return callable.call();
      } catch (Exception exception) {
        // might not be the best exception but does the job
        throw new IllegalStateException(exception);
      }
    };
  }

  @SuppressWarnings("unchecked")
  public static <T> boolean equals(
    @NotNull Class<T> expectedType,
    @Nullable Object original,
    @Nullable Object compare,
    @NotNull BiPredicate<T, T> checker
  ) {
    // fast check for nullability
    if (original == null || compare == null) {
      return original == null && compare == null;
    }

    // fast path for the same object
    if (original == compare) {
      return true;
    }

    // check if both values are of the same type
    if (!expectedType.isAssignableFrom(original.getClass()) || !expectedType.isAssignableFrom(compare.getClass())) {
      return false;
    }

    // cast both and apply to the checker
    return checker.test((T) original, (T) compare);
  }
}
