/*
 * This file is part of npc-lib, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022 Julian M., Pasqual K. and contributors
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

import com.github.juliarn.npclib.api.PlatformWorldAccessor;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BukkitWorldAccessor {

  private BukkitWorldAccessor() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull PlatformWorldAccessor<World> worldAccessor() {
    // check if we are on paper and newer (or equal) to 1.16.5
    if (PaperLib.isPaper() && PaperLib.isVersion(16, 5)) {
      return ModernAccessor.INSTANCE;
    } else {
      return LegacyAccessor.INSTANCE;
    }
  }

  public static @NotNull PlatformWorldAccessor<World> nameBasedAccessor() {
    return LegacyAccessor.INSTANCE;
  }

  public static @NotNull PlatformWorldAccessor<World> keyBasedAccessor() {
    return ModernAccessor.INSTANCE;
  }

  private static final class LegacyAccessor implements PlatformWorldAccessor<World> {

    private static final PlatformWorldAccessor<World> INSTANCE = new LegacyAccessor();

    @Override
    public @NotNull String extractWorldIdentifier(@NotNull World world) {
      return world.getName();
    }

    @Override
    public @Nullable World resolveWorldFromIdentifier(@NotNull String identifier) {
      return Bukkit.getWorld(identifier);
    }
  }

  private static final class ModernAccessor implements PlatformWorldAccessor<World> {

    private static final PlatformWorldAccessor<World> INSTANCE = new ModernAccessor();

    @Override
    public @NotNull String extractWorldIdentifier(@NotNull World world) {
      return world.getKey().toString();
    }

    @Override
    public @Nullable World resolveWorldFromIdentifier(@NotNull String identifier) {
      NamespacedKey key = NamespacedKey.fromString(identifier);
      return key == null ? null : Bukkit.getWorld(key);
    }
  }
}
