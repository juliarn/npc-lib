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

package com.github.juliarn.npclib.minestom;

import com.github.juliarn.npclib.api.PlatformVersionAccessor;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

public final class MinestomVersionAccessor {

  private MinestomVersionAccessor() {
    throw new UnsupportedOperationException();
  }

  private static int[] extractServerVersionParts() {
    String[] parts = MinecraftServer.VERSION_NAME.split("\\.");
    if (parts.length == 2 || parts.length == 3) {
      // should be in the correct format, just to make sure
      try {
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = parts.length == 3 ? Integer.parseInt(parts[2]) : 0;

        // return the version array from that
        return new int[]{major, minor, patch};
      } catch (NumberFormatException ignored) {
      }
    }

    // unable to pars
    return new int[0];
  }

  public static @NotNull PlatformVersionAccessor versionNameBased() {
    return MinestomVersionNameAccessor.INSTANCE;
  }

  private static final class MinestomVersionNameAccessor implements PlatformVersionAccessor {

    private static final int[] VERSION_NUMBER_PARTS = extractServerVersionParts();
    private static final MinestomVersionNameAccessor INSTANCE = new MinestomVersionNameAccessor();

    private static int safeGetPart(int index, int def) {
      return VERSION_NUMBER_PARTS.length > index ? VERSION_NUMBER_PARTS[index] : def;
    }

    @Override
    public int major() {
      return safeGetPart(0, 1);
    }

    @Override
    public int minor() {
      return safeGetPart(1, 0);
    }

    @Override
    public int patch() {
      return safeGetPart(2, 0);
    }

    @Override
    public boolean atLeast(int major, int minor, int patch) {
      return this.major() >= major && this.minor() >= major && this.patch() >= patch;
    }
  }
}
