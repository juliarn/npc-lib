package com.github.juliarn.npc.event;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.github.juliarn.npc.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * An event called when a player interacts with a npc.
 */
public class PlayerNPCInteractEvent extends PlayerNPCEvent {

  private static final HandlerList HANDLER_LIST = new HandlerList();

  /**
   * The action type of the interact.
   */
  private final EntityUseAction action;

  /**
   * Constructs a new event instance.
   *
   * @param who    The player who interacted with the npc.
   * @param npc    The npc with whom the player has interacted.
   * @param action The action type of the interact.
   */
  public PlayerNPCInteractEvent(
      @NotNull Player who,
      @NotNull NPC npc,
      @NotNull EnumWrappers.EntityUseAction action) {
    this(who, npc, EntityUseAction.fromHandle(action));
  }

  /**
   * Constructs a new event instance.
   *
   * @param who    The player who interacted with the npc.
   * @param npc    The npc with whom the player has interacted.
   * @param action The action type of the interact.
   */
  public PlayerNPCInteractEvent(
      @NotNull Player who,
      @NotNull NPC npc,
      @NotNull EntityUseAction action) {
    super(who, npc);
    this.action = action;
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
   * Gets the interact action as a protocol lib wrapper. This is not recommended to use, the
   * alternative is {@link #getUseAction()}.
   *
   * @return the interact action as a protocol lib wrapper.
   */
  @NotNull
  @Deprecated
  @ApiStatus.ScheduledForRemoval
  public EnumWrappers.EntityUseAction getAction() {
    return this.action.handle;
  }

  /**
   * Gets the interact action with the associated npc.
   *
   * @return the interact action with the associated npc.
   * @since 2.5-SNAPSHOT
   */
  @NotNull
  public EntityUseAction getUseAction() {
    return this.action;
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  public HandlerList getHandlers() {
    return HANDLER_LIST;
  }

  /**
   * A wrapper for the interact action with a npc.
   *
   * @since 2.5-SNAPSHOT
   */
  public enum EntityUseAction {
    /**
     * A normal interact. (right click)
     */
    INTERACT(EnumWrappers.EntityUseAction.INTERACT),
    /**
     * An attack. (left click)
     */
    ATTACK(EnumWrappers.EntityUseAction.ATTACK),
    /**
     * A normal interact to a specific entity. (right click)
     */
    INTERACT_AT(EnumWrappers.EntityUseAction.INTERACT_AT);

    /**
     * All values of the action, to prevent a copy.
     */
    private static final EntityUseAction[] VALUES = values();
    /**
     * The entity use action as the protocol lib wrapper.
     */
    private final EnumWrappers.EntityUseAction handle;

    /**
     * Constructs an instance of the interact action.
     *
     * @param handle The protocol lib association with the action.
     */
    EntityUseAction(EnumWrappers.EntityUseAction handle) {
      this.handle = handle;
    }

    /**
     * Converts the protocol lib wrapper to the associated action.
     *
     * @param action The protocol lib wrapper of the association.
     * @return The association with the protocol lib wrapper action.
     * @throws IllegalArgumentException When no association was found.
     */
    @NotNull
    private static EntityUseAction fromHandle(@NotNull EnumWrappers.EntityUseAction action) {
      for (EntityUseAction value : VALUES) {
        if (value.handle == action) {
          return value;
        }
      }
      throw new IllegalArgumentException("No use action for handle: " + action);
    }
  }
}
