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

package com.github.juliarn.npclib.fabric;

import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.NpcActionController;
import com.github.juliarn.npclib.api.NpcTracker;
import com.github.juliarn.npclib.api.Position;
import com.github.juliarn.npclib.api.event.ShowNpcEvent;
import com.github.juliarn.npclib.api.event.manager.NpcEventManager;
import com.github.juliarn.npclib.api.flag.NpcFlag;
import com.github.juliarn.npclib.api.protocol.meta.EntityMetadataFactory;
import com.github.juliarn.npclib.common.CommonNpcActionController;
import com.github.juliarn.npclib.common.flag.CommonNpcFlaggedBuilder;
import com.github.juliarn.npclib.fabric.event.ServerPlayerMoveEvent;
import com.github.juliarn.npclib.fabric.event.ServerPlayerToggleSneakEvent;
import com.github.juliarn.npclib.fabric.util.FabricUtil;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public final class FabricActionController extends CommonNpcActionController {

  private final NpcTracker<World, ServerPlayerEntity, ItemStack, Object> npcTracker;

  // based on the given flags
  private final int spawnDistance;
  private final int imitateDistance;

  public FabricActionController(
    @NotNull Map<NpcFlag<?>, Optional<?>> flags,
    @NotNull NpcEventManager eventManager,
    @NotNull NpcTracker<World, ServerPlayerEntity, ItemStack, Object> tracker
  ) {
    super(flags);
    this.npcTracker = tracker;

    // pre-calculate flag values
    int spawnDistance = this.flagValueOrDefault(SPAWN_DISTANCE);
    this.spawnDistance = spawnDistance * spawnDistance;

    int imitateDistance = this.flagValueOrDefault(IMITATE_DISTANCE);
    this.imitateDistance = imitateDistance * imitateDistance;

    // register listener to update the npc rotation after it is tracked
    if (this.flagValueOrDefault(NpcActionController.AUTO_SYNC_POSITION_ON_SPAWN)) {
      eventManager.registerEventHandler(ShowNpcEvent.Post.class, event -> {
        /*Player player = event.player();
        Pos to = player.getPosition();
        Instance instance = player.getInstance();

        // check if the player is within the imitate distance and spawned into an instance
        // in normal cases the instance check should no evaluate to false at this point
        double distance = MinestomUtil.distance(event.npc(), to);
        if (instance != null
          && distance <= this.imitateDistance
          && event.npc().flagValueOrDefault(Npc.LOOK_AT_PLAYER)) {
          event.npc().lookAt(MinestomUtil.positionFromMinestom(to, instance)).schedule(player);
        }*/
      });
    }

    // add all listeners we need
    this.registerListeners();
  }

  static @NotNull NpcActionController.Builder actionControllerBuilder(
    @NotNull NpcEventManager eventManager,
    @NotNull NpcTracker<World, ServerPlayerEntity, ItemStack, Object> npcTracker
  ) {
    Objects.requireNonNull(eventManager, "eventManager");
    Objects.requireNonNull(npcTracker, "npcTracker");

    return new FabricActionControllerBuilder(eventManager, npcTracker);
  }

  private void registerListeners() {
    System.out.println("###HIIII");
    ServerPlayerMoveEvent.EVENT.register(this::handleMove);
    ServerPlayConnectionEvents.DISCONNECT.register(this::handleQuit);
    ServerPlayerToggleSneakEvent.EVENT.register((player, isSneaking) -> {
      if (isSneaking) {
        handleStartSneak(player);
      } else {
        handleStopSneak(player);
      }
    });
    /*MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, this::handlePlayerInstanceSpawn);
    MinecraftServer.getGlobalEventHandler().addListener(PlayerHandAnimationEvent.class, this::handleHandAnimation);*/
  }

  private void handleQuit(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer minecraftServer) {
    for (Npc<World, ServerPlayerEntity, ItemStack, Object> npc : this.npcTracker.trackedNpcs()) {
      // check if the npc tracks the player which disconnected and stop tracking him if so
      npc.stopTrackingPlayer(serverPlayNetworkHandler.player);
    }
  }

  private void handleMove(ServerPlayerEntity player, Vec3d from, Vec3d to) {
    //Pos to = event.getNewPosition();
    //Pos from = event.getPlayer().getPosition();

    //boolean changedOrientation = from.yaw() != to.yaw() || from.pitch() != to.pitch();
    //boolean changedPosition = from.x() != to.x() || from.y() != to.y() || from.z() != to.z();
    boolean changedOrientation = true;
    boolean changedPosition = true;

    // check if any movement happened (event is also called when standing still)
    if (changedPosition || changedOrientation) {

      for (Npc<World, ServerPlayerEntity, ItemStack, Object> npc : this.npcTracker.trackedNpcs()) {
        // check if the chunk of the npc is still loaded
        Position pos = npc.position();
        if (!npc.world().isChunkLoaded(pos.chunkX(), pos.chunkZ())) {
          // if the player is tracked by the npc, stop that
          npc.stopTrackingPlayer(player);
          continue;
        }

        // check if the player moved in / out of any npc tracking distance
        double distance = FabricUtil.distance(npc, to);
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
          npc.lookAt(FabricUtil.positionFromMinestom(to, player.getServerWorld())).schedule(player);
        }
      }
    }
  }

  private void handleStartSneak(ServerPlayerEntity player) {
    this.handleToggleSneak(player, player.getServerWorld(), true);
  }

  private void handleStopSneak(ServerPlayerEntity player) {
    this.handleToggleSneak(player, player.getServerWorld(), false);
  }

  private void handleToggleSneak(@NotNull ServerPlayerEntity player, @NotNull World instance, boolean sneakActive) {
    String instanceId = instance.getRegistryKey().getRegistry().toString();
    for (Npc<World, ServerPlayerEntity, ItemStack, Object> npc : this.npcTracker.trackedNpcs()) {
      double distance = FabricUtil.distance(npc, player.getPos());

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

 /* private void handlePlayerInstanceSpawn(@NotNull PlayerSpawnEvent event) {
    // ensure that we stop tracking the player on NPCs which are not in the same world as the player
    String instanceId = event.getInstance().getUniqueId().toString();
    for (Npc<Instance, Player, ItemStack, Object> npc : this.npcTracker.trackedNpcs()) {
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

  /*private void handleHandAnimation(@NotNull PlayerHandAnimationEvent event) {
    Player player = event.getPlayer();
    String instanceId = event.getInstance().getUniqueId().toString();
    for (Npc<Instance, Player, ItemStack, Object> npc : this.npcTracker.trackedNpcs()) {
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
  }*/

  private static final class FabricActionControllerBuilder
    extends CommonNpcFlaggedBuilder<Builder>
    implements Builder {

    private final NpcEventManager eventManager;
    private final NpcTracker<World, ServerPlayerEntity, ItemStack, Object> npcTracker;

    public FabricActionControllerBuilder(
      @NotNull NpcEventManager eventManager,
      @NotNull NpcTracker<World, ServerPlayerEntity, ItemStack, Object> npcTracker
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
