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

package com.github.juliarn.npclib.api;

import com.github.juliarn.npclib.api.event.NpcEvent;
import com.github.juliarn.npclib.api.profile.ProfileResolver;
import com.github.juliarn.npclib.api.protocol.PlatformPacketAdapter;
import java.util.Optional;
import java.util.function.Consumer;
import net.kyori.event.EventBus;
import org.jetbrains.annotations.NotNull;

public interface Platform<W, P, I> {

  boolean debug();

  @NotNull EventBus<NpcEvent> eventBus();

  @NotNull NpcTracker<W, P, I> npcTracker();

  @NotNull ProfileResolver profileResolver();

  @NotNull PlatformTaskManager taskManager();

  @NotNull Npc.Builder<W, P, I> newNpcBuilder();

  @NotNull PlatformWorldAccessor<W> worldAccessor();

  @NotNull PlatformPacketAdapter<W, P, I> packetFactory();

  @NotNull Optional<NpcActionController> actionController();

  interface Builder<W, P, I> {

    @NotNull Builder<W, P, I> debug(boolean debug);

    @NotNull Builder<W, P, I> eventBus(@NotNull EventBus<NpcEvent> eventBus);

    @NotNull Builder<W, P, I> npcTracker(@NotNull NpcTracker<W, P, I> npcTracker);

    @NotNull Builder<W, P, I> taskManager(@NotNull PlatformTaskManager taskManager);

    @NotNull Builder<W, P, I> profileResolver(@NotNull ProfileResolver profileResolver);

    @NotNull Builder<W, P, I> worldAccessor(@NotNull PlatformWorldAccessor<W> worldAccessor);

    @NotNull Builder<W, P, I> packetFactory(@NotNull PlatformPacketAdapter<W, P, I> packetFactory);

    @NotNull Builder<W, P, I> actionController(@NotNull Consumer<NpcActionController.Builder> decorator);

    @NotNull Platform<W, P, I> build();
  }
}
