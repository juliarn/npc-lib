package com.github.juliarn.npc;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.github.juliarn.npc.event.PlayerNPCInteractEvent;
import com.github.juliarn.npc.modifier.AnimationModifier;
import com.github.juliarn.npc.modifier.MetadataModifier;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NPCPool implements Listener {

    private final JavaPlugin javaPlugin;

    private final double spawnDistance;

    private final double actionDistance;

    private final Map<Integer, NPC> npcMap = new ConcurrentHashMap<>();

    /**
     * Creates a new NPC pool which handles events, spawning and destruction of the NPCs for players
     *
     * @param javaPlugin the instance of the plugin which creates this pool
     */
    public NPCPool(@NotNull JavaPlugin javaPlugin) {
        this(javaPlugin, 50, 20);
    }

    /**
     * Creates a new NPC pool which handles events, spawning and destruction of the NPCs for players
     *
     * @param javaPlugin     the instance of the plugin which creates this pool
     * @param spawnDistance  the distance in which NPCs are spawned for players
     * @param actionDistance the distance in which NPC actions are displayed for players
     */
    public NPCPool(@NotNull JavaPlugin javaPlugin, int spawnDistance, int actionDistance) {
        Preconditions.checkArgument(spawnDistance > 0 && actionDistance > 0, "Distance has to be > 0!");
        Preconditions.checkArgument(actionDistance <= spawnDistance, "Action distance cannot be higher than spawn distance!");

        this.javaPlugin = javaPlugin;

        this.spawnDistance = spawnDistance * spawnDistance;
        this.actionDistance = actionDistance * actionDistance;

        Bukkit.getPluginManager().registerEvents(this, javaPlugin);

        this.addInteractListener();
        this.npcTick();
    }

    private void addInteractListener() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this.javaPlugin, PacketType.Play.Client.USE_ENTITY) {

            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketContainer packetContainer = event.getPacket();
                int targetId = packetContainer.getIntegers().read(0);

                if (npcMap.containsKey(targetId)) {
                    NPC npc = npcMap.get(targetId);
                    EnumWrappers.EntityUseAction action = packetContainer.getEntityUseActions().read(0);

                    Bukkit.getScheduler().runTask(javaPlugin, () ->
                            Bukkit.getPluginManager().callEvent(
                                    new PlayerNPCInteractEvent(event.getPlayer(), npc, action)
                            ));
                }
            }

        });
    }

    private void npcTick() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this.javaPlugin, () -> {
            for (Player player : ImmutableList.copyOf(Bukkit.getOnlinePlayers())) {

                for (NPC npc : this.npcMap.values()) {
                    double distance = npc.getLocation().distanceSquared(player.getLocation());

                    if (distance >= this.spawnDistance && npc.isShownFor(player)) {
                        npc.hide(player);
                    } else if (distance <= this.spawnDistance && !npc.isShownFor(player)) {
                        npc.show(player, this.javaPlugin);
                    }

                    if (npc.isLookAtPlayer() && distance <= this.actionDistance) {
                        npc.rotation().queueLookAt(player.getLocation()).send(player);
                    }
                }
            }
        }, 20, 2);
    }

    protected void takeCareOf(@NotNull NPC npc) {
        this.npcMap.put(npc.getEntityId(), npc);
    }

    @Nullable
    public NPC getNPC(int entityId) {
        return this.npcMap.get(entityId);
    }

    public void removeNPC(int entityId) {
        NPC npc = this.getNPC(entityId);

        if (npc != null) {
            this.npcMap.remove(entityId);
            npc.getSeeingPlayers().forEach(npc::hide);
        }
    }

    @EventHandler
    public void handleQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        this.npcMap.values().stream()
                .filter(npc -> npc.isShownFor(player))
                .forEach(npc -> npc.removeSeeingPlayer(player));
    }

    @EventHandler
    public void handleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        this.npcMap.values().stream()
                .filter(npc -> npc.isImitatePlayer() && npc.isShownFor(player) && npc.getLocation().distanceSquared(player.getLocation()) <= this.actionDistance)
                .forEach(npc -> npc.metadata().queue(MetadataModifier.EntityMetadata.SNEAKING, event.isSneaking()).send(player));
    }

    @EventHandler
    public void handleClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            this.npcMap.values().stream()
                    .filter(npc -> npc.isImitatePlayer() && npc.isShownFor(player) && npc.getLocation().distanceSquared(player.getLocation()) <= this.actionDistance)
                    .forEach(npc -> npc.animation().queue(AnimationModifier.EntityAnimation.SWING_MAIN_ARM).send(player));
        }
    }

    public Collection<NPC> getNPCs() {
        return this.npcMap.values();
    }

}
