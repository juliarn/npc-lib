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

final class DefaultNpcSpecificOutboundPacket<W, P, I, E> implements NpcSpecificOutboundPacket<W, P, I, E> {

  private final Npc<W, P, I, E> target;
  private final OutboundPacket<W, P, I, E> outboundPacket;

  public DefaultNpcSpecificOutboundPacket(
    @NotNull Npc<W, P, I, E> target,
    @NotNull OutboundPacket<W, P, I, E> outboundPacket
  ) {
    this.target = target;
    this.outboundPacket = outboundPacket;
  }

  @Override
  public @NotNull Npc<W, P, I, E> npc() {
    return this.target;
  }

  @Override
  public void scheduleForTracked() {
    this.outboundPacket.scheduleForTracked(this.target);
  }

  @Override
  public void schedule(@NotNull P player) {
    this.outboundPacket.schedule(player, this.target);
  }

  @Override
  public void schedule(@NotNull Collection<P> players) {
    this.outboundPacket.schedule(players, this.target);
  }

  @Override
  public void schedule(@NotNull Function<Npc<W, P, I, E>, Collection<P>> extractor) {
    this.outboundPacket.schedule(extractor, this.target);
  }
}
