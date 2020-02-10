package com.github.realpanamo.npc.modifier;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.github.realpanamo.npc.NPC;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MetadataModifier extends NPCModifier {

    private List<WrappedWatchableObject> metadata = new ArrayList<>();

    public MetadataModifier(@NotNull NPC npc) {
        super(npc);
    }

    public MetadataModifier skinLayers(boolean showSkinLayers) {
        return this.putSingle(16, (byte) (showSkinLayers ? 0xff : 0), Byte.class);
    }

    public MetadataModifier sneaking(boolean sneaking) {
        return this.putSingle(0, (byte) (sneaking ? 0x02 : 0), Byte.class);
    }

    private void setMetadata(@NotNull List<WrappedWatchableObject> metadata) {
        PacketContainer packetContainer = super.newContainer(PacketType.Play.Server.ENTITY_METADATA);

        packetContainer.getWatchableCollectionModifier().write(0, metadata);
    }

    public <T> MetadataModifier putSingle(int index, @NotNull T value, @NotNull WrappedDataWatcher.Serializer serializer) {
        this.metadata.add(new WrappedWatchableObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(index, serializer),
                value
        ));

        return this;
    }

    public <T> MetadataModifier putSingle(int index, @NotNull T value, @NotNull Class<T> clazz) {
        return this.putSingle(index, value, WrappedDataWatcher.Registry.get(clazz));
    }

    @Override
    public void send(@NotNull Player... targetPlayers) {
        if (!this.metadata.isEmpty()) {
            this.setMetadata(this.metadata);
        }

        super.send(targetPlayers);
    }

}
