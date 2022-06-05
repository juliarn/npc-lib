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

package com.github.juliarn.npclib.common.util;

import com.github.juliarn.npclib.api.Platform;
import com.github.juliarn.npclib.api.event.NpcEvent;
import java.io.PrintStream;
import java.util.Map;
import net.kyori.event.EventSubscriber;
import net.kyori.event.PostResult;
import org.jetbrains.annotations.NotNull;

public final class EventDispatcher {

  private EventDispatcher() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull <W, P, I, E extends NpcEvent<W, P, I>> E dispatch(
    @NotNull Platform<W, P, I> platform,
    @NotNull E event
  ) {
    // post the event
    PostResult result = platform.eventBus().post(event);

    // check if we need to print out if something failed during execution
    Map<EventSubscriber<?>, Throwable> exceptions = result.exceptions();
    if (platform.debug() && !exceptions.isEmpty()) {
      // print all exceptions
      PrintStream err = System.err;
      for (Map.Entry<EventSubscriber<?>, Throwable> entry : exceptions.entrySet()) {
        err.printf(
          "Subscriber %s was unable to handle %s:%n",
          entry.getKey().getClass().getName(),
          event.getClass().getSimpleName());
        entry.getValue().printStackTrace(err);
      }
    }

    // the same event instance, for chaining
    return event;
  }
}
