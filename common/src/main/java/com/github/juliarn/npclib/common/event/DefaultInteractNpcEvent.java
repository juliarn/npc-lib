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

package com.github.juliarn.npclib.common.event;

import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.event.InteractNpcEvent;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class DefaultInteractNpcEvent extends CommonPlayerNpcEvent implements InteractNpcEvent {

  private final Hand hand;
  private boolean cancelled = false;

  private DefaultInteractNpcEvent(@NotNull Npc<?, ?, ?, ?> npc, @NotNull Object player, @NotNull Hand hand) {
    super(npc, player);
    this.hand = hand;
  }

  public static @NotNull InteractNpcEvent interactNpc(
    @NotNull Npc<?, ?, ?, ?> npc,
    @NotNull Object player,
    @NotNull Hand hand
  ) {
    Objects.requireNonNull(npc, "npc");
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(hand, "hand");

    return new DefaultInteractNpcEvent(npc, player, hand);
  }

  @Override
  public boolean cancelled() {
    return this.cancelled;
  }

  @Override
  public void cancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }

  @Override
  public @NotNull Hand hand() {
    return this.hand;
  }
}
