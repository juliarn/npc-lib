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

package com.github.juliarn.npclib.api;

import com.github.juliarn.npclib.api.flag.NpcFlag;
import com.github.juliarn.npclib.api.flag.NpcFlaggedBuilder;
import com.github.juliarn.npclib.api.flag.NpcFlaggedObject;
import com.github.juliarn.npclib.api.profile.Profile;
import com.github.juliarn.npclib.api.profile.ProfileResolver;
import com.github.juliarn.npclib.api.protocol.NpcSpecificOutboundPacket;
import com.github.juliarn.npclib.api.protocol.enums.ItemSlot;
import com.github.juliarn.npclib.api.settings.NpcSettings;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public interface Npc<W, P, I, E> extends NpcFlaggedObject {

  NpcFlag<Boolean> LOOK_AT_PLAYER = NpcFlag.flag("imitate_player_look", false);
  NpcFlag<Boolean> HIT_WHEN_PLAYER_HITS = NpcFlag.flag("imitate_player_hit", false);
  NpcFlag<Boolean> SNEAK_WHEN_PLAYER_SNEAKS = NpcFlag.flag("imitate_player_sneak", false);

  int entityId();

  @NotNull Profile.Resolved profile();

  @NotNull W world();

  @NotNull Position position();

  @NotNull NpcSettings<P> settings();

  @NotNull Platform<W, P, I, E> platform();

  @NotNull NpcTracker<W, P, I, E> npcTracker();

  boolean shouldIncludePlayer(@NotNull P player);

  @UnmodifiableView
  @NotNull Collection<P> includedPlayers();

  boolean includesPlayer(@NotNull P player);

  @NotNull Npc<W, P, I, E> addIncludedPlayer(@NotNull P player);

  @NotNull Npc<W, P, I, E> removeIncludedPlayer(@NotNull P player);

  @NotNull Npc<W, P, I, E> unlink();

  @UnmodifiableView
  @NotNull Collection<P> trackedPlayers();

  boolean tracksPlayer(@NotNull P player);

  @NotNull Npc<W, P, I, E> trackPlayer(@NotNull P player);

  @NotNull Npc<W, P, I, E> forceTrackPlayer(@NotNull P player);

  @NotNull Npc<W, P, I, E> stopTrackingPlayer(@NotNull P player);

  @NotNull NpcSpecificOutboundPacket<W, P, I, E> lookAt(@NotNull Position position);

  @NotNull NpcSpecificOutboundPacket<W, P, I, E> changeItem(@NotNull ItemSlot slot, @NotNull I item);

  interface Builder<W, P, I, E> extends NpcFlaggedBuilder<Builder<W, P, I, E>> {

    @NotNull Builder<W, P, I, E> entityId(int id);

    @NotNull Builder<W, P, I, E> position(@NotNull Position position);

    @NotNull Builder<W, P, I, E> profile(@NotNull Profile.Resolved profile);

    default @NotNull CompletableFuture<Builder<W, P, I, E>> profile(@NotNull Profile profile) {
      return this.profile(null, profile);
    }

    @NotNull CompletableFuture<Builder<W, P, I, E>> profile(@Nullable ProfileResolver resolver,
      @NotNull Profile profile);

    @NotNull Builder<W, P, I, E> npcSettings(@NotNull Consumer<NpcSettings.Builder<P>> decorator);

    @NotNull Npc<W, P, I, E> build();

    @NotNull Npc<W, P, I, E> buildAndTrack();
  }
}
