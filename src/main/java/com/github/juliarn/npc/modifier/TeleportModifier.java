package com.github.juliarn.npc.modifier;

import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.PacketContainer;
import com.github.juliarn.npc.NPC;
import org.bukkit.Location;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * A teleport modifier for a player
 *
 * @since 2.7-SNAPSHOT
 * @author unldenis
 */
public class TeleportModifier extends NPCModifier {

  /**
   * Creates a new modifier.
   *
   * @param npc The npc this modifier is for.
   * @see NPC#teleport()
   */
  @ApiStatus.Internal
  public TeleportModifier(@NotNull NPC npc) {
    super(npc);
  }

  /**
   * Queues a teleportation packet for the wrapped npc.
   *
   * @param location the target location the npc should teleport to.
   * @param onGround if the destination is on the ground.
   * @return The same instance of this class, for chaining.
   */
  @NotNull
  public TeleportModifier queueTeleport(@NotNull Location location, boolean onGround) {
    byte yawAngle = this.getCompressedAngle(location.getYaw());
    byte pitchAngle = this.getCompressedAngle(location.getPitch());
    super.queueInstantly((targetNpc, target) -> {
      PacketContainer container = new PacketContainer(Server.ENTITY_TELEPORT);
      container.getIntegers().write(0, targetNpc.getEntityId());
      if (MINECRAFT_VERSION < 9) {
        container.getIntegers().write(1, (int) Math.floor(location.getX() * 32.0D));
        container.getIntegers().write(2, (int) Math.floor(location.getY() * 32.0D));
        container.getIntegers().write(3, (int) Math.floor(location.getZ() * 32.0D));
      } else {
        container.getDoubles().write(0, location.getX());
        container.getDoubles().write(1, location.getY());
        container.getDoubles().write(2, location.getZ());
      }
      container.getBytes().write(0, yawAngle);
      container.getBytes().write(1, pitchAngle);
      container.getBooleans().write(0, onGround);

      super.npc.setLocation(location);
      return container;
    });
    return this;
  }

  /**
   * Queues a teleportation packet for the wrapped npc.
   *
   * @param location the target location the npc should teleport to.
   * @return The same instance of this class, for chaining.
   */
  @NotNull
  public TeleportModifier queueTeleport(@NotNull Location location) {
    return this.queueTeleport(location, false);
  }

  private byte getCompressedAngle(double angle) {
    return (byte) (angle * 256F / 360F);
  }
}
