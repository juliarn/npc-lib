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

package com.github.juliarn.npclib.api.protocol.chat;

import java.util.Objects;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Component {

  static @NotNull Component ofRawMessage(@NotNull String rawMessage) {
    Objects.requireNonNull(rawMessage, "rawMessage");
    return new DefaultComponent(rawMessage, null);
  }

  static @NotNull Component ofJsonEncodedMessage(@NotNull String jsonEncodedMessage) {
    Objects.requireNonNull(jsonEncodedMessage, "jsonEncodedMessage");
    return new DefaultComponent(null, jsonEncodedMessage);
  }

  @Contract("null, null -> fail")
  static @NotNull Component ofJsonEncodedOrRaw(@Nullable String rawMessage, @Nullable String jsonEncodedMessage) {
    if (rawMessage == null && jsonEncodedMessage == null) {
      throw new IllegalArgumentException("Either rawMessage or jsonEncodedMessage must be given");
    }

    return new DefaultComponent(rawMessage, jsonEncodedMessage);
  }

  @Nullable String rawMessage();

  @Nullable String encodedJsonMessage();
}
