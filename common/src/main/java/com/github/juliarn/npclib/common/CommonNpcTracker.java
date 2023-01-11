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

package com.github.juliarn.npclib.common;

import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.NpcTracker;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public class CommonNpcTracker<W, P, I, E> implements NpcTracker<W, P, I, E> {

  protected final Set<Npc<W, P, I, E>> trackedNpcs = Collections.synchronizedSet(new HashSet<>());

  public static @NotNull <W, P, I, E> CommonNpcTracker<W, P, I, E> newNpcTracker() {
    return new CommonNpcTracker<>();
  }

  @Override
  public @Nullable Npc<W, P, I, E> npcById(int entityId) {
    for (Npc<W, P, I, E> trackedNpc : this.trackedNpcs) {
      if (trackedNpc.entityId() == entityId) {
        return trackedNpc;
      }
    }

    return null;
  }

  @Override
  public @Nullable Npc<W, P, I, E> npcByUniqueId(@NotNull UUID uniqueId) {
    for (Npc<W, P, I, E> trackedNpc : this.trackedNpcs) {
      if (trackedNpc.profile().uniqueId().equals(uniqueId)) {
        return trackedNpc;
      }
    }

    return null;
  }

  @Override
  public void trackNpc(@NotNull Npc<W, P, I, E> npc) {
    this.trackedNpcs.add(npc);
  }

  @Override
  public void stopTrackingNpc(@NotNull Npc<W, P, I, E> npc) {
    this.trackedNpcs.remove(npc);
  }

  @Override
  public @UnmodifiableView @NotNull Collection<Npc<W, P, I, E>> trackedNpcs() {
    return Collections.unmodifiableCollection(this.trackedNpcs);
  }
}
