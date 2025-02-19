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

import static com.comphenix.protocol.ProtocolLibrary.getPlugin;

import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.NpcActionController;
import com.github.juliarn.npclib.api.NpcTracker;
import com.github.juliarn.npclib.api.PlatformVersionAccessor;
import com.github.juliarn.npclib.api.Position;
import com.github.juliarn.npclib.api.event.ShowNpcEvent;
import com.github.juliarn.npclib.api.event.manager.NpcEventManager;
import com.github.juliarn.npclib.api.flag.NpcFlag;
import com.github.juliarn.npclib.api.protocol.meta.EntityMetadataFactory;
import com.github.juliarn.npclib.common.CommonNpcActionController;
import com.github.juliarn.npclib.common.flag.CommonNpcFlaggedBuilder;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class BukkitActionController extends CommonNpcActionController implements Listener {

  private final Plugin plugin;
  private final NpcTracker<World, Player, ItemStack, Plugin> npcTracker;
  private final Map<World, Set<Long>> loadedChunks = new ConcurrentHashMap<>();
  private final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();
  private final NpcEventManager eventManager;
  private final PlatformVersionAccessor versionAccessor;

  private static final int COOLDOWN_TICKS = 10;
  private static final long COOLDOWN_MILLIS = COOLDOWN_TICKS * 50L;
  private static final double MOVEMENT_THRESHOLD = 0.5;
  private final int spawnDistance;

  public BukkitActionController(
    @NotNull Map<NpcFlag<?>, Optional<?>> flags,
    @NotNull Plugin plugin,
    @NotNull NpcEventManager eventManager,
    @NotNull PlatformVersionAccessor versionAccessor,
    @NotNull NpcTracker<World, Player, ItemStack, Plugin> npcTracker) {
    super(flags);
    this.plugin = plugin;
    this.npcTracker = npcTracker;
    this.eventManager = eventManager;
    this.versionAccessor = versionAccessor;
    this.spawnDistance = 50;
    scheduleCleanup();
  }

  private void scheduleCleanup() {
    Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      long currentTime = System.currentTimeMillis();
      playerCooldowns.entrySet().removeIf(entry ->
        currentTime - entry.getValue() > COOLDOWN_MILLIS * 2);
    }, 6000L, 6000L);
  }

  private long chunkKey(int x, int z) {
    return ((long) x << 32) | (z & 0xFFFFFFFFL);
  }

  @EventHandler
  public void handleMove(@NotNull PlayerMoveEvent event) {
    Location from = event.getFrom();
    Location to = event.getTo();
    if (to == null) return;

    double dx = to.getX() - from.getX();
    double dy = to.getY() - from.getY();
    double dz = to.getZ() - from.getZ();

    if (Math.abs(dx) <= MOVEMENT_THRESHOLD &&
      Math.abs(dy) <= MOVEMENT_THRESHOLD &&
      Math.abs(dz) <= MOVEMENT_THRESHOLD) return;

    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();
    long currentTime = System.currentTimeMillis();

    Long lastProcessed = playerCooldowns.get(playerId);
    if (lastProcessed != null && currentTime - lastProcessed < COOLDOWN_MILLIS) return;
    playerCooldowns.put(playerId, currentTime);

    World playerWorld = player.getWorld();
    Location playerLoc = player.getLocation();

    for (Npc<World, Player, ItemStack, Plugin> npc : npcTracker.trackedNpcs()) {
      World npcWorld = npc.world();
      if (!npcWorld.equals(playerWorld)) {
        npc.stopTrackingPlayer(player);
        continue;
      }

      Position pos = npc.position();
      Set<Long> worldChunks = loadedChunks.get(npcWorld);
      if (worldChunks == null || !worldChunks.contains(chunkKey(pos.chunkX(), pos.chunkZ()))) {
        npc.stopTrackingPlayer(player);
        continue;
      }

      double distX = playerLoc.getX() - pos.x();
      double distY = playerLoc.getY() - pos.y();
      double distZ = playerLoc.getZ() - pos.z();
      double distanceSquared = distX * distX + distY * distY + distZ * distZ;

      if (npc.trackedPlayers().contains(player)) {
        if (distanceSquared > spawnDistance * spawnDistance) {
          npc.stopTrackingPlayer(player);
        }
      } else if (distanceSquared <= spawnDistance * spawnDistance) {
        ShowNpcEvent.Pre showEvent = new ShowNpcEvent.Pre() {
          private boolean isCancelled = false;

          @Override
          public boolean cancelled() {
            return isCancelled;
          }

          @Override
          public void cancelled(boolean cancelled) {
            this.isCancelled = cancelled;
          }

          @Override
          @SuppressWarnings("unchecked")
          public <P> P player() {
            return (P) player;
          }

          @Override
          public Npc<World, Player, ItemStack, Plugin> npc() {
            return npc;
          }
        };

        eventManager.post(showEvent);

        if (!showEvent.cancelled()) {
          npc.trackPlayer(player);
        }
      }
    }
  }

  @EventHandler
  public void onChunkLoad(ChunkLoadEvent event) {
    World world = event.getWorld();
    Chunk chunk = event.getChunk();
    loadedChunks.computeIfAbsent(world, k -> ConcurrentHashMap.newKeySet())
      .add(chunkKey(chunk.getX(), chunk.getZ()));
  }

  @EventHandler
  public void onChunkUnload(ChunkUnloadEvent event) {
    World world = event.getWorld();
    Set<Long> chunks = loadedChunks.get(world);
    if (chunks != null) {
      chunks.remove(chunkKey(event.getChunk().getX(), event.getChunk().getZ()));
      if (chunks.isEmpty()) {
        loadedChunks.remove(world);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void handleSneak(@NotNull PlayerToggleSneakEvent event) {
    Player player = event.getPlayer();
    boolean isSneaking = event.isSneaking();

    npcTracker.trackedNpcs().stream()
      .filter(npc -> npc.trackedPlayers().contains(player))
      .forEach(npc -> {
        npc.changeMetadata(EntityMetadataFactory.sneakingMetaFactory(), isSneaking);
      });
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void handleQuit(@NotNull PlayerQuitEvent event) {
    Player player = event.getPlayer();
    playerCooldowns.remove(player.getUniqueId());
    npcTracker.trackedNpcs().forEach(npc -> npc.stopTrackingPlayer(player));
  }

  // Static factory method for builder
  public static @NotNull NpcActionController.Builder actionControllerBuilder(
    @NotNull Plugin plugin,
    @NotNull NpcEventManager eventManager,
    @NotNull PlatformVersionAccessor versionAccessor,
    @NotNull NpcTracker<World, Player, ItemStack, Plugin> npcTracker) {
    return new BukkitActionControllerBuilder(plugin, eventManager, versionAccessor, npcTracker);
  }

  private static final class BukkitActionControllerBuilder
    extends CommonNpcFlaggedBuilder<NpcActionController.Builder>
    implements NpcActionController.Builder {

    private final Plugin plugin;
    private final NpcEventManager eventManager;
    private final PlatformVersionAccessor versionAccessor;
    private final NpcTracker<World, Player, ItemStack, Plugin> npcTracker;

    public BukkitActionControllerBuilder(
      @NotNull Plugin plugin,
      @NotNull NpcEventManager eventManager,
      @NotNull PlatformVersionAccessor versionAccessor,
      @NotNull NpcTracker<World, Player, ItemStack, Plugin> npcTracker) {
      Objects.requireNonNull(plugin, "plugin");
      Objects.requireNonNull(eventManager, "eventManager");
      Objects.requireNonNull(versionAccessor, "versionAccessor");
      Objects.requireNonNull(npcTracker, "npcTracker");

      this.plugin = plugin;
      this.eventManager = eventManager;
      this.versionAccessor = versionAccessor;
      this.npcTracker = npcTracker;
    }

    @Override
    public @NotNull NpcActionController build() {
      return new BukkitActionController(
        (Map<NpcFlag<?>, Optional<?>>) this.flags,
        this.plugin,
        this.eventManager,
        this.versionAccessor,
        this.npcTracker);
    }
  }
}
