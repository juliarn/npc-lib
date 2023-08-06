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

import com.github.juliarn.npclib.api.event.CancellableNpcEvent;
import com.github.juliarn.npclib.api.event.NpcEvent;
import com.github.juliarn.npclib.api.log.PlatformLogger;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;

final class DefaultNpcEventManager implements NpcEventManager {

  private static final Comparator<NpcEventSubscription<? super NpcEvent>> SUBSCRIPTION_COMPARABLE =
    Comparator.comparingInt(NpcEventSubscription::order);

  private final boolean debugEnabled;
  private final PlatformLogger platformLogger;

  private final Map<Class<?>, List<NpcEventSubscription<? super NpcEvent>>> registeredSubscribers =
    new ConcurrentHashMap<>(16, 0.9f, 1);

  public DefaultNpcEventManager(boolean debugEnabled, @NotNull PlatformLogger logger) {
    this.debugEnabled = debugEnabled;
    this.platformLogger = logger;
  }

  private static boolean isEventCancelled(@NotNull NpcEvent event) {
    return event instanceof CancellableNpcEvent && ((CancellableNpcEvent) event).cancelled();
  }

  @Override
  public <E extends NpcEvent> @NotNull E post(@NotNull E event) {
    Objects.requireNonNull(event, "event");

    for (Map.Entry<Class<?>, List<NpcEventSubscription<? super NpcEvent>>> entry : this.registeredSubscribers.entrySet()) {
      Class<?> subscribedEventType = entry.getKey();
      List<NpcEventSubscription<? super NpcEvent>> subscriptions = entry.getValue();

      if (subscribedEventType.isInstance(event) && !subscriptions.isEmpty()) {
        for (NpcEventSubscription<? super E> subscription : subscriptions) {
          // once the event was cancelled we don't want to post it to any further subscribers
          boolean eventWasCancelled = isEventCancelled(event);
          if (eventWasCancelled) {
            break;
          }

          try {
            subscription.eventConsumer().handle(event);
          } catch (Throwable throwable) {
            EventExceptionHandler.rethrowFatalException(throwable);
            if (this.debugEnabled) {
              // not a fatal exception but debug is enabled to we log it anyway
              this.platformLogger.error(
                String.format(
                  "Subscriber %s was unable to handle %s",
                  subscription.eventConsumer().getClass().getName(),
                  event.getClass().getSimpleName()),
                throwable);
            }
          }
        }
      }
    }

    return event;
  }

  @Override
  public <E extends NpcEvent> @NotNull NpcEventSubscription<? super E> registerEventHandler(
    @NotNull Class<E> eventType,
    @NotNull NpcEventConsumer<E> consumer
  ) {
    return this.registerEventHandler(eventType, consumer, 0);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E extends NpcEvent> @NotNull NpcEventSubscription<? super E> registerEventHandler(
    @NotNull Class<E> eventType,
    @NotNull NpcEventConsumer<E> consumer,
    int eventHandlerPriority
  ) {
    NpcEventSubscription<? super E> subscription = new DefaultNpcEventSubscription<>(
      eventHandlerPriority,
      eventType,
      consumer,
      this);

    List<NpcEventSubscription<? super NpcEvent>> eventSubscriptions = this.registeredSubscribers.computeIfAbsent(
      eventType,
      __ -> new CopyOnWriteArrayList<>());
    eventSubscriptions.add((NpcEventSubscription<? super NpcEvent>) subscription);

    eventSubscriptions.sort(SUBSCRIPTION_COMPARABLE);
    return subscription;
  }

  @Override
  public void unregisterEventHandlerIf(@NotNull Predicate<NpcEventSubscription<? super NpcEvent>> subscriptionFilter) {
    for (List<NpcEventSubscription<? super NpcEvent>> subscriptions : this.registeredSubscribers.values()) {
      subscriptions.removeIf(subscriptionFilter);
    }
  }

  void removeSubscription(@NotNull NpcEventSubscription<?> subscription) {
    List<NpcEventSubscription<? super NpcEvent>> subscriptions = this.registeredSubscribers.get(
      subscription.eventType());
    if (subscriptions != null) {
      subscriptions.remove(subscription);
    }
  }
}
