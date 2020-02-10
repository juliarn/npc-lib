package com.github.realpanamo.npc;


import com.github.realpanamo.npc.modifier.AnimationModifier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class NPCPool implements Listener {

    private JavaPlugin javaPlugin;

    private int spawnDistance = 50;

    private int actionDistance = 20;

    private Map<Integer, NPC> npcMap = new HashMap<>();


    /**
     * Creates a new NPC pool which handles events, spawning and destruction of the NPCs for players
     *
     * @param javaPlugin the instance of the plugin which creates this pool
     */
    public NPCPool(@NotNull JavaPlugin javaPlugin) {
        this.javaPlugin = javaPlugin;

        Bukkit.getPluginManager().registerEvents(this, javaPlugin);

        this.npcTick();
    }

    /**
     * Creates a new NPC pool which handles events, spawning and destruction of the NPCs for players
     *
     * @param javaPlugin     the instance of the plugin which creates this pool
     * @param spawnDistance  the distance in which NPCs are spawned for players
     * @param actionDistance the distance in which NPC actions are displayed for players
     */
    public NPCPool(@NotNull JavaPlugin javaPlugin, int spawnDistance, int actionDistance) {
        this(javaPlugin);

        if (spawnDistance < 0 || actionDistance < 0) {
            throw new IllegalArgumentException("Distance has to be > 0!");
        }
        if (actionDistance > spawnDistance) {
            throw new IllegalArgumentException("Action distance cannot be higher than spawn distance!");
        }

        this.spawnDistance = spawnDistance;
        this.actionDistance = actionDistance;
    }

    private void npcTick() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this.javaPlugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                for (NPC npc : this.npcMap.values()) {
                    double distance = npc.getLocation().distance(player.getLocation());

                    if (distance >= this.spawnDistance && npc.isShownFor(player)) {
                        npc.hide(player);
                    } else if (distance <= this.spawnDistance && !npc.isShownFor(player)) {
                        npc.show(player, this.javaPlugin);
                    }

                    if (npc.isLookAtPlayer() && distance <= this.actionDistance) {
                        npc.rotation().lookAt(player.getLocation()).send(player);
                    }
                }
            }
        }, 20, 3);
    }

    protected void takeCareOf(@NotNull NPC npc) {
        this.npcMap.put(npc.getEntityId(), npc);
    }

    @Nullable
    public NPC getNPC(int entityId) {
        return this.npcMap.get(entityId);
    }

    @EventHandler
    public void handleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        this.npcMap.values().stream()
                .filter(npc -> npc.isImitatePlayer() && npc.getLocation().distance(player.getLocation()) <= this.actionDistance)
                .forEach(npc -> npc.metadata().sneaking(event.isSneaking()).send(player));
    }

    @EventHandler
    public void handleClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            this.npcMap.values().stream()
                    .filter(npc -> npc.isImitatePlayer() && npc.getLocation().distance(player.getLocation()) <= this.actionDistance)
                    .forEach(npc -> npc.animation().play(AnimationModifier.EntityAnimation.SWING_MAIN_ARM).send(player));
        }
    }

    public Collection<NPC> getNPCs() {
        return this.npcMap.values();
    }

}
