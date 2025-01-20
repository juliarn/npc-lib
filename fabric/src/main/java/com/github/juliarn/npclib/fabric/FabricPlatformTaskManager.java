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

package com.github.juliarn.npclib.fabric;

import com.github.juliarn.npclib.api.PlatformTaskManager;
import com.github.juliarn.npclib.common.task.AsyncPlatformTaskManager;
import com.github.juliarn.npclib.fabric.mixin.accessor.MinecraftServerAccessor;
import net.minecraft.server.ServerTask;
import org.jetbrains.annotations.NotNull;

public final class FabricPlatformTaskManager extends AsyncPlatformTaskManager {

  private static final FabricPlatformTaskManager INSTANCE = new FabricPlatformTaskManager();

  private FabricPlatformTaskManager() {
    super("Fabric");
  }

  public static @NotNull PlatformTaskManager taskManager() {
    return INSTANCE;
  }

  @Override
  public void scheduleSync(@NotNull Runnable task) {
    NpcLibServer.getServer().execute(task);
  }

  @Override
  public void scheduleDelayedSync(@NotNull Runnable task, int delayTicks) {
    ((MinecraftServerAccessor) NpcLibServer.getServer()).invokeExecuteTask(new ServerTask(delayTicks, task));
  }
}
