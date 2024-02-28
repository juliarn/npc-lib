/*
 * This file is part of npc-lib, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022-2023 Julian M., Pasqual K. and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.juliarn.npclib.minestom.protocol;

import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.Platform;
import com.github.juliarn.npclib.api.PlatformVersionAccessor;
import com.github.juliarn.npclib.api.event.InteractNpcEvent;
import com.github.juliarn.npclib.api.profile.ProfileProperty;
import com.github.juliarn.npclib.api.protocol.OutboundPacket;
import com.github.juliarn.npclib.api.protocol.PlatformPacketAdapter;
import com.github.juliarn.npclib.api.protocol.chat.Component;
import com.github.juliarn.npclib.api.protocol.enums.EntityAnimation;
import com.github.juliarn.npclib.api.protocol.enums.EntityPose;
import com.github.juliarn.npclib.api.protocol.enums.ItemSlot;
import com.github.juliarn.npclib.api.protocol.enums.PlayerInfoAction;
import com.github.juliarn.npclib.api.protocol.meta.EntityMetadata;
import com.github.juliarn.npclib.api.protocol.meta.EntityMetadataFactory;
import com.github.juliarn.npclib.common.event.DefaultAttackNpcEvent;
import com.github.juliarn.npclib.common.event.DefaultInteractNpcEvent;
import com.github.juliarn.npclib.minestom.util.MinestomUtil;
import io.leangen.geantyref.TypeFactory;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.client.play.ClientInteractEntityPacket;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.network.packet.server.play.EntityAnimationPacket;
import net.minestom.server.network.packet.server.play.EntityEquipmentPacket;
import net.minestom.server.network.packet.server.play.EntityHeadLookPacket;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.EntityRotationPacket;
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public final class MinestomProtocolAdapter implements PlatformPacketAdapter<Instance, Player, ItemStack, Object> {

  private static final MinestomProtocolAdapter INSTANCE = new MinestomProtocolAdapter();

  private static final Type OPTIONAL_CHAT_COMPONENT_TYPE = TypeFactory.parameterizedClass(
    Optional.class,
    net.kyori.adventure.text.Component.class);

  private static final EnumMap<ItemSlot, EquipmentSlot> ITEM_SLOT_CONVERTER;
  private static final EnumMap<EntityPose, Entity.Pose> ENTITY_POSE_CONVERTER;
  private static final EnumMap<Player.Hand, InteractNpcEvent.Hand> HAND_CONVERTER;
  private static final EnumMap<EntityAnimation, EntityAnimationPacket.Animation> ANIMATION_CONVERTER;

  private static final Map<Type, Function<Object, Metadata.Entry<?>>> META_ENTRY_FACTORY;
  private static final Map<Type, Map.Entry<Type, UnaryOperator<Object>>> SERIALIZER_CONVERTERS;

  private static final EnumSet<PlayerInfoUpdatePacket.Action> ADD_ACTIONS = EnumSet.of(
    PlayerInfoUpdatePacket.Action.ADD_PLAYER,
    PlayerInfoUpdatePacket.Action.UPDATE_LISTED,
    PlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
    PlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
    PlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME
  );

  static {
    // associate item slots with their respective minestom lib enum
    ITEM_SLOT_CONVERTER = new EnumMap<>(ItemSlot.class);
    ITEM_SLOT_CONVERTER.put(ItemSlot.MAIN_HAND, EquipmentSlot.MAIN_HAND);
    ITEM_SLOT_CONVERTER.put(ItemSlot.OFF_HAND, EquipmentSlot.OFF_HAND);
    ITEM_SLOT_CONVERTER.put(ItemSlot.FEET, EquipmentSlot.BOOTS);
    ITEM_SLOT_CONVERTER.put(ItemSlot.LEGS, EquipmentSlot.LEGGINGS);
    ITEM_SLOT_CONVERTER.put(ItemSlot.CHEST, EquipmentSlot.CHESTPLATE);
    ITEM_SLOT_CONVERTER.put(ItemSlot.HEAD, EquipmentSlot.HELMET);

    // associate entity poses with their respective minestom lib enum
    ENTITY_POSE_CONVERTER = new EnumMap<>(EntityPose.class);
    ENTITY_POSE_CONVERTER.put(EntityPose.STANDING, Entity.Pose.STANDING);
    ENTITY_POSE_CONVERTER.put(EntityPose.FALL_FLYING, Entity.Pose.FALL_FLYING);
    ENTITY_POSE_CONVERTER.put(EntityPose.SLEEPING, Entity.Pose.SLEEPING);
    ENTITY_POSE_CONVERTER.put(EntityPose.SWIMMING, Entity.Pose.SWIMMING);
    ENTITY_POSE_CONVERTER.put(EntityPose.SPIN_ATTACK, Entity.Pose.SPIN_ATTACK);
    ENTITY_POSE_CONVERTER.put(EntityPose.CROUCHING, Entity.Pose.SNEAKING);
    ENTITY_POSE_CONVERTER.put(EntityPose.LONG_JUMPING, Entity.Pose.LONG_JUMPING);
    ENTITY_POSE_CONVERTER.put(EntityPose.DYING, Entity.Pose.DYING);
    ENTITY_POSE_CONVERTER.put(EntityPose.CROAKING, Entity.Pose.CROAKING);
    ENTITY_POSE_CONVERTER.put(EntityPose.USING_TONGUE, Entity.Pose.USING_TONGUE);
    ENTITY_POSE_CONVERTER.put(EntityPose.ROARING, Entity.Pose.ROARING);
    ENTITY_POSE_CONVERTER.put(EntityPose.SNIFFING, Entity.Pose.SNIFFING);
    ENTITY_POSE_CONVERTER.put(EntityPose.EMERGING, Entity.Pose.EMERGING);
    ENTITY_POSE_CONVERTER.put(EntityPose.DIGGING, Entity.Pose.DIGGING);
    ENTITY_POSE_CONVERTER.put(EntityPose.SLIDING, Entity.Pose.SLIDING);
    ENTITY_POSE_CONVERTER.put(EntityPose.SHOOTING, Entity.Pose.SHOOTING);
    ENTITY_POSE_CONVERTER.put(EntityPose.INHALING, Entity.Pose.INHALING);

    // associate hands with their respective minestom lib enum
    HAND_CONVERTER = new EnumMap<>(Player.Hand.class);
    HAND_CONVERTER.put(Player.Hand.MAIN, InteractNpcEvent.Hand.MAIN_HAND);
    HAND_CONVERTER.put(Player.Hand.OFF, InteractNpcEvent.Hand.OFF_HAND);

    // associate animations with their respective minestom lib enum
    ANIMATION_CONVERTER = new EnumMap<>(EntityAnimation.class);
    ANIMATION_CONVERTER.put(EntityAnimation.SWING_MAIN_ARM, EntityAnimationPacket.Animation.SWING_MAIN_ARM);
    ANIMATION_CONVERTER.put(EntityAnimation.TAKE_DAMAGE, EntityAnimationPacket.Animation.TAKE_DAMAGE);
    ANIMATION_CONVERTER.put(EntityAnimation.LEAVE_BED, EntityAnimationPacket.Animation.LEAVE_BED);
    ANIMATION_CONVERTER.put(EntityAnimation.SWING_OFF_HAND, EntityAnimationPacket.Animation.SWING_OFF_HAND);
    ANIMATION_CONVERTER.put(EntityAnimation.CRITICAL_EFFECT, EntityAnimationPacket.Animation.CRITICAL_EFFECT);
    ANIMATION_CONVERTER.put(
      EntityAnimation.MAGIC_CRITICAL_EFFECT,
      EntityAnimationPacket.Animation.MAGICAL_CRITICAL_EFFECT);

    // init the meta value converter
    SERIALIZER_CONVERTERS = new HashMap<>(1);
    //noinspection SuspiciousMethodCalls
    SERIALIZER_CONVERTERS.put(EntityPose.class, new AbstractMap.SimpleImmutableEntry<>(
      Entity.Pose.class,
      ENTITY_POSE_CONVERTER::get));
    SERIALIZER_CONVERTERS.put(
      TypeFactory.parameterizedClass(Optional.class, Component.class),
      new AbstractMap.SimpleImmutableEntry<>(
        OPTIONAL_CHAT_COMPONENT_TYPE,
        value -> {
          //noinspection unchecked
          Optional<Component> optionalComponent = (Optional<Component>) value;
          return optionalComponent.map(component -> {
            // build the component based on the given input
            String rawMessage = component.rawMessage();
            if (rawMessage != null) {
              return LegacyComponentSerializer.legacySection().deserialize(rawMessage);
            } else {
              return GsonComponentSerializer.gson().deserializeOrNull(component.encodedJsonMessage());
            }
          });
        }
      ));

    // init the meta entry factories
    META_ENTRY_FACTORY = new HashMap<>(6);
    META_ENTRY_FACTORY.put(byte.class, value -> Metadata.Byte((byte) value));
    META_ENTRY_FACTORY.put(int.class, value -> Metadata.VarInt((int) value));
    META_ENTRY_FACTORY.put(float.class, value -> Metadata.Float((float) value));
    META_ENTRY_FACTORY.put(boolean.class, value -> Metadata.Boolean((boolean) value));
    META_ENTRY_FACTORY.put(String.class, value -> Metadata.String((String) value));
    META_ENTRY_FACTORY.put(Entity.Pose.class, value -> Metadata.Pose((Entity.Pose) value));
    //noinspection unchecked
    META_ENTRY_FACTORY.put(
      OPTIONAL_CHAT_COMPONENT_TYPE,
      value -> Metadata.OptChat(((Optional<net.kyori.adventure.text.Component>) value).orElse(null)));
  }

  private MinestomProtocolAdapter() {
  }

  public static @NotNull PlatformPacketAdapter<Instance, Player, ItemStack, Object> minestomProtocolAdapter() {
    return INSTANCE;
  }

  private static @NotNull Metadata.Entry<?> createMetadataEntry(@NotNull Type type, @NotNull Object value) {
    // check if we need to convert the value before creating the meta object
    Map.Entry<Type, UnaryOperator<Object>> converter = SERIALIZER_CONVERTERS.get(type);
    if (converter != null) {
      type = converter.getKey();
      value = converter.getValue().apply(value);
    }

    // get the meta factory which is converting the type
    Function<Object, Metadata.Entry<?>> metaFactory = META_ENTRY_FACTORY.get(type);
    if (metaFactory == null) {
      // unable to handle that
      throw new IllegalArgumentException("Unsupported type: " + type);
    }

    // create the meta entry
    return metaFactory.apply(value);
  }

  @Override
  public @NotNull OutboundPacket<Instance, Player, ItemStack, Object> createEntitySpawnPacket() {
    return (player, npc) -> {
      Pos position = MinestomUtil.minestomFromPosition(npc.position());
      SpawnEntityPacket packet = new SpawnEntityPacket(
        npc.entityId(),
        npc.profile().uniqueId(),
        EntityType.PLAYER.id(),
        position,
        0F,
        0,
        (short) 0,
        (short) 0,
        (short) 0);
      player.sendPacket(packet);
    };
  }

  @Override
  public @NotNull OutboundPacket<Instance, Player, ItemStack, Object> createEntityRemovePacket() {
    return (player, npc) -> {
      DestroyEntitiesPacket packet = new DestroyEntitiesPacket(npc.entityId());
      player.sendPacket(packet);
    };
  }

  @Override
  public @NotNull OutboundPacket<Instance, Player, ItemStack, Object> createPlayerInfoPacket(
    @NotNull PlayerInfoAction action
  ) {
    return (player, npc) -> npc.settings().profileResolver().resolveNpcProfile(player, npc).thenAcceptAsync(profile -> {
      if (action == PlayerInfoAction.REMOVE_PLAYER) {
        // just remove the player from the tablist
        PlayerInfoRemovePacket removePacket = new PlayerInfoRemovePacket(profile.uniqueId());
        player.sendPacket(removePacket);
        return;
      }

      // convert the profile properties
      List<PlayerInfoUpdatePacket.Property> properties = new ArrayList<>();
      for (ProfileProperty property : profile.properties()) {
        PlayerInfoUpdatePacket.Property prop = new PlayerInfoUpdatePacket.Property(
          property.name(),
          property.value(),
          property.signature());
        properties.add(prop);
      }

      // build the action
      PlayerInfoUpdatePacket updatePacket = new PlayerInfoUpdatePacket(
        ADD_ACTIONS,
        Collections.singletonList(new PlayerInfoUpdatePacket.Entry(
          profile.uniqueId(),
          profile.name(),
          properties,
          false,
          20,
          GameMode.CREATIVE,
          null,
          null
        )));
      player.sendPacket(updatePacket);
    });
  }

  @Override
  public @NotNull OutboundPacket<Instance, Player, ItemStack, Object> createRotationPacket(float yaw, float pitch) {
    return (player, npc) -> {
      // head rotation (https://wiki.vg/Protocol#Entity_Head_Look) & rotation (https://wiki.vg/Protocol#Player_Rotation)
      EntityHeadLookPacket headLookPacket = new EntityHeadLookPacket(npc.entityId(), yaw);
      EntityRotationPacket rotationPacket = new EntityRotationPacket(npc.entityId(), yaw, pitch, true);

      player.sendPackets(headLookPacket, rotationPacket);
    };
  }

  @Override
  public @NotNull OutboundPacket<Instance, Player, ItemStack, Object> createAnimationPacket(
    @NotNull EntityAnimation animation
  ) {
    return (player, npc) -> {
      EntityAnimationPacket.Animation convertedAnimation = ANIMATION_CONVERTER.get(animation);
      EntityAnimationPacket packet = new EntityAnimationPacket(npc.entityId(), convertedAnimation);

      player.sendPacket(packet);
    };
  }

  @Override
  public @NotNull OutboundPacket<Instance, Player, ItemStack, Object> createEquipmentPacket(
    @NotNull ItemSlot slot,
    @NotNull ItemStack item
  ) {
    return (player, npc) -> {
      // get the meta of the item to send
      EquipmentSlot equipmentSlot = ITEM_SLOT_CONVERTER.get(slot);
      Map<EquipmentSlot, ItemStack> items = Collections.singletonMap(equipmentSlot, item);

      // send the packet
      EntityEquipmentPacket packet = new EntityEquipmentPacket(npc.entityId(), items);
      player.sendPacket(packet);
    };
  }

  @Override
  public @NotNull OutboundPacket<Instance, Player, ItemStack, Object> createCustomPayloadPacket(
    @NotNull String channelId,
    byte[] payload
  ) {
    return (player, npc) -> {
      PluginMessagePacket packet = new PluginMessagePacket(channelId, payload);
      player.sendPacket(packet);
    };
  }

  @Override
  public @NotNull <T, O> OutboundPacket<Instance, Player, ItemStack, Object> createEntityMetaPacket(
    @NotNull EntityMetadataFactory<T, O> metadata,
    @NotNull T value
  ) {
    return (player, npc) -> {
      // create the entity meta
      PlatformVersionAccessor versionAccessor = npc.platform().versionAccessor();
      EntityMetadata<O> entityMetadata = metadata.create(value, versionAccessor);

      // check if the meta is available
      if (!entityMetadata.available()) {
        return;
      }

      Map<Integer, Metadata.Entry<?>> metadataEntries = new HashMap<>();
      metadataEntries.put(entityMetadata.index(), createMetadataEntry(entityMetadata.type(), entityMetadata.value()));

      // add all dependant metas
      for (EntityMetadataFactory<T, Object> relatedMetadata : metadata.relatedMetadata()) {
        EntityMetadata<Object> related = relatedMetadata.create(value, versionAccessor);
        if (related.available()) {
          metadataEntries.put(related.index(), createMetadataEntry(related.type(), related.value()));
        }
      }

      // create & send the packet
      EntityMetaDataPacket packet = new EntityMetaDataPacket(npc.entityId(), metadataEntries);
      player.sendPacket(packet);
    };
  }

  @Override
  public void initialize(@NotNull Platform<Instance, Player, ItemStack, Object> platform) {
    MinecraftServer.getGlobalEventHandler().addListener(PlayerPacketEvent.class, event -> {
      // check if the inbound packet is USE_ENTITY, it's the only interesting for us
      if (event.getPacket() instanceof ClientInteractEntityPacket packet) {
        // get the associated npc from the tracked entities
        Npc<Instance, Player, ItemStack, Object> npc = platform.npcTracker().npcById(packet.targetId());
        if (npc != null) {
          // call the correct event based on the taken action
          if (packet.type() instanceof ClientInteractEntityPacket.Attack) {
            platform.eventManager().post(DefaultAttackNpcEvent.attackNpc(npc, event.getPlayer()));
          } else if (packet.type() instanceof ClientInteractEntityPacket.Interact interact) {
            // extract the used hand from the packet
            InteractNpcEvent.Hand hand = HAND_CONVERTER.get(interact.hand());

            // call the event
            platform.eventManager().post(DefaultInteractNpcEvent.interactNpc(npc, event.getPlayer(), hand));
          }

          // don't pass the packet to the server
          event.setCancelled(true);
        }
      }
    });
  }
}
