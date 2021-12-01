package com.github.juliarn.npc;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedEnumEntityUseAction;
import com.github.juliarn.npc.event.PlayerNPCHideEvent;
import com.github.juliarn.npc.event.PlayerNPCInteractEvent;
import com.github.juliarn.npc.modifier.AnimationModifier;
import com.github.juliarn.npc.modifier.LabyModModifier;
import com.github.juliarn.npc.modifier.MetadataModifier;
import com.github.juliarn.npc.modifier.NPCModifier;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Represents the main management point for {@link NPC}s.
 */
public class NPCPool implements Listener {

  private static final Random RANDOM = new Random();

  private final Plugin plugin;

  private final double spawnDistance;
  private final double actionDistance;
  private final long tabListRemoveTicks;

  private final Map<Integer, NPC> npcMap = new ConcurrentHashMap<>();

  /**
   * Creates a new NPC pool which handles events, spawning and destruction of the NPCs for players.
   * Please use {@link #createDefault(Plugin)} instead, this constructor will be private in a
   * further release.
   *
   * @param plugin the instance of the plugin which creates this pool
   * @deprecated Use {@link #createDefault(Plugin)} instead
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval
  public NPCPool(@NotNull Plugin plugin) {
    this(plugin, 50, 20, 30);
  }

  /**
   * Creates a new NPC pool which handles events, spawning and destruction of the NPCs for players.
   * Please use {@link #builder(Plugin)} instead, this constructor will be private in a further
   * release.
   *
   * @param plugin             the instance of the plugin which creates this pool
   * @param spawnDistance      the distance in which NPCs are spawned for players
   * @param actionDistance     the distance in which NPC actions are displayed for players
   * @param tabListRemoveTicks the time in ticks after which the NPC will be removed from the
   *                           players tab
   */
  @Deprecated
  @ApiStatus.Internal
  public NPCPool(@NotNull Plugin plugin, int spawnDistance, int actionDistance,
      long tabListRemoveTicks) {
    Preconditions.checkArgument(spawnDistance > 0 && actionDistance > 0, "Distance has to be > 0!");
    Preconditions.checkArgument(actionDistance <= spawnDistance,
        "Action distance cannot be higher than spawn distance!");

    this.plugin = plugin;

    // limiting the spawn distance to the Bukkit view distance to avoid NPCs not being shown
    this.spawnDistance = Math.min(
        spawnDistance * spawnDistance,
        Math.pow(Bukkit.getViewDistance() << 4, 2));
    this.actionDistance = actionDistance * actionDistance;
    this.tabListRemoveTicks = tabListRemoveTicks;

    Bukkit.getPluginManager().registerEvents(this, plugin);

    // communication with LabyMod
    String labyModPluginChannel = LabyModModifier.LABYMOD_PLUGIN_CHANNEL.getFullKey();
    // we might send messages on this channel
    Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, labyModPluginChannel);
    if (!Bukkit.getMessenger().isIncomingChannelRegistered(plugin, labyModPluginChannel)) {
      Bukkit.getMessenger().registerIncomingPluginChannel(plugin, labyModPluginChannel,
          (channel, player, message) -> {
            // we don't actually handle LabyMod messages, we just register
            // incoming messages to make sure minecraft:register is sent to the proxy,
            // so that it will forward our messages on the LabyMod channel to the player
          });
    }

