package com.github.juliarn.npc.modifier;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.github.juliarn.npc.NPC;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MetadataModifier extends NPCModifier {

    private List<WrappedWatchableObject> metadata = new ArrayList<>();

    public MetadataModifier(@NotNull NPC npc) {
        super(npc);
    }

    public MetadataModifier queueSneaking(boolean sneaking) {
        return this.queue(0, (byte) (sneaking ? 0x02 : 0), Byte.class);
    }

    public MetadataModifier queuePotionEffect(int color) {
        return this.queuePotionEffect(color, false);
    }

    public MetadataModifier queuePotionEffect(int color, boolean ambient) {
        int indexModifier = MINECRAFT_VERSION < 14 ? -1 : 0;
        return this
                .queue(9 + indexModifier, color, Integer.class)
                .queue(10 + indexModifier, ambient, Boolean.class);
    }

    public MetadataModifier queueArrows(int arrowAmount) {
        int index = MINECRAFT_VERSION < 14 ? 10 : 11;
        return this.queue(index, arrowAmount, Integer.class);
    }

    public MetadataModifier queueSkinLayers(boolean showSkinLayers) {
        int index = MINECRAFT_VERSION < 15 ? (MINECRAFT_VERSION < 14 ? 13 : 15) : 16;
        return this.queue(index, (byte) (showSkinLayers ? 0xff : 0), Byte.class);
    }

    public <T> MetadataModifier queue(int index, @NotNull T value, @NotNull WrappedDataWatcher.Serializer serializer) {
        this.metadata.add(new WrappedWatchableObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(index, serializer),
                value
        ));

        return this;
    }

    public <T> MetadataModifier queue(int index, @NotNull T value, @NotNull Class<T> clazz) {
        return this.queue(index, value, WrappedDataWatcher.Registry.get(clazz));
    }

    private void setMetadata(@NotNull List<WrappedWatchableObject> metadata) {
        PacketContainer packetContainer = super.newContainer(PacketType.Play.Server.ENTITY_METADATA);

        packetContainer.getWatchableCollectionModifier().write(0, metadata);
    }

    @Override
    public void send(@NotNull Player... targetPlayers) {
        if (!this.metadata.isEmpty()) {
            this.setMetadata(this.metadata);
        }

        super.send(targetPlayers);
    }

}
