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

package com.github.juliarn.npclib.fabric.controller;

import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.NpcActionController;
import com.github.juliarn.npclib.api.NpcTracker;
import com.github.juliarn.npclib.api.event.ShowNpcEvent;
import com.github.juliarn.npclib.api.event.manager.NpcEventManager;
import com.github.juliarn.npclib.api.flag.NpcFlag;
import com.github.juliarn.npclib.api.protocol.enums.EntityAnimation;
import com.github.juliarn.npclib.api.protocol.meta.EntityMetadataFactory;
import com.github.juliarn.npclib.common.CommonNpcActionController;
import com.github.juliarn.npclib.common.flag.CommonNpcFlaggedBuilder;
import com.github.juliarn.npclib.fabric.util.FabricUtil;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public final class FabricActionController extends CommonNpcActionController {

  private final NpcTracker<ServerLevel, ServerPlayer, ItemStack, Object> npcTracker;

  // based on the given flags
  private final int spawnDistance;
  private final int imitateDistance;

  public FabricActionController(
    @NotNull Map<NpcFlag<?>, Optional<?>> flags,
    @NotNull NpcEventManager eventManager,
    @NotNull NpcTracker<ServerLevel, ServerPlayer, ItemStack, Object> tracker
  ) {
    super(flags);
    this.npcTracker = tracker;

    // pre-calculate flag values
    var spawnDistance = this.flagValueOrDefault(SPAWN_DISTANCE);
    this.spawnDistance = spawnDistance * spawnDistance;

    var imitateDistance = this.flagValueOrDefault(IMITATE_DISTANCE);
    this.imitateDistance = imitateDistance * imitateDistance;

    // register listener to update the npc rotation after it is tracked
    if (this.flagValueOrDefault(NpcActionController.AUTO_SYNC_POSITION_ON_SPAWN)) {
      eventManager.registerEventHandler(ShowNpcEvent.Post.class, event -> {
        ServerPlayer player = event.player();
        var pos = player.position();
        var rot = player.getRotationVector();
        var level = player.serverLevel();

        double distance = FabricUtil.distance(event.npc(), pos);
        if (distance <= this.imitateDistance && event.npc().flagValueOrDefault(Npc.LOOK_AT_PLAYER)) {
          var playerPos = FabricUtil.positionFromPosAndRot(level, pos, rot);
          event.npc().lookAt(playerPos).schedule(player);
        }
      });
    }

    // register listeners to handle player actions
    FabricActionControllerEvents.SERVER_PLAYER_MOVE.register(this::handleMove);
    FabricActionControllerEvents.SERVER_PLAYER_DISCONNECT.register(this::handleQuit);
    FabricActionControllerEvents.SERVER_PLAYER_TOGGLE_SNEAK.register(this::handleToggleSneak);
    FabricActionControllerEvents.SERVER_PLAYER_HAND_SWING.register(this::handlePlayerHandSwing);
    FabricActionControllerEvents.SERVER_PLAYER_LEVEL_CHANGE.register(this::handleLevelChange);
  }

  public static @NotNull NpcActionController.Builder actionControllerBuilder(
    @NotNull NpcEventManager eventManager,
    @NotNull NpcTracker<ServerLevel, ServerPlayer, ItemStack, Object> npcTracker
  ) {
    Objects.requireNonNull(eventManager, "eventManager");
    Objects.requireNonNull(npcTracker, "npcTracker");

    return new FabricActionControllerBuilder(eventManager, npcTracker);
  }

  private void handleMove(@NotNull ServerPlayer player, @Nullable Vec3 pos, @Nullable Vec2 rot) {
    for (var npc : this.npcTracker.trackedNpcs()) {
      // check if the player is still in the same world as the npc
      var npcPos = npc.position();
      var level = player.serverLevel();
      if (!npc.world().equals(level) || !npc.world().hasChunk(npcPos.chunkX(), npcPos.chunkZ())) {
        // if the player is tracked by the npc, stop that
        npc.stopTrackingPlayer(player);
        continue;
      }

      // check if the player moved in / out of any npc tracking distance
      var playerPos = Objects.requireNonNullElse(pos, player.position());
      var distance = FabricUtil.distance(npc, playerPos);
      if (distance > this.spawnDistance) {
        // this will only do something if the player is already tracked by the npc
        npc.stopTrackingPlayer(player);
        continue;
      } else {
        // this will only do something if the player is not already tracked by the npc
        npc.trackPlayer(player);
      }

      // check if we should rotate the npc towards the player
      if (rot != null
        && npc.tracksPlayer(player)
        && distance <= this.imitateDistance
        && npc.flagValueOrDefault(Npc.LOOK_AT_PLAYER)) {
        var playerPosition = FabricUtil.positionFromPosAndRot(level, playerPos, rot);
        npc.lookAt(playerPosition).schedule(player);
      }
    }
  }

  private void handleLevelChange(
    @NotNull ServerPlayer player,
    @Nullable ServerLevel oldLevel,
    @NotNull ServerLevel newLevel
  ) {
    // ensure that we stop tracking the player on NPCs which are not in the same world as the player
    var levelId = newLevel.dimension().location().toString();
    for (var npc : this.npcTracker.trackedNpcs()) {
      if (!npc.position().worldId().equals(levelId)) {
        // the player is no longer in the same world, stop tracking
        npc.stopTrackingPlayer(player);
        continue;
      }

      // the player is now in the same instance as the npc, check if we should track him
      var distance = FabricUtil.distance(npc, player.position());
      if (this.spawnDistance >= distance) {
        npc.trackPlayer(player);
      }
    }
  }

  private void handleToggleSneak(@NotNull ServerPlayer player, boolean sneaking) {
    var levelId = player.serverLevel().dimension().location().toString();
    for (var npc : this.npcTracker.trackedNpcs()) {
      // check if we should imitate the action
      var distance = FabricUtil.distance(npc, player.position());
      if (npc.position().worldId().equals(levelId)
        && npc.tracksPlayer(player)
        && distance <= this.imitateDistance
        && npc.flagValueOrDefault(Npc.SNEAK_WHEN_PLAYER_SNEAKS)) {
        // let the npc sneak as well
        npc.platform().packetFactory()
          .createEntityMetaPacket(EntityMetadataFactory.sneakingMetaFactory(), sneaking)
          .schedule(player, npc);
      }
    }
  }

  private void handlePlayerHandSwing(@NotNull ServerPlayer player) {
    var levelId = player.serverLevel().dimension().location().toString();
    for (var npc : this.npcTracker.trackedNpcs()) {
      // check if we should imitate the action
      var distance = FabricUtil.distance(npc, player.position());
      if (npc.position().worldId().equals(levelId)
        && npc.tracksPlayer(player)
        && distance <= this.imitateDistance
        && npc.flagValueOrDefault(Npc.HIT_WHEN_PLAYER_HITS)) {
        // let the npc left click as well
        npc.platform().packetFactory().createAnimationPacket(EntityAnimation.SWING_MAIN_ARM).schedule(player, npc);
      }
    }
  }

  private void handleQuit(@NotNull ServerPlayer player) {
    for (var npc : this.npcTracker.trackedNpcs()) {
      // check if the npc tracks the player which disconnected and stop tracking him if so
      npc.stopTrackingPlayer(player);
    }
  }

  private static final class FabricActionControllerBuilder
    extends CommonNpcFlaggedBuilder<Builder>
    implements NpcActionController.Builder {

    private final NpcEventManager eventManager;
    private final NpcTracker<ServerLevel, ServerPlayer, ItemStack, Object> npcTracker;

    public FabricActionControllerBuilder(
      @NotNull NpcEventManager eventManager,
      @NotNull NpcTracker<ServerLevel, ServerPlayer, ItemStack, Object> npcTracker
    ) {
      this.eventManager = eventManager;
      this.npcTracker = npcTracker;
    }

    @Override
    public @NotNull NpcActionController build() {
      return new FabricActionController(this.flags, this.eventManager, this.npcTracker);
    }
  }
}
