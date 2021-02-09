package com.github.juliarn.npc;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.github.juliarn.npc.modifier.AnimationModifier;
import com.github.juliarn.npc.modifier.EquipmentModifier;
import com.github.juliarn.npc.modifier.MetadataModifier;
import com.github.juliarn.npc.modifier.RotationModifier;
import com.github.juliarn.npc.modifier.VisibilityModifier;
import com.github.juliarn.npc.profile.Profile;
import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArraySet;

public class NPC {

  private static final Random RANDOM = new Random();

  private final Collection<Player> seeingPlayers = new CopyOnWriteArraySet<>();
  private final Collection<Player> excludedPlayers = new CopyOnWriteArraySet<>();

  private final int entityId = RANDOM.nextInt(Short.MAX_VALUE);

  private final Location location;
  private final Profile profile;
  private final WrappedGameProfile gameProfile;
  private final SpawnCustomizer spawnCustomizer;

  private boolean lookAtPlayer;
  private boolean imitatePlayer;

  private NPC(Profile profile, Location location, boolean lookAtPlayer, boolean imitatePlayer, SpawnCustomizer spawnCustomizer) {
    this.profile = profile;
    this.gameProfile = this.convertProfile(profile);

    this.location = location;
    this.lookAtPlayer = lookAtPlayer;
    this.imitatePlayer = imitatePlayer;
    this.spawnCustomizer = spawnCustomizer;
  }

  @NotNull
  public static NPC.Builder builder() {
    return new NPC.Builder();
  }

  protected void show(@NotNull Player player, @NotNull JavaPlugin javaPlugin, long tabListRemoveTicks) {
    this.seeingPlayers.add(player);

    VisibilityModifier visibilityModifier = new VisibilityModifier(this);
    visibilityModifier.queuePlayerListChange(EnumWrappers.PlayerInfoAction.ADD_PLAYER).send(player);

    Bukkit.getScheduler().runTaskLater(javaPlugin, () -> {
      visibilityModifier.queueSpawn().send(player);
      this.spawnCustomizer.handleSpawn(this, player);

      // keeping the NPC longer in the player list, otherwise the skin might not be shown sometimes.
      Bukkit.getScheduler().runTaskLater(javaPlugin, () -> visibilityModifier.queuePlayerListChange(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER).send(player), tabListRemoveTicks);
    }, 10L);
  }

  protected void hide(@NotNull Player player) {
    new VisibilityModifier(this)
      .queuePlayerListChange(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER)
      .queueDestroy()
      .send(player);

    this.removeSeeingPlayer(player);
  }

  @NotNull
  protected WrappedGameProfile convertProfile(@NotNull Profile profile) {
    WrappedGameProfile gameProfile = new WrappedGameProfile(profile.getUniqueId(), profile.getName());
    profile.getProperties().forEach(property -> gameProfile.getProperties().put(
      property.getName(),
      new WrappedSignedProperty(property.getName(), property.getValue(), property.getSignature())
    ));
    return gameProfile;
  }

  protected void removeSeeingPlayer(Player player) {
    this.seeingPlayers.remove(player);
  }

  /**
   * @return a copy of all players seeing this NPC
   */
  public Collection<Player> getSeeingPlayers() {
    return new HashSet<>(this.seeingPlayers);
  }

  public boolean isShownFor(Player player) {
    return this.seeingPlayers.contains(player);
  }

  /**
   * Adds a player which should be explicitly excluded from seeing this NPC
   *
   * @param player the player to be excluded
   */
  public void addExcludedPlayer(Player player) {
    this.excludedPlayers.add(player);
  }

  /**
   * Removes a player from being explicitly excluded from seeing this NPC
   *
   * @param player the player to be included again
   */
  public void removeExcludedPlayer(Player player) {
    this.excludedPlayers.remove(player);
  }

  /**
   * @return a modifiable collection of all players which are explicitly excluded from seeing this NPC
   */
  public Collection<Player> getExcludedPlayers() {
    return this.excludedPlayers;
  }

  public boolean isExcluded(Player player) {
    return this.excludedPlayers.contains(player);
  }

  /**
   * Creates a new animation modifier which serves methods to play animations on an NPC
   *
   * @return a animation modifier modifying this NPC
   */
  public AnimationModifier animation() {
    return new AnimationModifier(this);
  }

  /**
   * Creates a new rotation modifier which serves methods related to entity rotation
   *
   * @return a rotation modifier modifying this NPC
   */
  public RotationModifier rotation() {
    return new RotationModifier(this);
  }

