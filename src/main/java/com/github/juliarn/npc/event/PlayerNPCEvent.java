package com.github.juliarn.npc.event;

import com.github.juliarn.npc.NPC;
import com.github.juliarn.npc.modifier.NPCModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

/**
 * Represents an event fired when an action between a player and a npc occurs.
 */
public abstract class PlayerNPCEvent extends PlayerEvent {

  /**
   * The npc involved in this event.
   */
  private final NPC npc;

  /**
   * Constructs a new event instance.
   *
   * @param who The player involved in this event
   * @param npc The npc involved in this event
   */
  public PlayerNPCEvent(Player who, NPC npc) {
    super(who);
    this.npc = npc;
  }

  /**
   * Sends the queued data in the provided {@link NPCModifier}s to the player involved in this
   * event.
   *
   * @param npcModifiers The {@link NPCModifier}s whose data should be send
   */
  public void send(NPCModifier... npcModifiers) {
    for (NPCModifier npcModifier : npcModifiers) {
      npcModifier.send(super.getPlayer());
    }
  }

  /**
   * @return The npc involved in this event
   */
  public NPC getNPC() {
    return this.npc;
  }
}
