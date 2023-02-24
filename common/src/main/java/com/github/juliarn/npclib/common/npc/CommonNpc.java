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
import com.github.juliarn.npclib.api.NpcTracker;
import com.github.juliarn.npclib.api.Platform;
import com.github.juliarn.npclib.api.Position;
import com.github.juliarn.npclib.api.flag.NpcFlag;
import com.github.juliarn.npclib.api.profile.Profile;
import com.github.juliarn.npclib.api.protocol.NpcSpecificOutboundPacket;
import com.github.juliarn.npclib.api.protocol.enums.EntityAnimation;
import com.github.juliarn.npclib.api.protocol.enums.ItemSlot;
import com.github.juliarn.npclib.api.protocol.enums.PlayerInfoAction;
import com.github.juliarn.npclib.api.protocol.meta.EntityMetadataFactory;
import com.github.juliarn.npclib.api.settings.NpcSettings;
import com.github.juliarn.npclib.api.util.Util;
import com.github.juliarn.npclib.common.event.DefaultHideNpcEvent;
import com.github.juliarn.npclib.common.event.DefaultShowNpcEvent;
import com.github.juliarn.npclib.common.flag.CommonNpcFlaggedObject;
import com.github.juliarn.npclib.common.util.EventDispatcher;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

public class CommonNpc<W, P, I, E> extends CommonNpcFlaggedObject implements Npc<W, P, I, E> {

  protected final int entityId;
  protected final Profile.Resolved profile;

  protected final W world;
  protected final Position pos;

  protected final Platform<W, P, I, E> platform;
  protected final NpcSettings<P> npcSettings;

  protected final Set<P> trackedPlayers = Collections.synchronizedSet(new HashSet<>());
  protected final Set<P> includedPlayers = Collections.synchronizedSet(new HashSet<>());

  public CommonNpc(
    @NotNull Map<NpcFlag<?>, Optional<?>> flags,
    int entityId,
    @NotNull Profile.Resolved profile,
    @NotNull W world,
    @NotNull Position pos,
    @NotNull Platform<W, P, I, E> platform,
    @NotNull NpcSettings<P> npcSettings
  ) {
    super(flags);
    this.entityId = entityId;
    this.profile = profile;
    this.world = world;
    this.pos = pos;
    this.platform = platform;
    this.npcSettings = npcSettings;
  }

  @Override
  public int entityId() {
    return this.entityId;
  }

  @Override
  public @NotNull Profile.Resolved profile() {
    return this.profile;
  }

  @Override
  public @NotNull W world() {
    return this.world;
  }

  @Override
  public @NotNull Position position() {
    return this.pos;
  }

  @Override
  public @NotNull NpcSettings<P> settings() {
    return this.npcSettings;
  }

  @Override
  public @NotNull Platform<W, P, I, E> platform() {
    return this.platform;
  }

  @Override
  public @NotNull NpcTracker<W, P, I, E> npcTracker() {
    return this.platform.npcTracker();
  }

  @Override
  public boolean shouldIncludePlayer(@NotNull P player) {
    return this.npcSettings.trackingRule().shouldTrack(this, player);
  }

  @Override
  public @UnmodifiableView @NotNull Collection<P> includedPlayers() {
    return Collections.unmodifiableSet(this.includedPlayers);
  }

  @Override
  public boolean includesPlayer(@NotNull P player) {
    return this.includedPlayers.contains(player);
  }

  @Override
  public @NotNull Npc<W, P, I, E> addIncludedPlayer(@NotNull P player) {
    this.includedPlayers.add(player);
    return this;
  }

