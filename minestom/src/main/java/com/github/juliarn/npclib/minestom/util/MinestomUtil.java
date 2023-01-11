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

package com.github.juliarn.npclib.minestom.util;

import static net.minestom.server.utils.MathUtils.square;

import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.Position;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public final class MinestomUtil {

  private MinestomUtil() {
    throw new UnsupportedOperationException();
  }

  public static double distance(@NotNull Npc<?, ?, ?, ?> npc, @NotNull Pos pos) {
    Position position = npc.position();
    return square(pos.x() - position.x()) + square(pos.y() - position.y()) + square(pos.z() - position.z());
  }

  public static @NotNull Pos minestomFromPosition(@NotNull Position position) {
    return new Pos(position.x(), position.y(), position.z(), position.yaw(), position.pitch());
  }

  public static @NotNull Position positionFromMinestom(@NotNull Pos pos, @NotNull Instance world) {
    return Position.position(pos.x(), pos.y(), pos.z(), pos.yaw(), pos.pitch(), world.getUniqueId().toString());
  }
}