    this.addInteractListener();
    this.npcTick();
  }

  /**
   * Creates a new npc pool with the default values of a npc pool. The default values of a builder
   * are {@code spawnDistance} to {@code 50}, {@code actionDistance} to {@code 20} and {@code
   * tabListRemoveTicks} to {@code 30}.
   *
   * @param plugin the instance of the plugin which creates this pool.
   * @return the created npc pool with the default values of a pool.
   * @since 2.5-SNAPSHOT
   */
  @NotNull
  public static NPCPool createDefault(@NotNull Plugin plugin) {
    return NPCPool.builder(plugin).build();
  }

  /**
   * Creates a new builder for a npc pool.
   *
   * @param plugin the instance of the plugin which creates the builder for the pool.
   * @return a new builder for creating a npc pool instance.
   * @since 2.5-SNAPSHOT
   */
  @NotNull
  public static Builder builder(@NotNull Plugin plugin) {
    return new Builder(plugin);
  }

  /**
   * Adds a packet listener for listening to all use entity packets sent by a client.
   */
  protected void addInteractListener() {
    ProtocolLibrary.getProtocolManager()
        .addPacketListener(new PacketAdapter(this.plugin, PacketType.Play.Client.USE_ENTITY) {
          @Override
          public void onPacketReceiving(PacketEvent event) {
            PacketContainer container = event.getPacket();
            int targetId = container.getIntegers().read(0);

            if (NPCPool.this.npcMap.containsKey(targetId)) {
              NPC npc = NPCPool.this.npcMap.get(targetId);

              EnumWrappers.Hand usedHand;
              EnumWrappers.EntityUseAction action;

              if (NPCModifier.MINECRAFT_VERSION >= 17) {
                WrappedEnumEntityUseAction useAction = container.getEnumEntityUseActions().read(0);
                // the hand is only available when not attacking
                action = useAction.getAction();
                usedHand = action == EnumWrappers.EntityUseAction.ATTACK
                    ? EnumWrappers.Hand.MAIN_HAND
                    : useAction.getHand();
              } else {
                // the hand is only available when not attacking
                action = container.getEntityUseActions().read(0);
                usedHand = action == EnumWrappers.EntityUseAction.ATTACK
                    ? EnumWrappers.Hand.MAIN_HAND
                    : container.getHands().optionRead(0).orElse(EnumWrappers.Hand.MAIN_HAND);
              }

              Bukkit.getScheduler().runTask(
                  NPCPool.this.plugin,
                  () -> Bukkit.getPluginManager().callEvent(
                      new PlayerNPCInteractEvent(
                          event.getPlayer(),
                          npc,
                          action,
                          usedHand))
              );
            }
          }
        });
  }

  /**
   * Starts the npc tick.
   */
  protected void npcTick() {
    Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, () -> {
      for (Player player : ImmutableList.copyOf(Bukkit.getOnlinePlayers())) {
        for (NPC npc : this.npcMap.values()) {
          Location npcLoc = npc.getLocation();
          Location playerLoc = player.getLocation();
          if (!npcLoc.getWorld().equals(playerLoc.getWorld())) {
            if (npc.isShownFor(player)) {
              npc.hide(player, this.plugin, PlayerNPCHideEvent.Reason.SPAWN_DISTANCE);
            }
            continue;
          } else if (!npcLoc.getWorld()
              .isChunkLoaded(npcLoc.getBlockX() >> 4, npcLoc.getBlockZ() >> 4)) {
            if (npc.isShownFor(player)) {
              npc.hide(player, this.plugin, PlayerNPCHideEvent.Reason.UNLOADED_CHUNK);
            }
            continue;
          }

          double distance = npcLoc.distanceSquared(playerLoc);
          boolean inRange = distance <= this.spawnDistance;

          if ((npc.isExcluded(player) || !inRange) && npc.isShownFor(player)) {
            npc.hide(player, this.plugin, PlayerNPCHideEvent.Reason.SPAWN_DISTANCE);
          } else if ((!npc.isExcluded(player) && inRange) && !npc.isShownFor(player)) {
            npc.show(player, this.plugin, this.tabListRemoveTicks);
          }

          if (npc.isShownFor(player) && npc.isLookAtPlayer() && distance <= this.actionDistance) {
            npc.rotation().queueLookAt(playerLoc).send(player);
          }
        }
      }
    }, 20, 2);
  }

  /**
   * @return A free entity id which can be used for NPCs
   */
  protected int getFreeEntityId() {
    int id;

    do {
      id = RANDOM.nextInt(Integer.MAX_VALUE);
    } while (this.npcMap.containsKey(id));

    return id;
  }

  /**
   * Adds the given {@code npc} to the list of handled NPCs of this pool.
   *
   * @param npc The npc to add.
   * @see NPC#builder()
   */
  protected void takeCareOf(@NotNull NPC npc) {
    this.npcMap.put(npc.getEntityId(), npc);
  }

  /**
   * Gets a specific npc by the given {@code entityId}.
   *
   * @param entityId the entity id of the npc to get.
   * @return The npc or {@code null} if there is no npc with the given entity id.
   * @deprecated Use {@link #getNpc(int)} instead.
   */
  @Nullable
  @Deprecated
  @ApiStatus.ScheduledForRemoval
  public NPC getNPC(int entityId) {
    return this.npcMap.get(entityId);
  }

  /**
   * Gets a specific npc by the given {@code entityId}.
   *
   * @param entityId the entity id of the npc to get.
   * @return The npc by the given {@code entityId}.
   * @since 2.5-SNAPSHOT
   */
  @NotNull
  public Optional<NPC> getNpc(int entityId) {
    return Optional.ofNullable(this.npcMap.get(entityId));
  }

  /**
   * Gets a specific npc by the given {@code uniqueId}.
   *
   * @param uniqueId the entity unique id of the npc to get.
   * @return The npc by the given {@code uniqueId}.
   * @since 2.5-SNAPSHOT
   */
  @NotNull
  public Optional<NPC> getNpc(@NotNull UUID uniqueId) {
    return this.npcMap.values().stream()
        .filter(npc -> npc.getProfile().getUniqueId().equals(uniqueId)).findFirst();
  }

  /**
   * Removes the given npc by it's entity id from the handled NPCs of this pool.
   *
   * @param entityId the entity id of the npc to get.
   */
  public void removeNPC(int entityId) {
    this.getNpc(entityId).ifPresent(npc -> {
      this.npcMap.remove(entityId);
      npc.getSeeingPlayers()
          .forEach(player -> npc.hide(player, this.plugin, PlayerNPCHideEvent.Reason.REMOVED));
    });
  }

  /**
   * Get an unmodifiable copy of all NPCs handled by this pool.
   *
   * @return a copy of the NPCs this pool manages.
   */
  @NotNull
  @Unmodifiable
  public Collection<NPC> getNPCs() {
    return Collections.unmodifiableCollection(this.npcMap.values());
  }

  @EventHandler
  public void handleRespawn(PlayerRespawnEvent event) {
    Player player = event.getPlayer();

    this.npcMap.values().stream()
        .filter(npc -> npc.isShownFor(player))
        .forEach(npc -> npc.hide(player, this.plugin, PlayerNPCHideEvent.Reason.RESPAWNED));
  }

  @EventHandler
  public void handleQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();

    this.npcMap.values().stream()
        .filter(npc -> npc.isShownFor(player) || npc.isExcluded(player))
        .forEach(npc -> {
          npc.removeSeeingPlayer(player);
          npc.removeExcludedPlayer(player);
        });
  }

  @EventHandler
  public void handleSneak(PlayerToggleSneakEvent event) {
    Player player = event.getPlayer();

    for (NPC npc : this.npcMap.values()) {
      if (npc.isImitatePlayer()
          && npc.getLocation().getWorld().equals(player.getWorld())
          && npc.isShownFor(player)
          && npc.getLocation().distanceSquared(player.getLocation()) <= this.actionDistance) {
        npc.metadata()
            .queue(MetadataModifier.EntityMetadata.SNEAKING, event.isSneaking()).send(player);
      }
    }
  }

  @EventHandler
  public void handleClick(PlayerInteractEvent event) {
    Player player = event.getPlayer();

    if (event.getAction() == Action.LEFT_CLICK_AIR
        || event.getAction() == Action.LEFT_CLICK_BLOCK) {
      for (NPC npc : this.npcMap.values()) {
        if (npc.isImitatePlayer()
            && npc.getLocation().getWorld().equals(player.getWorld())
            && npc.isShownFor(player)
            && npc.getLocation().distanceSquared(player.getLocation()) <= this.actionDistance) {
          npc.animation().queue(AnimationModifier.EntityAnimation.SWING_MAIN_ARM)
              .send(player);
        }
      }
    }
  }

  /**
   * A builder for a npc pool.
   *
   * @since 2.5-SNAPSHOT
   */
  public static class Builder {

    /**
     * The instance of the plugin which creates this pool
     */
    private final Plugin plugin;

    /**
     * The distance in which NPCs are spawned for players
     */
    private int spawnDistance = 50;
    /**
     * The distance in which NPC actions are displayed for players
     */
    private int actionDistance = 20;
    /**
     * The time in ticks after which the NPC will be removed from the players tab
     */
    private long tabListRemoveTicks = 30;

    /**
     * Creates a new builder for a npc pool.
     *
     * @param plugin The instance of the plugin which creates the builder.
     */
    private Builder(@NotNull Plugin plugin) {
      this.plugin = Preconditions.checkNotNull(plugin, "plugin");
    }

    /**
     * Sets the spawn distance in which NPCs are spawned for players. Must be higher than {@code
     * 0}.
     *
     * @param spawnDistance the spawn distance in which NPCs are spawned for players.
     * @return The same instance of this class, for chaining.
     */
    @NotNull
    public Builder spawnDistance(int spawnDistance) {
      Preconditions.checkArgument(spawnDistance > 0, "Spawn distance must be more than 0");
      this.spawnDistance = spawnDistance;
      return this;
    }

    /**
     * Sets the distance in which NPC actions are displayed for players. Must be higher than {@code
     * 0}.
     *
     * @param actionDistance the distance in which NPC actions are displayed for players.
     * @return The same instance of this class, for chaining.
     */
    @NotNull
    public Builder actionDistance(int actionDistance) {
      Preconditions.checkArgument(actionDistance > 0, "Action distance must be more than 0");
      this.actionDistance = actionDistance;
      return this;
    }

    /**
     * Sets the distance in which NPC actions are displayed for players. A negative value indicates
     * that the npc is never removed from the player list by default.
     *
     * @param tabListRemoveTicks the distance in which NPC actions are displayed for players.
     * @return The same instance of this class, for chaining.
     */
    @NotNull
    public Builder tabListRemoveTicks(long tabListRemoveTicks) {
      this.tabListRemoveTicks = tabListRemoveTicks;
      return this;
    }

    /**
     * Creates a new npc tool by the values passed to the builder.
     *
     * @return The created npc pool.
     */
    @NotNull
    public NPCPool build() {
      return new NPCPool(this.plugin, this.spawnDistance, this.actionDistance,
          this.tabListRemoveTicks);
    }
  }
}