  @Override
  public @NotNull Npc<W, P, I, E> removeIncludedPlayer(@NotNull P player) {
    this.includedPlayers.remove(player);
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @NotNull Npc<W, P, I, E> unlink() {
    // remove this npc from the tracked ones, do it first to prevent further player tracking
    this.npcTracker().stopTrackingNpc(this);

    // remove this npc for all tracked players
    Object[] players = this.trackedPlayers.toArray();
    for (Object player : players) {
      this.stopTrackingPlayer((P) player);
    }

    // for chaining
    return this;
  }

  @Override
  public @UnmodifiableView @NotNull Collection<P> trackedPlayers() {
    return Collections.unmodifiableSet(this.trackedPlayers);
  }

  @Override
  public boolean tracksPlayer(@NotNull P player) {
    return this.trackedPlayers.contains(player);
  }

  @Override
  public @NotNull Npc<W, P, I, E> trackPlayer(@NotNull P player) {
    // check if we should track the player
    if (this.shouldIncludePlayer(player)) {
      return this.forceTrackPlayer(player);
    }

    // nothing to do
    return this;
  }

  @Override
  public @NotNull Npc<W, P, I, E> forceTrackPlayer(@NotNull P player) {
    // check if the player is not already tracked
    if (this.trackedPlayers.add(player)) {
      // break early if the add is not wanted by plugin
      if (EventDispatcher.dispatch(this.platform, DefaultShowNpcEvent.pre(this, player)).cancelled()) {
        return this;
      }

      // send the player info packet
      this.platform.packetFactory().createPlayerInfoPacket(PlayerInfoAction.ADD_PLAYER).schedule(player, this);

      // schedule the spawn a bit delayed
      this.platform.taskManager().scheduleDelayedAsync(() -> {
        // spawn the player entity
        this.platform.packetFactory().createEntitySpawnPacket().schedule(player, this);

        // post the finish of the add to all plugins
        EventDispatcher.dispatch(this.platform, DefaultShowNpcEvent.post(this, player));
      }, 10);
    }

    // for chaining
    return this;
  }

  @Override
  public @NotNull Npc<W, P, I, E> stopTrackingPlayer(@NotNull P player) {
    // check if the player was previously tracked
    if (this.trackedPlayers.remove(player)) {
      // break early if the removal is not wanted by plugin
      if (EventDispatcher.dispatch(this.platform, DefaultHideNpcEvent.pre(this, player)).cancelled()) {
        return this;
      }

      // schedule an entity remove (the player list change is not needed normally, but to make sure that the npc is gone)
      this.platform.packetFactory().createEntityRemovePacket().schedule(player, this);
      this.platform.packetFactory().createPlayerInfoPacket(PlayerInfoAction.REMOVE_PLAYER).schedule(player, this);

      // post the finish of the removal to all plugins
      EventDispatcher.dispatch(this.platform, DefaultHideNpcEvent.post(this, player));
    }

    // for chaining
    return this;
  }

  @Override
  public @NotNull NpcSpecificOutboundPacket<W, P, I, E> lookAt(@NotNull Position position) {
    double diffX = position.x() - this.pos.x();
    double diffY = position.y() - this.pos.y();
    double diffZ = position.z() - this.pos.z();

    double distanceXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
    double distanceY = Math.sqrt(distanceXZ * distanceXZ + diffY * diffY);

    double yaw = Math.toDegrees(Math.acos(diffX / distanceXZ));
    double pitch = Math.toDegrees(Math.acos(diffY / distanceY)) - 90;

    // correct yaw according to difference
    if (diffZ < 0) {
      yaw += Math.abs(180 - yaw) * 2;
    }
    yaw -= 90;

    return this.platform.packetFactory().createRotationPacket((float) yaw, (float) pitch).toSpecific(this);
  }

  @Override
  public @NotNull NpcSpecificOutboundPacket<W, P, I, E> playAnimation(@NotNull EntityAnimation animation) {
    return this.platform.packetFactory().createAnimationPacket(animation).toSpecific(this);
  }

  @Override
  public @NotNull NpcSpecificOutboundPacket<W, P, I, E> changeItem(@NotNull ItemSlot slot, @NotNull I item) {
    return this.platform.packetFactory().createEquipmentPacket(slot, item).toSpecific(this);
  }

  @Override
  public @NotNull <T, O> NpcSpecificOutboundPacket<W, P, I, E> changeMetadata(
    @NotNull EntityMetadataFactory<T, O> metadata,
    @NotNull T value
  ) {
    return this.platform.packetFactory().createEntityMetaPacket(value, metadata).toSpecific(this);
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(this.entityId());
  }

  @Override
  public boolean equals(Object obj) {
    return Util.equals(Npc.class, this, obj, (orig, comp) -> orig.entityId() == comp.entityId());
  }
}
