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

package com.github.juliarn.npclib.api.protocol;

import com.github.juliarn.npclib.api.Npc;
import java.util.Collection;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface OutboundPacket<W, P, I, E> {

  void schedule(@NotNull P player, @NotNull Npc<W, P, I, E> npc);

  default void scheduleForTracked(@NotNull Npc<W, P, I, E> npc) {
    this.schedule(Npc::trackedPlayers, npc);
  }

  default void schedule(@NotNull Function<Npc<W, P, I, E>, Collection<P>> extractor, @NotNull Npc<W, P, I, E> npc) {
    this.schedule(extractor.apply(npc), npc);
  }

  default void schedule(@NotNull Collection<P> players, @NotNull Npc<W, P, I, E> npc) {
    players.forEach(player -> this.schedule(player, npc));
  }

  default @NotNull NpcSpecificOutboundPacket<W, P, I, E> toSpecific(@NotNull Npc<W, P, I, E> targetNpc) {
    return NpcSpecificOutboundPacket.fromOutboundPacket(targetNpc, this);
  }
}
