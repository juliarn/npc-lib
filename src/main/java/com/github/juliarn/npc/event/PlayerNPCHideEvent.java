package com.github.juliarn.npc.event;

import com.github.juliarn.npc.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * An event fired when a npc is hidden for a certain player.
 */
public class PlayerNPCHideEvent extends PlayerEvent {

  private static final HandlerList HANDLER_LIST = new HandlerList();

  private final NPC npc;

  /**
   * Constructs a new event instance.
   *
   * @param who The player who is no longer seeing the npc
   * @param npc The npc the player is no longer seeing
   */
  public PlayerNPCHideEvent(Player who, NPC npc) {
    super(who);
    this.npc = npc;
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
   * @return The npc which is no longer shown for the player
   */
  @NotNull
  public NPC getNPC() {
    return this.npc;
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
