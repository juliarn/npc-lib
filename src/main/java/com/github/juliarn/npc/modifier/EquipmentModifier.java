package com.github.juliarn.npc.modifier;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.github.juliarn.npc.NPC;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class EquipmentModifier extends NPCModifier {

  public EquipmentModifier(@NotNull NPC npc) {
    super(npc);
  }

  public EquipmentModifier queue(@NotNull EnumWrappers.ItemSlot itemSlot, @NotNull ItemStack equipment) {
    PacketContainer packetContainer = super.newContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);

    if (MINECRAFT_VERSION < 16) {
      packetContainer.getItemSlots().write(MINECRAFT_VERSION < 9 ? 1 : 0, itemSlot);
      packetContainer.getItemModifier().write(0, equipment);
    } else {
      packetContainer.getSlotStackPairLists().write(0, Collections.singletonList(new Pair<>(itemSlot, equipment)));
    }

    return this;
  }

  public EquipmentModifier queue(int itemSlot, @NotNull ItemStack equipment) {
    PacketContainer packetContainer = super.newContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);

    if (MINECRAFT_VERSION < 16) {
      packetContainer.getIntegers().write(MINECRAFT_VERSION < 9 ? 1 : 0, itemSlot);
      packetContainer.getItemModifier().write(0, equipment);
    } else {
      for (EnumWrappers.ItemSlot slot : EnumWrappers.ItemSlot.values()) {
        if (slot.ordinal() == itemSlot) {
          packetContainer.getSlotStackPairLists().write(0, Collections.singletonList(new Pair<>(slot, equipment)));
          break;
        }
      }
    }

    return this;
  }

}
