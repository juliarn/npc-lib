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

package com.github.juliarn.npclib.api.event.manager;

import org.jetbrains.annotations.NotNull;

final class EventExceptionHandler {

  private EventExceptionHandler() {
    throw new UnsupportedOperationException();
  }

  public static void rethrowFatalException(@NotNull Throwable throwable) {
    if (isFatal(throwable)) {
      throwUnchecked(throwable);
    }
  }

  private static boolean isFatal(@NotNull Throwable throwable) {
    // this includes the most fatal errors that can occur on a thread which we should not silently ignore and rethrow
    return throwable instanceof InterruptedException
      || throwable instanceof LinkageError
      || throwable instanceof ThreadDeath
      || throwable instanceof VirtualMachineError;
  }

  @SuppressWarnings("unchecked")
  private static <T extends Throwable> void throwUnchecked(@NotNull Throwable throwable) throws T {
    throw (T) throwable;
  }
}
