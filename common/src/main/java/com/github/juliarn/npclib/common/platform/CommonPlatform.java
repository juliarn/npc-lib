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

package com.github.juliarn.npclib.common.platform;

import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.NpcActionController;
import com.github.juliarn.npclib.api.NpcTracker;
import com.github.juliarn.npclib.api.Platform;
import com.github.juliarn.npclib.api.PlatformTaskManager;
import com.github.juliarn.npclib.api.PlatformVersionAccessor;
import com.github.juliarn.npclib.api.PlatformWorldAccessor;
import com.github.juliarn.npclib.api.event.NpcEvent;
import com.github.juliarn.npclib.api.log.PlatformLogger;
import com.github.juliarn.npclib.api.profile.ProfileResolver;
import com.github.juliarn.npclib.api.protocol.PlatformPacketAdapter;
import com.github.juliarn.npclib.common.npc.CommonNpcBuilder;
import java.util.Optional;
import net.kyori.event.EventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CommonPlatform<W, P, I, E> implements Platform<W, P, I, E> {

  protected final E extension;
  protected final boolean debug;
  protected final PlatformLogger logger;
  protected final EventBus<NpcEvent> eventBus;
  protected final NpcTracker<W, P, I, E> npcTracker;
  protected final ProfileResolver profileResolver;
  protected final PlatformTaskManager taskManager;
  protected final NpcActionController actionController;
  protected final PlatformVersionAccessor versionAccessor;
  protected final PlatformWorldAccessor<W> worldAccessor;
  protected final PlatformPacketAdapter<W, P, I, E> packetAdapter;

  public CommonPlatform(
    boolean debug,
    @NotNull E extension,
    @NotNull PlatformLogger logger,
    @NotNull NpcTracker<W, P, I, E> npcTracker,
    @NotNull ProfileResolver profileResolver,
    @NotNull PlatformTaskManager taskManager,
    @Nullable NpcActionController actionController,
    @NotNull PlatformVersionAccessor versionAccessor,
    @NotNull EventBus<NpcEvent> eventBus,
    @NotNull PlatformWorldAccessor<W> worldAccessor,
    @NotNull PlatformPacketAdapter<W, P, I, E> packetAdapter
  ) {
    this.debug = debug;
    this.logger = logger;
    this.extension = extension;
    this.npcTracker = npcTracker;
    this.profileResolver = profileResolver;
    this.taskManager = taskManager;
    this.actionController = actionController;
    this.versionAccessor = versionAccessor;
    this.eventBus = eventBus;
    this.worldAccessor = worldAccessor;
    this.packetAdapter = packetAdapter;

    // register the packet listeners
    this.packetAdapter.initialize(this);
  }

  @Override
  public boolean debug() {
    return this.debug;
  }

  @Override
  public @NotNull E extension() {
    return this.extension;
  }

  @Override
  public @NotNull PlatformLogger logger() {
    return this.logger;
  }

  @Override
  public @NotNull NpcTracker<W, P, I, E> npcTracker() {
    return this.npcTracker;
  }

  @Override
  public @NotNull ProfileResolver profileResolver() {
    return this.profileResolver;
  }

  @Override
  public @NotNull PlatformTaskManager taskManager() {
    return this.taskManager;
  }

  @Override
  public @NotNull Npc.Builder<W, P, I, E> newNpcBuilder() {
    return new CommonNpcBuilder<>(this);
  }

  @Override
  public @NotNull PlatformVersionAccessor versionAccessor() {
    return this.versionAccessor;
  }

  @Override
  public @NotNull EventBus<NpcEvent> eventBus() {
    return this.eventBus;
  }

  @Override
  public @NotNull PlatformWorldAccessor<W> worldAccessor() {
    return this.worldAccessor;
  }

  @Override
  public @NotNull PlatformPacketAdapter<W, P, I, E> packetFactory() {
    return this.packetAdapter;
  }

  @Override
  public @NotNull Optional<NpcActionController> actionController() {
    return Optional.ofNullable(this.actionController);
  }
}
