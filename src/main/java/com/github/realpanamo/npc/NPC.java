package com.github.realpanamo.npc;


import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.github.realpanamo.npc.modifier.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class NPC {

    private static final Random RANDOM = new Random();

    private final Collection<Player> seeingPlayers = new HashSet<>();

    private final int entityId = RANDOM.nextInt(Short.MAX_VALUE);

    private final WrappedGameProfile gameProfile;

    private final Location location;
    private final VisibilityModifier visibilityModifier = new VisibilityModifier(this);
    private final AnimationModifier animationModifier = new AnimationModifier(this);
    private final EquipmentModifier equipmentModifier = new EquipmentModifier(this);
    private final RotationModifier rotationModifier = new RotationModifier(this);
    private final MetadataModifier metadataModifier = new MetadataModifier(this);
    private boolean lookAtPlayer;
    private boolean imitatePlayer;
    private SpawnCustomizer spawnCustomizer;

    private NPC(Set<ProfileProperty> profileProperties, String name, Location location, boolean lookAtPlayer, boolean imitatePlayer, SpawnCustomizer spawnCustomizer) {
        this.gameProfile = new WrappedGameProfile(new UUID(RANDOM.nextLong(), 0), name);

        this.appendProperties(profileProperties);

        this.location = location;
        this.lookAtPlayer = lookAtPlayer;
        this.imitatePlayer = imitatePlayer;
        this.spawnCustomizer = spawnCustomizer;
    }

    private NPC(UUID textureUUID, String name, Location location, boolean lookAtPlayer, boolean imitatePlayer, SpawnCustomizer spawnCustomizer) {
        this.gameProfile = new WrappedGameProfile(new UUID(RANDOM.nextLong(), 0), name);

        PlayerProfile profile = Bukkit.createProfile(textureUUID);
        profile.complete();
        this.appendProperties(profile.getProperties());

        this.location = location;
        this.lookAtPlayer = lookAtPlayer;
        this.imitatePlayer = imitatePlayer;
        this.spawnCustomizer = spawnCustomizer;
    }

    private void appendProperties(Set<ProfileProperty> profileProperties) {
        profileProperties.stream()
                .map(property -> new WrappedSignedProperty(property.getName(), property.getValue(), property.getSignature()))
                .forEach(property -> this.gameProfile.getProperties().put(property.getName(), property));
    }

    protected void show(@NotNull Player player, @NotNull JavaPlugin javaPlugin) {
        this.visibilityModifier.addToPlayerList().send(player);
        this.visibilityModifier.spawn().send(player);

        this.seeingPlayers.add(player);

        this.spawnCustomizer.handleSpawn(this, player);

        Bukkit.getScheduler().runTaskLater(javaPlugin, () -> this.visibilityModifier.removeFromPlayerList().send(player), 5L);
    }

    protected void hide(@NotNull Player player) {
        this.visibilityModifier
                .removeFromPlayerList()
                .destroy()
                .send(player);

        this.seeingPlayers.remove(player);
    }

    public boolean isShownFor(Player player) {
        return this.seeingPlayers.contains(player);
    }

    /**
     * Serves methods to play animations on an NPC
     *
     * @return the animation modifier modifying this NPC
     */
    public AnimationModifier animation() {
        return this.animationModifier;
    }

    /**
     * Serves methods related to entity rotation
     *
     * @return the rotation modifier modifying this NPC
     */
    public RotationModifier rotation() {
        return this.rotationModifier;
    }

    /**
     * Serves methods to change an NPCs equipment
     *
     * @return the equipment modifier modifying this NPC
     */
    public EquipmentModifier equipment() {
        return this.equipmentModifier;
    }

    /**
     * Serves methods to change an NPCs metadata, including sneaking etc.
     *
     * @return the metadata modifier modifying this NPC
     */
    public MetadataModifier metadata() {
        return this.metadataModifier;
    }

    public WrappedGameProfile getGameProfile() {
        return gameProfile;
    }

    public int getEntityId() {
        return entityId;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isLookAtPlayer() {
        return lookAtPlayer;
    }

    public void setLookAtPlayer(boolean lookAtPlayer) {
        this.lookAtPlayer = lookAtPlayer;
    }

    public boolean isImitatePlayer() {
        return imitatePlayer;
    }

    public void setImitatePlayer(boolean imitatePlayer) {
        this.imitatePlayer = imitatePlayer;
    }

    public static class Builder {

        private Set<ProfileProperty> profileProperties;

        private UUID textureUUID;

        private String name;

        private Location location = new Location(Bukkit.getWorlds().get(0), 0D, 0D, 0D);

        private boolean lookAtPlayer = true;

        private boolean imitatePlayer = true;

        private SpawnCustomizer spawnCustomizer = (npc, player) -> {
        };

        /**
         * Creates a new instance of the NPC builder
         *
         * @param textureUUID textures of this profile will be fetched and shown on the NPC
         * @param name        the name the NPC should have
         */
        public Builder(@NotNull UUID textureUUID, @NotNull String name) {
            this.textureUUID = textureUUID;
            this.name = name;
        }

        /**
         * Creates a new instance of the NPC builder
         *
         * @param profileProperties a set of Paper profile properties, including textures
         * @param name              the name the NPC should have
         */
        public Builder(@NotNull Set<ProfileProperty> profileProperties, @NotNull String name) {
            this.profileProperties = profileProperties;
            this.name = name;
        }

        /**
         * Sets the location of the npc, cannot be changed afterwards
         *
         * @param location the location
         * @return this builder instance
         */
        public Builder location(@NotNull Location location) {
            this.location = location;
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
         * Modifications the NPC should have the whole time should be done inside of this executor.
         *
         * @param spawnCustomizer the spawn customizer which will be called on every spawn
         * @return this builder instance
         */
        public Builder spawnCustomizer(@NotNull SpawnCustomizer spawnCustomizer) {
            this.spawnCustomizer = spawnCustomizer;
            return this;
        }

        /**
         * Passes the NPC to a pool which handles events and spawning and destruction of this NPC for players
         *
         * @param pool the pool the NPC will be passed to
         * @return this builder instance
         */
        @NotNull
        public NPC build(@NotNull NPCPool pool) {
            NPC npc = this.profileProperties != null
                    ? new NPC(this.profileProperties, this.name, this.location, this.lookAtPlayer, this.imitatePlayer, this.spawnCustomizer)
                    : new NPC(this.textureUUID, this.name, this.location, this.lookAtPlayer, this.imitatePlayer, this.spawnCustomizer);

            pool.takeCareOf(npc);

            return npc;
        }

    }

}
