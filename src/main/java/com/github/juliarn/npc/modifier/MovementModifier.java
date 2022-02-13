package com.github.juliarn.npc.modifier;

import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.PacketContainer;
import com.github.juliarn.npc.NPC;
import org.bukkit.Location;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public class MovementModifier extends NPCModifier {

  /**
   * Creates a new npc modifier.
   *
   * @param npc The npc this modifier is for.
   */
  @ApiStatus.Internal
  public MovementModifier(@NotNull NPC npc) {
    super(npc);
  }

  @NotNull
  public MovementModifier queueTeleport(@NotNull Location to, boolean onGround) {
    byte yawAngle = getCompressAngle(to.getYaw());
    byte pitchAngle = getCompressAngle(to.getPitch());
    super.queueInstantly(((targetNpc, target) -> {
      PacketContainer container = new PacketContainer(Server.ENTITY_TELEPORT);
      container.getIntegers()
          .write(0, targetNpc.getEntityId());
      if(MINECRAFT_VERSION < 9) {
        container.getIntegers()
            .write(1, (int) Math.floor(to.getX() * 32.0D))
            .write(2, (int) Math.floor(to.getY() * 32.0D))
            .write(3, (int) Math.floor(to.getZ() * 32.0D));
      } else {
        container.getDoubles()
            .write(0, to.getX())
            .write(1, to.getY())
            .write(2, to.getZ());
      }
      container.getBytes()
          .write(0, yawAngle)
          .write(1, pitchAngle);
      container.getBooleans()
          .write(0, onGround);
      return container;
    }));
    return this;
  }

  private byte getCompressAngle(double angle) {
    return (byte) (angle * 256F / 360F);
  }
}
