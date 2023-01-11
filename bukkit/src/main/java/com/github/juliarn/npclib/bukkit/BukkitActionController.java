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

package com.github.juliarn.npclib.bukkit;

import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.NpcActionController;
import com.github.juliarn.npclib.api.NpcTracker;
import com.github.juliarn.npclib.api.PlatformVersionAccessor;
import com.github.juliarn.npclib.api.Position;
import com.github.juliarn.npclib.api.event.NpcEvent;
import com.github.juliarn.npclib.api.event.ShowNpcEvent;
import com.github.juliarn.npclib.api.flag.NpcFlag;
import com.github.juliarn.npclib.api.protocol.enums.EntityAnimation;
import com.github.juliarn.npclib.api.protocol.enums.PlayerInfoAction;
import com.github.juliarn.npclib.api.protocol.meta.EntityMetadataFactory;
import com.github.juliarn.npclib.bukkit.util.BukkitPlatformUtil;
import com.github.juliarn.npclib.common.CommonNpcActionController;
import com.github.juliarn.npclib.common.flag.CommonNpcFlaggedBuilder;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.kyori.event.EventBus;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class BukkitActionController extends CommonNpcActionController implements Listener {

  private final NpcTracker<World, Player, ItemStack, Plugin> npcTracker;

  // based on the given flags
  private final int spawnDistance;
  private final int imitateDistance;

  public BukkitActionController(
    @NotNull Map<NpcFlag<?>, Optional<?>> flags,
    @NotNull Plugin plugin,
    @NotNull EventBus<NpcEvent> eventBus,
    @NotNull PlatformVersionAccessor versionAccessor,
    @NotNull NpcTracker<World, Player, ItemStack, Plugin> tracker
  ) {
    super(flags);
    this.npcTracker = tracker;

    // add all listeners
    plugin.getServer().getPluginManager().registerEvents(this, plugin);

    // register a listener for the post spawn event if we need to send out an update to remove the spawned player
    if (!versionAccessor.atLeast(1, 19, 3)) {
      eventBus.subscribe(ShowNpcEvent.Post.class, event -> {
        // remove the npc from the tab list after the given amount of time (never smaller than 0 because of validation)
        int tabRemovalTicks = this.flagValueOrDefault(TAB_REMOVAL_TICKS);
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
          // schedule the removal of the player from the tab list, can be done async
          Player player = event.player();
          event.npc().platform().packetFactory()
            .createPlayerInfoPacket(PlayerInfoAction.REMOVE_PLAYER)
            .schedule(player, event.npc());
        }, tabRemovalTicks);
      });
    }

    // pre-calculate flag values
    int spawnDistance = this.flagValueOrDefault(SPAWN_DISTANCE);
    this.spawnDistance = spawnDistance * spawnDistance;

    int imitateDistance = this.flagValueOrDefault(IMITATE_DISTANCE);
    this.imitateDistance = imitateDistance * imitateDistance;
  }

  static @NotNull NpcActionController.Builder actionControllerBuilder(
    @NotNull Plugin plugin,
    @NotNull EventBus<NpcEvent> eventBus,
    @NotNull PlatformVersionAccessor versionAccessor,
    @NotNull NpcTracker<World, Player, ItemStack, Plugin> npcTracker
  ) {
    Objects.requireNonNull(plugin, "plugin");
    Objects.requireNonNull(eventBus, "eventBus");
    Objects.requireNonNull(npcTracker, "npcTracker");
    Objects.requireNonNull(versionAccessor, "versionAccessor");

    return new BukkitActionControllerBuilder(plugin, eventBus, versionAccessor, npcTracker);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void handleMove(@NotNull PlayerMoveEvent event) {
    Location to = event.getTo();
    Location from = event.getFrom();

    boolean changedWorld = !Objects.equals(from.getWorld(), to.getWorld());
    boolean changedOrientation = from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch();
    boolean changedPosition = from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ();

    // check if any movement happened (event is also called when standing still)
    if (changedPosition || changedOrientation || changedWorld) {
      Player player = event.getPlayer();
      for (Npc<World, Player, ItemStack, Plugin> npc : this.npcTracker.trackedNpcs()) {
        // check if the player is still in the same world as the npc
        Position pos = npc.position();
        if (!npc.world().equals(player.getWorld()) || !npc.world().isChunkLoaded(pos.chunkX(), pos.chunkZ())) {
          // if the player is tracked by the npc, stop that
          npc.stopTrackingPlayer(player);
          continue;
        }

        // check if the player moved in / out of any npc tracking distance
        double distance = BukkitPlatformUtil.distance(npc, to);
        if (distance > this.spawnDistance) {
          // this will only do something if the player is already tracked by the npc
          npc.stopTrackingPlayer(player);
          continue;
        } else {
          // this will only do something if the player is not already tracked by the npc
          npc.trackPlayer(player);
        }

        // check if we should rotate the npc towards the player
        if (changedOrientation
          && npc.tracksPlayer(player)
          && distance <= this.imitateDistance
          && npc.flagValueOrDefault(Npc.LOOK_AT_PLAYER)) {
          npc.lookAt(BukkitPlatformUtil.positionFromBukkitLegacy(to)).schedule(player);
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void handleSneak(@NotNull PlayerToggleSneakEvent event) {
    Player player = event.getPlayer();
    for (Npc<World, Player, ItemStack, Plugin> npc : this.npcTracker.trackedNpcs()) {
      double distance = BukkitPlatformUtil.distance(npc, player.getLocation());

      // check if we should imitate the action
      if (Objects.equals(player.getWorld(), npc.world())
        && npc.tracksPlayer(player)
        && distance <= this.imitateDistance
        && npc.flagValueOrDefault(Npc.SNEAK_WHEN_PLAYER_SNEAKS)) {
        // let the npc sneak as well
        npc.platform().packetFactory()
          .createEntityMetaPacket(event.isSneaking(), EntityMetadataFactory.sneakingMetaFactory())
          .schedule(player, npc);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void handleLeftClick(@NotNull PlayerInteractEvent event) {
    if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
      Player player = event.getPlayer();
      for (Npc<World, Player, ItemStack, Plugin> npc : this.npcTracker.trackedNpcs()) {
        double distance = BukkitPlatformUtil.distance(npc, player.getLocation());

        // check if we should imitate the action
        if (Objects.equals(player.getWorld(), npc.world())
          && npc.tracksPlayer(player)
          && distance <= this.imitateDistance
          && npc.flagValueOrDefault(Npc.HIT_WHEN_PLAYER_HITS)) {
          // let the npc left click as well
          npc.platform().packetFactory().createAnimationPacket(EntityAnimation.SWING_MAIN_ARM).schedule(player, npc);
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void handleQuit(@NotNull PlayerQuitEvent event) {
    for (Npc<World, Player, ItemStack, Plugin> npc : this.npcTracker.trackedNpcs()) {
      // check if the npc tracks the player which disconnected and stop tracking him if so
      npc.stopTrackingPlayer(event.getPlayer());
    }
  }

  private static final class BukkitActionControllerBuilder
    extends CommonNpcFlaggedBuilder<NpcActionController.Builder>
    implements NpcActionController.Builder {

    private final Plugin plugin;
    private final EventBus<NpcEvent> eventBus;
    private final PlatformVersionAccessor versionAccessor;
    private final NpcTracker<World, Player, ItemStack, Plugin> npcTracker;

    public BukkitActionControllerBuilder(
      @NotNull Plugin plugin,
      @NotNull EventBus<NpcEvent> eventBus,
      @NotNull PlatformVersionAccessor versionAccessor,
      @NotNull NpcTracker<World, Player, ItemStack, Plugin> npcTracker
    ) {
      this.plugin = plugin;
      this.eventBus = eventBus;
      this.npcTracker = npcTracker;
      this.versionAccessor = versionAccessor;
    }

    @Override
    public @NotNull NpcActionController build() {
      return new BukkitActionController(this.flags, this.plugin, this.eventBus, this.versionAccessor, this.npcTracker);
    }
  }
}
