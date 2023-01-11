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

package com.github.juliarn.npclib.common.npc;

import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.Platform;
import com.github.juliarn.npclib.api.Position;
import com.github.juliarn.npclib.api.profile.Profile;
import com.github.juliarn.npclib.api.profile.ProfileResolver;
import com.github.juliarn.npclib.api.settings.NpcSettings;
import com.github.juliarn.npclib.common.flag.CommonNpcFlaggedBuilder;
import com.github.juliarn.npclib.common.settings.CommonNpcSettingsBuilder;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CommonNpcBuilder<W, P, I, E>
  extends CommonNpcFlaggedBuilder<Npc.Builder<W, P, I, E>>
  implements Npc.Builder<W, P, I, E> {

  protected final Platform<W, P, I, E> platform;

  protected int entityId = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);

  protected W world;
  protected Position pos;

  protected Profile.Resolved profile;
  protected NpcSettings<P> npcSettings;

  public CommonNpcBuilder(@NotNull Platform<W, P, I, E> platform) {
    this.platform = platform;
  }

  @Override
  public @NotNull Npc.Builder<W, P, I, E> entityId(int id) {
    // validate the npc entity id
    if (id < 0) {
      throw new IllegalArgumentException("NPC entity id must be positive");
    }

    this.entityId = id;
    return this;
  }

  @Override
  public @NotNull Npc.Builder<W, P, I, E> position(@NotNull Position position) {
    Objects.requireNonNull(position, "position");

    // try to resolve the world from the given position
    W world = this.platform.worldAccessor().resolveWorldFromIdentifier(position.worldId());
    if (world == null) {
      throw new IllegalArgumentException("Could not resolve world from identifier: " + position.worldId());
    }

    // store both, world and position
    this.world = world;
    this.pos = position;

    return this;
  }

  @Override
  public @NotNull Npc.Builder<W, P, I, E> profile(@NotNull Profile.Resolved profile) {
    this.profile = Objects.requireNonNull(profile, "profile");
    return this;
  }

  @Override
  public @NotNull CompletableFuture<Npc.Builder<W, P, I, E>> profile(
    @Nullable ProfileResolver resolver,
    @NotNull Profile profile
  ) {
    // use the default platform resolver if no resolver is given
    if (resolver == null) {
      resolver = this.platform.profileResolver();
    }

    // resolve the profile using the given resolver or the platform resolver
    return resolver.resolveProfile(profile).thenApply(this::profile);
  }

  @Override
  public @NotNull Npc.Builder<W, P, I, E> npcSettings(@NotNull Consumer<NpcSettings.Builder<P>> decorator) {
    // build the npc settings
    NpcSettings.Builder<P> builder = new CommonNpcSettingsBuilder<>();
    decorator.accept(builder);
    this.npcSettings = builder.build();

    return this;
  }

  @Override
  public @NotNull Npc<W, P, I, E> build() {
    // fill in empty npc settings if not given
    if (this.npcSettings == null) {
      this.npcSettings(builder -> {
      });
    }

    return new CommonNpc<>(
      this.flags,
      this.entityId,
      Objects.requireNonNull(this.profile, "profile must be given"),
      Objects.requireNonNull(this.world, "world and position must be given"),
      Objects.requireNonNull(this.pos, "world and position must be given"),
      this.platform,
      Objects.requireNonNull(this.npcSettings, "npc settings must be given"));
  }

  @Override
  public @NotNull Npc<W, P, I, E> buildAndTrack() {
    Npc<W, P, I, E> npc = this.build();
    this.platform.npcTracker().trackNpc(npc);

    return npc;
  }
}
