package com.github.juliarn.npc.modifier;

import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.github.juliarn.npc.NPC;
import java.util.Collections;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * A modifier for modifying the equipment of a player.
 */
public class EquipmentModifier extends NPCModifier {

  /**
   * The id of the main hand item slot.
   *
   * @since 2.5-SNAPSHOT
   */
  public static final int MAINHAND = 0;
  /**
   * The id of the off hand item slot.
   *
   * @since 2.5-SNAPSHOT
   */
  public static final int OFFHAND = 1;
  /**
   * The id of the feet armor item slot.
   *
   * @since 2.5-SNAPSHOT
   */
  public static final int FEET = 2;
  /**
   * The id of the legs armor item slot.
   *
   * @since 2.5-SNAPSHOT
   */
  public static final int LEGS = 3;
  /**
   * The id of the chest armor item slot.
   *
   * @since 2.5-SNAPSHOT
   */
  public static final int CHEST = 4;
  /**
   * The id of the head armor item slot.
   *
   * @since 2.5-SNAPSHOT
   */
  public static final int HEAD = 5;

  private static final EnumWrappers.ItemSlot[] ITEM_SLOTS = EnumWrappers.ItemSlot.values();

  /**
   * Creates a new modifier.
   *
   * @param npc The npc this modifier is for.
   * @see NPC#equipment()
   */
  @ApiStatus.Internal
  public EquipmentModifier(@NotNull NPC npc) {
    super(npc);
  }

  /**
   * Queues the change of an item slot using the protocol lib item slot enum wrapper directly. If
   * you don't want to use protocol lib as a dependency, use {@link #queue(int, ItemStack)} with the
   * item slot numbers defined at the top of this class.
   *
   * @param itemSlot  The item slot the modification should take place.
   * @param equipment The item which should be placed at the specific slot.
   * @return The same instance of this class, for chaining.
   */
  @NotNull
  public EquipmentModifier queue(
      @NotNull EnumWrappers.ItemSlot itemSlot,
      @NotNull ItemStack equipment
  ) {
    super.queueInstantly((targetNpc, target) -> {
      PacketContainer container = new PacketContainer(Server.ENTITY_EQUIPMENT);
      container.getIntegers().write(0, targetNpc.getEntityId());

      if (MINECRAFT_VERSION < 16) {
        if (MINECRAFT_VERSION < 9) {
          // fix the item slot association for minecraft 1.8
          int slotId = itemSlot.ordinal();
          if (slotId > 0) {
            // the main hand representation is 0, that didn't change - 1 was added as the representation
            // of the off-hand so everything != 0 needs to be shifted one down to skip the unknown
            // off-hand slot
            slotId--;
          }

          container.getIntegers().write(1, slotId);
        } else {
          container.getItemSlots().write(0, itemSlot);
        }
        container.getItemModifier().write(0, equipment);
      } else {
        container.getSlotStackPairLists()
            .write(0, Collections.singletonList(new Pair<>(itemSlot, equipment)));
      }
      return container;
    });
    return this;
  }

  /**
   * Queues the change of an item slot using the specified slot number.
   *
   * @param itemSlot  The item slot the modification should take place.
   * @param equipment The item which should be placed at the specific slot.
   * @return The same instance of this class, for chaining.
   */
  @NotNull
  public EquipmentModifier queue(int itemSlot, @NotNull ItemStack equipment) {
    for (EnumWrappers.ItemSlot slot : ITEM_SLOTS) {
      if (slot.ordinal() == itemSlot) {
        return queue(slot, equipment);
      }
    }

    throw new IllegalArgumentException("Provided itemSlot is invalid");
  }
}
