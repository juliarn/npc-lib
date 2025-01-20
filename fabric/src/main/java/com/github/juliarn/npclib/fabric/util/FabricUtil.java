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

import static net.minecraft.util.math.MathHelper.square;

import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public final class FabricUtil {

  private FabricUtil() {
    throw new UnsupportedOperationException();
  }

  public static double distance(@NotNull Npc<?, ?, ?, ?> npc, @NotNull Vec3d pos) {
    Position position = npc.position();
    return square(pos.getX() - position.x()) + square(pos.getY() - position.y()) + square(pos.getZ() - position.z());
  }

  /*public static @NotNull Pos minestomFromPosition(@NotNull Position position) {
    return new Pos(position.x(), position.y(), position.z(), position.yaw(), position.pitch());
  }*/

  //TODO yaw, pitch, key
  public static @NotNull Position positionFromMinestom(@NotNull Vec3d pos, @NotNull World world) {
    return Position.position(pos.getX(), pos.getY(), pos.getZ(), 0f, 0f,
      world.getRegistryKey().getRegistry().toString());
  }
}
