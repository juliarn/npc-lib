package com.github.juliarn.npc.modifier;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.github.juliarn.npc.NPC;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EquipmentModifier extends NPCModifier {

    public EquipmentModifier(@NotNull NPC npc) {
        super(npc);
    }

    public EquipmentModifier queue(@NotNull EnumWrappers.ItemSlot itemSlot, @NotNull ItemStack equipment) {
        PacketContainer packetContainer = super.newContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);

        packetContainer.getItemSlots().write(MINECRAFT_VERSION < 9 ? 1 : 0, itemSlot);
        packetContainer.getItemModifier().write(0, equipment);

        return this;
    }

    public EquipmentModifier queue(int itemSlot, @NotNull ItemStack equipment) {
        PacketContainer packetContainer = super.newContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);

        packetContainer.getIntegers().write(MINECRAFT_VERSION < 9 ? 1 : 0, itemSlot);
        packetContainer.getItemModifier().write(0, equipment);

        return this;
    }

}
