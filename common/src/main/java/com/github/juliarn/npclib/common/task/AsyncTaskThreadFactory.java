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

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;

final class AsyncTaskThreadFactory implements ThreadFactory {

  private final String threadNameFormat;
  private final ThreadGroup parentThreadGroup;

  private final AtomicLong createdThreadCount = new AtomicLong(0);

  private AsyncTaskThreadFactory(@NotNull String threadNameFormat) {
    this.threadNameFormat = threadNameFormat;
    this.parentThreadGroup = Thread.currentThread().getThreadGroup();
  }

  public static @NotNull ThreadFactory create(@NotNull String threadNameFormat) {
    Objects.requireNonNull(threadNameFormat, "threadNameFormat");
    return new AsyncTaskThreadFactory(threadNameFormat);
  }

  @Override
  public @NotNull Thread newThread(@NotNull Runnable runnable) {
    // construct the name thread
    String threadName = String.format(this.threadNameFormat, this.createdThreadCount.incrementAndGet());
    Thread thread = new Thread(this.parentThreadGroup, runnable, threadName, 0);

    // set up the thread configuration
    thread.setDaemon(true);
    thread.setPriority(Thread.NORM_PRIORITY);
    return thread;
  }
}
