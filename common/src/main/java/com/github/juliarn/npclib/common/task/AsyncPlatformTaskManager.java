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

package com.github.juliarn.npclib.common.task;

import com.github.juliarn.npclib.api.PlatformTaskManager;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public final class AsyncPlatformTaskManager implements PlatformTaskManager {

  private static final long ONE_TICK_MS = 1000 / 20;

  private final ExecutorService runOnceExecutorService;
  private final ScheduledExecutorService scheduledExecutorService;

  private AsyncPlatformTaskManager(@NotNull String extensionId) {
    ThreadFactory runOnceThreadFactory = AsyncTaskThreadFactory.create(extensionId + " NPC-Lib Task #%d");
    this.runOnceExecutorService = Executors.newCachedThreadPool(runOnceThreadFactory);

    ThreadFactory scheduledThreadFactory = AsyncTaskThreadFactory.create(extensionId + " NPC-Lib Scheduled Task #%d");
    this.scheduledExecutorService = Executors.newScheduledThreadPool(0, scheduledThreadFactory);
  }

  public static @NotNull PlatformTaskManager taskManager(@NotNull String extensionIdentifier) {
    Objects.requireNonNull(extensionIdentifier, "extensionIdentifier");
    return new AsyncPlatformTaskManager(extensionIdentifier);
  }

  @Override
  public void scheduleSync(@NotNull Runnable task) {
    this.runOnceExecutorService.execute(task);
  }

  @Override
  public void scheduleDelayedSync(@NotNull Runnable task, int delayTicks) {
    this.scheduledExecutorService.schedule(task, delayTicks * ONE_TICK_MS, TimeUnit.MILLISECONDS);
  }

  @Override
  public void scheduleAsync(@NotNull Runnable task) {
    this.runOnceExecutorService.execute(task);
  }

  @Override
  public void scheduleDelayedAsync(@NotNull Runnable task, int delayTicks) {
    this.scheduledExecutorService.schedule(task, delayTicks * ONE_TICK_MS, TimeUnit.MILLISECONDS);
  }
}
