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

    public <I, O> MetadataModifier queue(EntityMetadata<I, O> metadata, I value) {
        return this.queue(metadata.getIndex(), metadata.getMapper().apply(value), metadata.getOutputType());
    }

    public <T> MetadataModifier queue(int index, @NotNull T value, @NotNull Class<T> clazz) {
        return this.queue(index, value, MINECRAFT_VERSION < 9 ? null : WrappedDataWatcher.Registry.get(clazz));
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

    @Override
    public void send(@NotNull Player... targetPlayers) {
        PacketContainer packetContainer = super.newContainer(PacketType.Play.Server.ENTITY_METADATA);

        packetContainer.getWatchableCollectionModifier().write(0, this.metadata);

        super.send(targetPlayers);
    }

}
