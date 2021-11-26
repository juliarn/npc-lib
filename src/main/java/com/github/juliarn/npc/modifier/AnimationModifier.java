package com.github.juliarn.npc.modifier;

import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.PacketContainer;
import com.github.juliarn.npc.NPC;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * A modifier for various animations a npc can play.
 */
public class AnimationModifier extends NPCModifier {

  /**
   * Creates a new modifier.
   *
   * @param npc The npc this modifier is for.
   * @see NPC#animation()
   */
  @ApiStatus.Internal
  public AnimationModifier(@NotNull NPC npc) {
    super(npc);
  }

  /**
   * Queues the animation to be played.
   *
   * @param entityAnimation The animation to play.
   * @return The same instance of this class, for chaining.
   */
  @NotNull
  public AnimationModifier queue(@NotNull EntityAnimation entityAnimation) {
    return this.queue(entityAnimation.id);
  }

  /**
   * Queues the animation to be played.
   *
   * @param animationId The id of the animation to play.
   * @return The same instance of this class, for chaining.
   */
  @NotNull
  public AnimationModifier queue(int animationId) {
    super.queueInstantly((targetNpc, target) -> {
      PacketContainer container = new PacketContainer(Server.ANIMATION);
      container.getIntegers()
          .write(0, targetNpc.getEntityId())
          .write(1, animationId);
      return container;
    });
    return this;
  }

  /**
   * All official supported entity animations.
   */
  public enum EntityAnimation {
    /**
     * Swings the main hand (hitting).
     */
    SWING_MAIN_ARM(0),
    /**
     * The damage effect.
     */
    TAKE_DAMAGE(1),
    /**
     * When a player enters a bed.
     */
    LEAVE_BED(2),
    /**
     * Swings the off hand (1.13+).
     */
    SWING_OFF_HAND(3),
    /**
     * When a player takes a critical effect.
     */
    CRITICAL_EFFECT(4),
    /**
     * When a player takes a critical effect caused by magic.
     */
    MAGIC_CRITICAL_EFFECT(5);

    /**
     * The id of the effect.
     */
    private final int id;

    EntityAnimation(int id) {
      this.id = id;
    }
  }
}
