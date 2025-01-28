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

package com.github.juliarn.npclib.fabric.util;

import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.Position;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
public final class FabricUtil {

  // @MonotonicNonNull
  private static MinecraftServer theServer;

  private FabricUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull MinecraftServer getServer() {
    Objects.requireNonNull(FabricUtil.theServer, "server not yet set");
    return FabricUtil.theServer;
  }

  public static void setServer(@NotNull MinecraftServer theServer) {
    if (FabricUtil.theServer != null) {
      throw new IllegalStateException("server already set");
    }

    FabricUtil.theServer = theServer;
  }

  public static double distance(@NotNull Npc<?, ?, ?, ?> npc, @NotNull Vec3 other) {
    Position pos = npc.position();
    return Mth.square(other.x() - pos.x()) + Mth.square(other.y() - pos.y()) + Mth.square(other.z() - pos.z());
  }

  public static @NotNull Position positionFromPosAndRot(
    @NotNull ServerLevel level,
    @NotNull Vec3 pos,
    @NotNull Vec2 rot
  ) {
    var worldId = level.dimension().location().toString();
    return Position.position(pos.x(), pos.y(), pos.z(), rot.x, rot.y, worldId);
  }
}
