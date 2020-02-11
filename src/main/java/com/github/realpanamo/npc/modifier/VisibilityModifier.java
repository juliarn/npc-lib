package com.github.realpanamo.npc.modifier;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.github.realpanamo.npc.NPC;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class VisibilityModifier extends NPCModifier {

    public VisibilityModifier(@NotNull NPC npc) {
        super(npc);
    }

    public VisibilityModifier queueAddToPlayerList() {
        PacketContainer packetContainer = super.newContainer(PacketType.Play.Server.PLAYER_INFO, false);

        packetContainer.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);

        PlayerInfoData playerInfoData = new PlayerInfoData(
                super.npc.getGameProfile(),
                20,
                EnumWrappers.NativeGameMode.CREATIVE,
                null
        );
        packetContainer.getPlayerInfoDataLists().write(0, Collections.singletonList(playerInfoData));

        return this;
    }

    public VisibilityModifier queueRemoveFromPlayerList() {
        PacketContainer packetContainer = super.newContainer(PacketType.Play.Server.PLAYER_INFO, false);

        packetContainer.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);

        PlayerInfoData playerInfoData = new PlayerInfoData(
                super.npc.getGameProfile(),
                20,
                EnumWrappers.NativeGameMode.NOT_SET,
                null
        );
        packetContainer.getPlayerInfoDataLists().write(0, Collections.singletonList(playerInfoData));

        return this;
    }

    public VisibilityModifier queueSpawn() {
        PacketContainer packetContainer = super.newContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN);

        packetContainer.getUUIDs().write(0, super.npc.getGameProfile().getUUID());

        packetContainer.getDoubles()
                .write(0, super.npc.getLocation().getX())
                .write(1, super.npc.getLocation().getY())
                .write(2, super.npc.getLocation().getZ());

        packetContainer.getBytes()
                .write(0, (byte) (super.npc.getLocation().getYaw() * 256F / 360F))
                .write(1, (byte) (super.npc.getLocation().getPitch() * 256F / 360F));

        if (MINECRAFT_VERSION < 15) {
            packetContainer.getDataWatcherModifier().write(0, new WrappedDataWatcher());
        }

        return this;
    }

    public VisibilityModifier queueDestroy() {
        PacketContainer packetContainer = super.newContainer(PacketType.Play.Server.ENTITY_DESTROY, false);
        packetContainer.getIntegerArrays().write(0, new int[]{super.npc.getEntityId()});
        return this;
    }

}
