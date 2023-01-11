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

package com.github.juliarn.npclib.common.flag;

import com.github.juliarn.npclib.api.flag.NpcFlag;
import com.github.juliarn.npclib.api.flag.NpcFlaggedBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CommonNpcFlaggedBuilder<B> implements NpcFlaggedBuilder<B> {

  protected final Map<NpcFlag<?>, Optional<?>> flags = new HashMap<>();

  @Override
  @SuppressWarnings("unchecked")
  public <T> @NotNull B flag(@NotNull NpcFlag<T> flag, @Nullable T value) {
    // check if the flag value is acceptable
    if (flag.accepts(value)) {
      this.flags.put(flag, Optional.ofNullable(value));
      return (B) this;
    }

    throw new IllegalArgumentException("Flag " + flag.key() + " does not accept " + value + " as it's value!");
  }
}
