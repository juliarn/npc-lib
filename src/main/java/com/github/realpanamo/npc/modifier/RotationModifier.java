package com.github.realpanamo.npc.modifier;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.github.realpanamo.npc.NPC;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public class RotationModifier extends NPCModifier {

    public RotationModifier(@NotNull NPC npc) {
        super(npc);
    }

    public RotationModifier queueRotate(float yaw, float pitch) {
        byte yawAngle = (byte) (yaw * 256.0F / 360.0F);

        PacketContainer entityHeadLookContainer = super.newContainer(PacketType.Play.Server.ENTITY_HEAD_ROTATION);

        entityHeadLookContainer.getBytes().write(0, yawAngle);

        PacketContainer entityLookContainer = super.newContainer(PacketType.Play.Server.ENTITY_LOOK);

        entityLookContainer.getBytes()
                .write(0, yawAngle)
                .write(1, (byte) (pitch * 256.0F / 360.0F));
        entityLookContainer.getBooleans().write(0, true);

        return this;
    }

    public RotationModifier queueLookAt(@NotNull Location location) {
        double xDifference = location.getX() - super.npc.getLocation().getX();
        double yDifference = location.getY() - super.npc.getLocation().getY();
        double zDifference = location.getZ() - super.npc.getLocation().getZ();

        double r = Math.sqrt(Math.pow(xDifference, 2) + Math.pow(yDifference, 2) + Math.pow(zDifference, 2));

        float yaw = (float) (-Math.atan2(xDifference, zDifference) / Math.PI * 180);
        yaw = yaw < 0 ? yaw + 360 : yaw;

        float pitch = (float) (-Math.asin(yDifference / r) / Math.PI * 180);

        return this.queueRotate(yaw, pitch);
    }

}
