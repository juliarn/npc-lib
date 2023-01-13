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

package com.github.juliarn.npclib.api;

import com.github.juliarn.npclib.api.util.Util;
import org.jetbrains.annotations.NotNull;

final class DefaultPosition implements Position {

  private final double x;
  private final double y;
  private final double z;

  private final float yaw;
  private final float pitch;

  private final String worldId;

  public DefaultPosition(double x, double y, double z, float yaw, float pitch, @NotNull String worldId) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
    this.worldId = worldId;
  }

  @Override
  public double x() {
    return this.x;
  }

  @Override
  public double y() {
    return this.y;
  }

  @Override
  public double z() {
    return this.z;
  }

  @Override
  public float yaw() {
    return this.yaw;
  }

  @Override
  public float pitch() {
    return this.pitch;
  }

  @Override
  public @NotNull String worldId() {
    return this.worldId;
  }

  @Override
  public int blockX() {
    return Util.floor(this.x);
  }

  @Override
  public int blockY() {
    return Util.floor(this.y);
  }

  @Override
  public int blockZ() {
    return Util.floor(this.z);
  }

  @Override
  public int chunkX() {
    return this.blockX() >> 4;
  }

  @Override
  public int chunkY() {
    return this.blockY() >> 8;
  }

  @Override
  public int chunkZ() {
    return this.blockZ() >> 4;
  }
}
