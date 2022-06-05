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

package com.github.juliarn.npclib.common.platform;

import com.github.juliarn.npclib.api.NpcActionController;
import com.github.juliarn.npclib.api.NpcTracker;
import com.github.juliarn.npclib.api.Platform;
import com.github.juliarn.npclib.api.PlatformTaskManager;
import com.github.juliarn.npclib.api.PlatformWorldAccessor;
import com.github.juliarn.npclib.api.event.NpcEvent;
import com.github.juliarn.npclib.api.profile.ProfileResolver;
import com.github.juliarn.npclib.api.protocol.PlatformPacketAdapter;
import java.util.Objects;
import net.kyori.event.EventBus;
import org.jetbrains.annotations.NotNull;

public abstract class CommonPlatformBuilder<W, P, I> implements Platform.Builder<W, P, I> {

  protected static final boolean DEFAULT_DEBUG = Boolean.getBoolean("npc.lib.debug");
  protected static final ProfileResolver DEFAULT_PROFILE_RESOLVER = ProfileResolver.caching(ProfileResolver.mojang());

  protected boolean debug = DEFAULT_DEBUG;
  protected EventBus<NpcEvent> eventBus;
  protected NpcTracker<W, P, I> npcTracker;
  protected ProfileResolver profileResolver;
  protected PlatformTaskManager taskManager;
  protected NpcActionController actionController;
  protected PlatformWorldAccessor<W> worldAccessor;
  protected PlatformPacketAdapter<W, P, I> packetAdapter;

  @Override
  public Platform.@NotNull Builder<W, P, I> debug(boolean debug) {
    this.debug = debug;
    return this;
  }

  @Override
  public @NotNull Platform.Builder<W, P, I> eventBus(@NotNull EventBus<NpcEvent> eventBus) {
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    return this;
  }

  @Override
  public @NotNull Platform.Builder<W, P, I> npcTracker(@NotNull NpcTracker<W, P, I> npcTracker) {
    this.npcTracker = Objects.requireNonNull(npcTracker, "npcTracker");
    return this;
  }

  @Override
  public Platform.@NotNull Builder<W, P, I> taskManager(@NotNull PlatformTaskManager taskManager) {
    this.taskManager = Objects.requireNonNull(taskManager, "taskManager");
    return this;
  }

  @Override
  public @NotNull Platform.Builder<W, P, I> profileResolver(@NotNull ProfileResolver profileResolver) {
    this.profileResolver = Objects.requireNonNull(profileResolver, "profileResolver");
    return this;
  }

  @Override
  public @NotNull Platform.Builder<W, P, I> worldAccessor(@NotNull PlatformWorldAccessor<W> worldAccessor) {
    this.worldAccessor = Objects.requireNonNull(worldAccessor, "worldAccessor");
    return this;
  }

  @Override
  public @NotNull Platform.Builder<W, P, I> packetFactory(@NotNull PlatformPacketAdapter<W, P, I> packetFactory) {
    this.packetAdapter = Objects.requireNonNull(packetFactory, "packetFactory");
    return this;
  }

  @Override
  public @NotNull Platform<W, P, I> build() {
    // use the default profile resolver if no specific one was specified
    if (this.profileResolver == null) {
      this.profileResolver = DEFAULT_PROFILE_RESOLVER;
    }

    // use a new event bus if no specific one was specified
    if (this.eventBus == null) {
      this.eventBus = EventBus.create(NpcEvent.class);
    }

    return new CommonPlatform<>(
      this.debug,
      Objects.requireNonNull(this.npcTracker, "npcTracker"),
      Objects.requireNonNull(this.profileResolver, "profileResolver"),
      Objects.requireNonNull(this.taskManager, "taskManager"),
      Objects.requireNonNull(this.actionController, "actionController"),
      Objects.requireNonNull(this.eventBus, "eventBus"),
      Objects.requireNonNull(this.worldAccessor, "worldAccessor"),
      Objects.requireNonNull(this.packetAdapter, "packetAdapter"));
  }
}
