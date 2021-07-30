package com.github.juliarn.npc.event;

import com.github.juliarn.npc.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * An event fired when a NPC is shown for a certain player.
 */
public class PlayerNPCShowEvent extends PlayerNPCEvent {

  private static final HandlerList HANDLER_LIST = new HandlerList();

  /**
   * Constructs a new event instance.
   *
   * @param who The player who is now seeing the npc
   * @param npc The npc the player is now seeing
   */
  public PlayerNPCShowEvent(Player who, NPC npc) {
    super(who, npc);
  }

  /**
   * Get the handlers for this event.
   *
   * @return the handlers for this event.
   */
  @NotNull
  public static HandlerList getHandlerList() {
    return HANDLER_LIST;
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  public HandlerList getHandlers() {
    return HANDLER_LIST;
  }
}
