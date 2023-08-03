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

package com.github.juliarn.npclib.minestom;

import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.NpcActionController;
import com.github.juliarn.npclib.api.NpcTracker;
import com.github.juliarn.npclib.api.Position;
import com.github.juliarn.npclib.api.event.manager.NpcEventManager;
import com.github.juliarn.npclib.api.flag.NpcFlag;
import com.github.juliarn.npclib.api.protocol.enums.EntityAnimation;
import com.github.juliarn.npclib.api.protocol.meta.EntityMetadataFactory;
import com.github.juliarn.npclib.common.CommonNpcActionController;
import com.github.juliarn.npclib.common.flag.CommonNpcFlaggedBuilder;
import com.github.juliarn.npclib.minestom.util.MinestomUtil;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerHandAnimationEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerStartSneakingEvent;
import net.minestom.server.event.player.PlayerStopSneakingEvent;
import net.minestom.server.extensions.Extension;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public final class MinestomActionController extends CommonNpcActionController {

  private final NpcTracker<Instance, Player, ItemStack, Extension> npcTracker;

  // based on the given flags
  private final int spawnDistance;
  private final int imitateDistance;

  public MinestomActionController(
    @NotNull Map<NpcFlag<?>, Optional<?>> flags,
    @NotNull NpcEventManager eventManager,
    @NotNull NpcTracker<Instance, Player, ItemStack, Extension> tracker
  ) {
    super(flags);
    this.npcTracker = tracker;

    // pre-calculate flag values
    int spawnDistance = this.flagValueOrDefault(SPAWN_DISTANCE);
    this.spawnDistance = spawnDistance * spawnDistance;

    int imitateDistance = this.flagValueOrDefault(IMITATE_DISTANCE);
    this.imitateDistance = imitateDistance * imitateDistance;

    // add all listeners we need
    this.registerListeners();
  }

  static @NotNull NpcActionController.Builder actionControllerBuilder(
    @NotNull NpcEventManager eventManager,
    @NotNull NpcTracker<Instance, Player, ItemStack, Extension> npcTracker
  ) {
    Objects.requireNonNull(eventManager, "eventManager");
    Objects.requireNonNull(npcTracker, "npcTracker");

    return new MinestomActionControllerBuilder(eventManager, npcTracker);
  }

  private void registerListeners() {
    MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, this::handleMove);
    MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, this::handlePlayerInstanceSpawn);
    MinecraftServer.getGlobalEventHandler().addListener(PlayerStartSneakingEvent.class, this::handleStartSneak);
    MinecraftServer.getGlobalEventHandler().addListener(PlayerStopSneakingEvent.class, this::handleStopSneak);
    MinecraftServer.getGlobalEventHandler().addListener(PlayerHandAnimationEvent.class, this::handleHandAnimation);
    MinecraftServer.getGlobalEventHandler().addListener(PlayerDisconnectEvent.class, this::handleQuit);
  }

  private void handleMove(@NotNull PlayerMoveEvent event) {
    Pos to = event.getNewPosition();
    Pos from = event.getPlayer().getPosition();

    boolean changedOrientation = from.yaw() != to.yaw() || from.pitch() != to.pitch();
    boolean changedPosition = from.x() != to.x() || from.y() != to.y() || from.z() != to.z();

    // check if any movement happened (event is also called when standing still)
    if (changedPosition || changedOrientation) {
      Player player = event.getPlayer();
      for (Npc<Instance, Player, ItemStack, Extension> npc : this.npcTracker.trackedNpcs()) {
        // check if the chunk of the npc is still loaded
        Position pos = npc.position();
        if (!npc.world().isChunkLoaded(pos.chunkX(), pos.chunkZ())) {
          // if the player is tracked by the npc, stop that
          npc.stopTrackingPlayer(player);
          continue;
        }

        // check if the player moved in / out of any npc tracking distance
        double distance = MinestomUtil.distance(npc, to);
        if (distance > this.spawnDistance) {
          // this will only do something if the player is already tracked by the npc
          npc.stopTrackingPlayer(player);
          continue;
        } else {
          // this will only do something if the player is not already tracked by the npc
          npc.trackPlayer(player);
        }

        // check if we should rotate the npc towards the player
        if (changedPosition
          && npc.tracksPlayer(player)
          && distance <= this.imitateDistance
          && npc.flagValueOrDefault(Npc.LOOK_AT_PLAYER)) {
          npc.lookAt(MinestomUtil.positionFromMinestom(to, event.getInstance())).schedule(player);
        }
      }
    }
  }

  private void handlePlayerInstanceSpawn(@NotNull PlayerSpawnEvent event) {
    // no need to do that on the first spawn - no npc should track the player
    if (!event.isFirstSpawn()) {
      // ensure that we stop tracking the player on NPCs which are not in the same world as the player
      String instanceId = event.getSpawnInstance().getUniqueId().toString();
      for (Npc<Instance, Player, ItemStack, Extension> npc : this.npcTracker.trackedNpcs()) {
        if (!npc.position().worldId().equals(instanceId)) {
          // the player is no longer in the same world, stop tracking
          npc.stopTrackingPlayer(event.getPlayer());
          continue;
        }

        // the player is now in the same instance as the npc, check if we should track him
        double distance = MinestomUtil.distance(npc, event.getPlayer().getPosition());
        if (this.spawnDistance >= distance) {
          npc.trackPlayer(event.getPlayer());
        }
      }
    }
  }

  private void handleStartSneak(@NotNull PlayerStartSneakingEvent event) {
    this.handleToggleSneak(event.getPlayer(), event.getInstance(), true);
  }

  private void handleStopSneak(@NotNull PlayerStopSneakingEvent event) {
    this.handleToggleSneak(event.getPlayer(), event.getInstance(), false);
  }

  private void handleToggleSneak(@NotNull Player player, @NotNull Instance instance, boolean sneakActive) {
    String instanceId = instance.getUniqueId().toString();
    for (Npc<Instance, Player, ItemStack, Extension> npc : this.npcTracker.trackedNpcs()) {
      double distance = MinestomUtil.distance(npc, player.getPosition());

      // check if we should imitate the action
      if (Objects.equals(instanceId, npc.position().worldId())
        && npc.tracksPlayer(player)
        && distance <= this.imitateDistance
        && npc.flagValueOrDefault(Npc.SNEAK_WHEN_PLAYER_SNEAKS)) {
        // let the npc sneak as well
        npc.platform().packetFactory()
          .createEntityMetaPacket(EntityMetadataFactory.sneakingMetaFactory(), sneakActive)
          .schedule(player, npc);
      }
    }
  }

  private void handleHandAnimation(@NotNull PlayerHandAnimationEvent event) {
    Player player = event.getPlayer();
    String instanceId = event.getInstance().getUniqueId().toString();
    for (Npc<Instance, Player, ItemStack, Extension> npc : this.npcTracker.trackedNpcs()) {
      double distance = MinestomUtil.distance(npc, player.getPosition());

      // check if we should imitate the action
      if (Objects.equals(instanceId, npc.position().worldId())
        && npc.tracksPlayer(player)
        && distance <= this.imitateDistance
        && npc.flagValueOrDefault(Npc.HIT_WHEN_PLAYER_HITS)) {
        // let the npc left click as well
        npc.platform().packetFactory().createAnimationPacket(EntityAnimation.SWING_MAIN_ARM).schedule(player, npc);
      }
    }
  }

  private void handleQuit(@NotNull PlayerDisconnectEvent event) {
    for (Npc<Instance, Player, ItemStack, Extension> npc : this.npcTracker.trackedNpcs()) {
      // check if the npc tracks the player which disconnected and stop tracking him if so
      npc.stopTrackingPlayer(event.getPlayer());
    }
  }

  private static final class MinestomActionControllerBuilder
    extends CommonNpcFlaggedBuilder<Builder>
    implements NpcActionController.Builder {

    private final NpcEventManager eventManager;
    private final NpcTracker<Instance, Player, ItemStack, Extension> npcTracker;

    public MinestomActionControllerBuilder(
      @NotNull NpcEventManager eventManager,
      @NotNull NpcTracker<Instance, Player, ItemStack, Extension> npcTracker
    ) {
      this.eventManager = eventManager;
      this.npcTracker = npcTracker;
    }

    @Override
    public @NotNull NpcActionController build() {
      return new MinestomActionController(this.flags, this.eventManager, this.npcTracker);
    }
  }
}
