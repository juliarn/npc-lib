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

package com.github.juliarn.npclib.bukkit;

import com.github.juliarn.npclib.api.PlatformVersionAccessor;
import io.papermc.lib.PaperLib;

public final class BukkitVersionAccessor {

  private BukkitVersionAccessor() {
    throw new UnsupportedOperationException();
  }

  public static PlatformVersionAccessor versionAccessor() {
    return PaperLibPlatformVersionAccessor.INSTANCE;
  }

  private static final class PaperLibPlatformVersionAccessor implements PlatformVersionAccessor {

    private static final PaperLibPlatformVersionAccessor INSTANCE = new PaperLibPlatformVersionAccessor();

    @Override
    public int major() {
      return 1;
    }

    @Override
    public int minor() {
      return PaperLib.getMinecraftVersion();
    }

    @Override
    public int patch() {
      return PaperLib.getMinecraftPatchVersion();
    }

    @Override
    public boolean atLeast(int major, int minor, int patch) {
      return PaperLib.isVersion(minor, patch);
    }
  }
}
