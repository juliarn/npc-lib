/*
 * This file is part of npc-lib, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022-present Julian M., Pasqual K. and contributors
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

package com.github.juliarn.npclib.fabric;

import com.github.juliarn.npclib.api.PlatformWorldAccessor;
import com.github.juliarn.npclib.fabric.util.FabricUtil;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FabricWorldAccessor {

  public static @NotNull PlatformWorldAccessor<ServerLevel> keyBased() {
    return KeyBasedLevelAccessor.INSTANCE;
  }

  private static final class KeyBasedLevelAccessor implements PlatformWorldAccessor<ServerLevel> {

    private static final KeyBasedLevelAccessor INSTANCE = new KeyBasedLevelAccessor();

    @Override
    public @NotNull String extractWorldIdentifier(@NotNull ServerLevel world) {
      return world.dimension().location().toString();
    }

    @Override
    public @Nullable ServerLevel resolveWorldFromIdentifier(@NotNull String identifier) {
      var levels = FabricUtil.getServer().getAllLevels();
      for (var level : levels) {
        var levelIdentifier = this.extractWorldIdentifier(level);
        if (levelIdentifier.equals(identifier)) {
          return level;
        }
      }

      return null;
    }
  }
}
