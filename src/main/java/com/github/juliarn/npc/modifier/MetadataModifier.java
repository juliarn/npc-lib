package com.github.juliarn.npc.modifier;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.github.juliarn.npc.NPC;
import com.github.juliarn.npc.util.EntityMetadata;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MetadataModifier extends NPCModifier {

    private final List<WrappedWatchableObject> metadata = new ArrayList<>();

    public MetadataModifier(@NotNull NPC npc) {
        super(npc);
    }

    public MetadataModifier queueSneaking(boolean sneaking) {
        return this.queue(EntityMetadata.POSE, (byte) (sneaking ? 0x02 : 0));
    }

    public MetadataModifier queueSkinLayers(boolean showSkinLayers) {
        return this.queue(EntityMetadata.SKIN_LAYERS, (byte) (showSkinLayers ? 0xff : 0));
    }

    public <T> MetadataModifier queue(EntityMetadata<T> metadata, T value) {
        return this.queue(metadata.getIndex(), value, metadata.getType());
    }

    public <T> MetadataModifier queue(int index, @NotNull T value, @Nullable WrappedDataWatcher.Serializer serializer) {
        this.metadata.add(
                serializer == null ?
                        new WrappedWatchableObject(
                                index,
                                value
                        ) :
                        new WrappedWatchableObject(
                                new WrappedDataWatcher.WrappedDataWatcherObject(index, serializer),
                                value
                        )
        );

        return this;
    }

    public <T> MetadataModifier queue(int index, @NotNull T value, @NotNull Class<T> clazz) {
        return this.queue(index, value, MINECRAFT_VERSION < 9 ? null : WrappedDataWatcher.Registry.get(clazz));
    }

    @Override
    public void send(@NotNull Player... targetPlayers) {
        PacketContainer packetContainer = super.newContainer(PacketType.Play.Server.ENTITY_METADATA);

        packetContainer.getWatchableCollectionModifier().write(0, this.metadata);

        super.send(targetPlayers);
    }

}
