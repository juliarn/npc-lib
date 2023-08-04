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

import com.github.juliarn.npclib.api.event.NpcEvent;
import com.github.juliarn.npclib.api.log.PlatformLogger;
import com.seiama.event.EventConfig;
import com.seiama.event.EventSubscription;
import com.seiama.event.bus.EventBus;
import com.seiama.event.bus.SimpleEventBus;
import com.seiama.event.registry.EventRegistry;
import com.seiama.event.registry.SimpleEventRegistry;
import java.util.OptionalInt;
import org.jetbrains.annotations.NotNull;

final class DefaultNpcEventManager implements NpcEventManager {

  private final EventBus<NpcEvent> eventBus;
  private final EventRegistry<NpcEvent> eventRegistry;

  public DefaultNpcEventManager(boolean debugEnabled, @NotNull PlatformLogger logger) {
    this.eventRegistry = new SimpleEventRegistry<>(NpcEvent.class);
    this.eventBus = new SimpleEventBus<>(this.eventRegistry, new LoggingEventExceptionHandler(debugEnabled, logger));
  }

  @Override
  public <E extends NpcEvent> @NotNull E post(@NotNull E event) {
    this.eventBus.post(event, OptionalInt.empty());
    return event;
  }

  @Override
  public <E extends NpcEvent> @NotNull NpcEventManager registerEventHandler(
    @NotNull Class<E> eventType,
    @NotNull NpcEventConsumer<E> consumer
  ) {
    return this.registerEventHandler(eventType, consumer, EventConfig.DEFAULT_ORDER);
  }

  @Override
  public <E extends NpcEvent> @NotNull NpcEventManager registerEventHandler(
    @NotNull Class<E> eventType,
    @NotNull NpcEventConsumer<E> consumer,
    int eventHandlerPriority
  ) {
    EventConfig eventConfig = EventConfig.defaults().acceptsCancelled(false).order(eventHandlerPriority);
    this.eventRegistry.subscribe(eventType, eventConfig, consumer::accept);
    return this;
  }

  private static final class LoggingEventExceptionHandler implements EventBus.EventExceptionHandler {

    private final boolean debugEnabled;
    private final PlatformLogger logger;

    public LoggingEventExceptionHandler(boolean debugEnabled, @NotNull PlatformLogger logger) {
      this.debugEnabled = debugEnabled;
      this.logger = logger;
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

    @Override
    public <E> void eventExceptionCaught(
      @NotNull EventSubscription<? super E> subscription,
      @NotNull E event,
      @NotNull Throwable throwable
    ) {
      if (isFatal(throwable)) {
        // rethrow fatal exceptions instantly
        throwUnchecked(throwable);
      } else if (this.debugEnabled) {
        // just log that we received an exception from the event handler
        this.logger.error(
          String.format(
            "Subscriber %s was unable to handle %s:",
            subscription.subscriber().getClass().getName(),
            event.getClass().getSimpleName()),
          throwable);
      }
    }
  }
}
