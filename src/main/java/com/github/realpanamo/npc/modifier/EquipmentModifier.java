package com.github.realpanamo.npc.modifier;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.github.realpanamo.npc.NPC;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EquipmentModifier extends NPCModifier {

    public EquipmentModifier(@NotNull NPC npc) {
        super(npc);
    }

    public EquipmentModifier put(@NotNull EnumWrappers.ItemSlot itemSlot, @NotNull ItemStack equipment) {
        PacketContainer packetContainer = super.newContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);

        packetContainer.getItemSlots().write(0, itemSlot);
        packetContainer.getItemModifier().write(0, equipment);

        return this;
    }

}