  /**
   * Creates a new equipemt modifier which serves methods to change an NPCs equipment
   *
   * @return an equipment modifier modifying this NPC
   */
  public EquipmentModifier equipment() {
    return new EquipmentModifier(this);
  }

  /**
   * Creates a new metadata modifier which serves methods to change an NPCs metadata, including sneaking etc.
   *
   * @return a metadata modifier modifying this NPC
   */
  public MetadataModifier metadata() {
    return new MetadataModifier(this);
  }

  /**
   * Get the protocol lib profile wrapper for this npc. To use this method {@code ProtocolLib} is needed
   * as a dependency of your project. If you don't want to do that, use {@link #getProfile()} instead.
   *
   * @return the protocol lib profile wrapper for this npc
   */
  @NotNull
  public WrappedGameProfile getGameProfile() {
    return this.gameProfile;
  }

  @NotNull
  public Profile getProfile() {
    return this.profile;
  }

  public int getEntityId() {
    return this.entityId;
  }

  @NotNull
  public Location getLocation() {
    return this.location;
  }

  public boolean isLookAtPlayer() {
    return this.lookAtPlayer;
  }

  public void setLookAtPlayer(boolean lookAtPlayer) {
    this.lookAtPlayer = lookAtPlayer;
  }

  public boolean isImitatePlayer() {
    return this.imitatePlayer;
  }

  public void setImitatePlayer(boolean imitatePlayer) {
    this.imitatePlayer = imitatePlayer;
  }

  public static class Builder {

    private Profile profile;

    private Location location = new Location(Bukkit.getWorlds().get(0), 0D, 0D, 0D);

    private boolean lookAtPlayer = true;

    private boolean imitatePlayer = true;

    private SpawnCustomizer spawnCustomizer = (npc, player) -> {
    };

    private Builder() {
    }

    /**
     * Creates a new instance of the NPC builder
     *
     * @param profile a player profile defining UUID, name and textures of the NPC
     * @deprecated Use {@link NPC#builder()} instead
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval
    public Builder(@NotNull Profile profile) {
      this.profile = profile;
    }

    /**
     * Sets the profile of the npc, cannot be changed afterwards
     *
     * @param profile the profile
     * @return this builder instance
     */
    public Builder profile(@NotNull Profile profile) {
      this.profile = Preconditions.checkNotNull(profile, "profile");
      return this;
    }

    /**
     * Sets the location of the npc, cannot be changed afterwards
     *
     * @param location the location
     * @return this builder instance
     */
    public Builder location(@NotNull Location location) {
      this.location = Preconditions.checkNotNull(location, "location");
      return this;
    }

    /**
     * Enables/disables looking at the player, default is true
     *
     * @param lookAtPlayer if the NPC should look at the player
     * @return this builder instance
     */
    public Builder lookAtPlayer(boolean lookAtPlayer) {
      this.lookAtPlayer = lookAtPlayer;
      return this;
    }

    /**
     * Enables/disables imitation of the player, such as sneaking and hitting the player, default is true
     *
     * @param imitatePlayer if the NPC should imitate players
     * @return this builder instance
     */
    public Builder imitatePlayer(boolean imitatePlayer) {
      this.imitatePlayer = imitatePlayer;
      return this;
    }

    /**
     * Sets an executor which will be called every time the NPC is spawned for a certain player.
     * Permanent NPC modifications should be done in this method, otherwise they will be lost at the next respawn of the NPC.
     *
     * @param spawnCustomizer the spawn customizer which will be called on every spawn
     * @return this builder instance
     */
    public Builder spawnCustomizer(@NotNull SpawnCustomizer spawnCustomizer) {
      this.spawnCustomizer = Preconditions.checkNotNull(spawnCustomizer, "spawnCustomizer");
      return this;
    }

    /**
     * Passes the NPC to a pool which handles events, spawning and destruction of this NPC for players
     *
     * @param pool the pool the NPC will be passed to
     * @return this builder instance
     */
    @NotNull
    public NPC build(@NotNull NPCPool pool) {
      Preconditions.checkNotNull(this.profile, "A profile must be given");
      Preconditions.checkArgument(this.profile.isComplete(), "The provided profile has to be complete!");

      NPC npc = new NPC(
        this.profile,
        this.location,
        this.lookAtPlayer,
        this.imitatePlayer,
        this.spawnCustomizer
      );
      pool.takeCareOf(npc);

      return npc;
    }
  }
}
