package com.github.juliarn.npc.modifier;

import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.PacketContainer;
import com.github.juliarn.npc.NPC;
import org.bukkit.Location;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * A modifier for modifying the rotation of a player.
 */
public class RotationModifier extends NPCModifier {

  /**
   * Creates a new modifier.
   *
   * @param npc The npc this modifier is for.
   * @see NPC#rotation()
   */
  @ApiStatus.Internal
  public RotationModifier(@NotNull NPC npc) {
    super(npc);
  }

  /**
   * Queues the change of the current rotation of the wrapped npc.
   *
   * @param yaw   The yaw of the target rotated location.
   * @param pitch The pitch of the target rotated location.
   * @return The same instance of this class, for chaining.
   */
  @NotNull
  public RotationModifier queueRotate(float yaw, float pitch) {
    byte yawAngle = (byte) (yaw * 256F / 360F);
    byte pitchAngle = (byte) (pitch * 256F / 360F);
    // head rotation
    super.queueInstantly((targetNpc, target) -> {
      PacketContainer container = new PacketContainer(Server.ENTITY_HEAD_ROTATION);
      container.getIntegers().write(0, targetNpc.getEntityId());
      container.getBytes().write(0, yawAngle);

      return container;
    });
    // entity position
    super.queueInstantly((targetNpc, target) -> {
      PacketContainer container;
      if (MINECRAFT_VERSION < 9) {
        container = new PacketContainer(Server.ENTITY_TELEPORT);
        container.getIntegers().write(0, targetNpc.getEntityId());

        Location location = super.npc.getLocation();
        container.getIntegers()
            .write(1, (int) Math.floor(location.getX() * 32.0D))
            .write(2, (int) Math.floor(location.getY() * 32.0D))
            .write(3, (int) Math.floor(location.getZ() * 32.0D));
      } else {
        container = new PacketContainer(Server.ENTITY_LOOK);
        container.getIntegers().write(0, targetNpc.getEntityId());
      }

      container.getBytes()
          .write(0, yawAngle)
          .write(1, pitchAngle);
      container.getBooleans().write(0, true);
      return container;
    });

    return this;
  }

  /**
   * Queues the change of the current rotation of the wrapped npc.
   *
   * @param location the target location the npc should look to.
   * @return The same instance of this class, for chaining.
   */
  @NotNull
  public RotationModifier queueLookAt(@NotNull Location location) {
    double xDifference = location.getX() - super.npc.getLocation().getX();
    double yDifference = location.getY() - super.npc.getLocation().getY();
    double zDifference = location.getZ() - super.npc.getLocation().getZ();

    double r = Math
        .sqrt(Math.pow(xDifference, 2) + Math.pow(yDifference, 2) + Math.pow(zDifference, 2));

    float yaw = (float) (-Math.atan2(xDifference, zDifference) / Math.PI * 180D);
    yaw = yaw < 0 ? yaw + 360 : yaw;

    float pitch = (float) (-Math.asin(yDifference / r) / Math.PI * 180D);

    return this.queueRotate(yaw, pitch);
  }
}
