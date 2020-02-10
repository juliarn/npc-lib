package com.github.realpanamo.npc;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface SpawnCustomizer {

    /**
     * Being called when a NPC was spawned for a certain player
     * Put your NPC modifications into this method or they will be lost at the next spawn.
     * Modifications should only be sent to this one player.
     *
     * @param npc    the NPC the has been spawned
     * @param player the Player the NPC has been spawned for
     */
    void handleSpawn(@NotNull NPC npc, @NotNull Player player);

}
