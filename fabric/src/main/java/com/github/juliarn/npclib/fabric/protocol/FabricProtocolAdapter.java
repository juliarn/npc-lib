/*
 * This file is part of npc-lib, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022-2025 Julian M., Pasqual K. and contributors
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

package com.github.juliarn.npclib.fabric.protocol;

import com.github.juliarn.npclib.api.Platform;
import com.github.juliarn.npclib.api.PlatformVersionAccessor;
import com.github.juliarn.npclib.api.event.InteractNpcEvent;
import com.github.juliarn.npclib.api.profile.ProfileProperty;
import com.github.juliarn.npclib.api.protocol.OutboundPacket;
import com.github.juliarn.npclib.api.protocol.PlatformPacketAdapter;
import com.github.juliarn.npclib.api.protocol.enums.EntityAnimation;
import com.github.juliarn.npclib.api.protocol.enums.EntityPose;
import com.github.juliarn.npclib.api.protocol.enums.ItemSlot;
import com.github.juliarn.npclib.api.protocol.enums.PlayerInfoAction;
import com.github.juliarn.npclib.api.protocol.meta.EntityMetadata;
import com.github.juliarn.npclib.api.protocol.meta.EntityMetadataFactory;
import com.github.juliarn.npclib.fabric.ext.EntityAnimationS2CPacketExt;
import com.github.juliarn.npclib.fabric.ext.EntitySetHeadYawS2CPacketExt;
import com.github.juliarn.npclib.fabric.ext.PlayerListS2CPacketExt;
import com.github.juliarn.npclib.fabric.mixin.accessor.DataTrackerAccessor;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.datafixers.util.Pair;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;


public final class FabricProtocolAdapter implements
  PlatformPacketAdapter<World, ServerPlayerEntity, ItemStack, Object> {

  private static final FabricProtocolAdapter INSTANCE = new FabricProtocolAdapter();

  private FabricProtocolAdapter() {
  }

  public static @NotNull PlatformPacketAdapter<World, ServerPlayerEntity, ItemStack, Object> fabricProtocolAdapter() {
    return INSTANCE;
  }

  private static final EnumMap<ItemSlot, EquipmentSlot> ITEM_SLOT_CONVERTER;
  private static final EnumMap<EntityPose, net.minecraft.entity.EntityPose> ENTITY_POSE_CONVERTER;
  private static final EnumMap<Hand, InteractNpcEvent.Hand> HAND_CONVERTER;

  //private static final Map<Type, Function<Object, Metadata.Entry<?>>> META_ENTRY_FACTORY;
  //private static final Map<Type, Map.Entry<Type, UnaryOperator<Object>>> SERIALIZER_CONVERTERS;

  private static final EnumSet<PlayerListS2CPacket.Action> ADD_ACTIONS = EnumSet.of(
    PlayerListS2CPacket.Action.ADD_PLAYER,
    PlayerListS2CPacket.Action.UPDATE_LISTED,
    PlayerListS2CPacket.Action.UPDATE_LATENCY,
    PlayerListS2CPacket.Action.UPDATE_GAME_MODE,
    PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME
  );

  private static final Map<Type, Map.Entry<Type, UnaryOperator<Object>>> SERIALIZER_CONVERTERS;
  private static final Map<Type, Function<Object, DataTracker.SerializedEntry<?>>> META_ENTRY_FACTORY;

  static {
    // associate item slots with their respective minestom lib enum
    ITEM_SLOT_CONVERTER = new EnumMap<>(ItemSlot.class);
    ITEM_SLOT_CONVERTER.put(ItemSlot.MAIN_HAND, EquipmentSlot.MAINHAND);
    ITEM_SLOT_CONVERTER.put(ItemSlot.OFF_HAND, EquipmentSlot.OFFHAND);
    ITEM_SLOT_CONVERTER.put(ItemSlot.FEET, EquipmentSlot.FEET);
    ITEM_SLOT_CONVERTER.put(ItemSlot.LEGS, EquipmentSlot.LEGS);
    ITEM_SLOT_CONVERTER.put(ItemSlot.CHEST, EquipmentSlot.CHEST);
    ITEM_SLOT_CONVERTER.put(ItemSlot.HEAD, EquipmentSlot.HEAD);

    // associate entity poses with their respective minestom lib enum
    ENTITY_POSE_CONVERTER = new EnumMap<>(EntityPose.class);
    ENTITY_POSE_CONVERTER.put(EntityPose.STANDING, net.minecraft.entity.EntityPose.STANDING);
    ENTITY_POSE_CONVERTER.put(EntityPose.FALL_FLYING, net.minecraft.entity.EntityPose.FALL_FLYING);
    ENTITY_POSE_CONVERTER.put(EntityPose.SLEEPING, net.minecraft.entity.EntityPose.SLEEPING);
    ENTITY_POSE_CONVERTER.put(EntityPose.SWIMMING, net.minecraft.entity.EntityPose.SWIMMING);
    ENTITY_POSE_CONVERTER.put(EntityPose.SPIN_ATTACK, net.minecraft.entity.EntityPose.SPIN_ATTACK);
    ENTITY_POSE_CONVERTER.put(EntityPose.CROUCHING, net.minecraft.entity.EntityPose.CROUCHING);
    ENTITY_POSE_CONVERTER.put(EntityPose.LONG_JUMPING, net.minecraft.entity.EntityPose.LONG_JUMPING);
    ENTITY_POSE_CONVERTER.put(EntityPose.DYING, net.minecraft.entity.EntityPose.DYING);
    ENTITY_POSE_CONVERTER.put(EntityPose.CROAKING, net.minecraft.entity.EntityPose.CROAKING);
    ENTITY_POSE_CONVERTER.put(EntityPose.USING_TONGUE, net.minecraft.entity.EntityPose.USING_TONGUE);
    ENTITY_POSE_CONVERTER.put(EntityPose.ROARING, net.minecraft.entity.EntityPose.ROARING);
    ENTITY_POSE_CONVERTER.put(EntityPose.SNIFFING, net.minecraft.entity.EntityPose.SNIFFING);
    ENTITY_POSE_CONVERTER.put(EntityPose.EMERGING, net.minecraft.entity.EntityPose.EMERGING);
    ENTITY_POSE_CONVERTER.put(EntityPose.DIGGING, net.minecraft.entity.EntityPose.DIGGING);
    ENTITY_POSE_CONVERTER.put(EntityPose.SLIDING, net.minecraft.entity.EntityPose.SLIDING);
    ENTITY_POSE_CONVERTER.put(EntityPose.SHOOTING, net.minecraft.entity.EntityPose.SHOOTING);
    ENTITY_POSE_CONVERTER.put(EntityPose.INHALING, net.minecraft.entity.EntityPose.INHALING);

    // associate hands with their respective minestom lib enum
    HAND_CONVERTER = new EnumMap<>(Hand.class);
    HAND_CONVERTER.put(Hand.MAIN_HAND, InteractNpcEvent.Hand.MAIN_HAND);
    HAND_CONVERTER.put(Hand.OFF_HAND, InteractNpcEvent.Hand.OFF_HAND);

    // init the meta value converter
    SERIALIZER_CONVERTERS = new HashMap<>(1);
    //noinspection SuspiciousMethodCalls
    SERIALIZER_CONVERTERS.put(EntityPose.class, new AbstractMap.SimpleImmutableEntry<>(
      net.minecraft.entity.EntityPose.class,
      ENTITY_POSE_CONVERTER::get));
    //TODO kyori?
    /*SERIALIZER_CONVERTERS.put(
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
      ));*/

    // init the meta entry factories
    var playerClassId = DataTrackerAccessor.getClassToLastId().get(PlayerEntity.class);
    META_ENTRY_FACTORY = new HashMap<>(6);
    META_ENTRY_FACTORY.put(byte.class,
      value -> DataTracker.SerializedEntry.of(TrackedDataHandlerRegistry.BYTE.create(playerClassId), (byte) value));
    META_ENTRY_FACTORY.put(int.class,
      value -> DataTracker.SerializedEntry.of(TrackedDataHandlerRegistry.INTEGER.create(playerClassId), (int) value));
    META_ENTRY_FACTORY.put(float.class,
      value -> DataTracker.SerializedEntry.of(TrackedDataHandlerRegistry.FLOAT.create(playerClassId), (float) value));
    META_ENTRY_FACTORY.put(boolean.class,
      value -> DataTracker.SerializedEntry.of(TrackedDataHandlerRegistry.BOOLEAN.create(playerClassId),
        (boolean) value));
    META_ENTRY_FACTORY.put(String.class,
      value -> DataTracker.SerializedEntry.of(TrackedDataHandlerRegistry.STRING.create(playerClassId), (String) value));
    META_ENTRY_FACTORY.put(
      net.minecraft.entity.EntityPose.class,
      value -> DataTracker.SerializedEntry.of(TrackedDataHandlerRegistry.ENTITY_POSE.create(playerClassId),
        (net.minecraft.entity.EntityPose) value));
  }

  @Override
  public @NotNull OutboundPacket<World, ServerPlayerEntity, ItemStack, Object> createEntitySpawnPacket() {
    return (player, npc) -> {
      EntitySpawnS2CPacket packet = new EntitySpawnS2CPacket(
        npc.entityId(),
        npc.profile().uniqueId(),
        npc.position().x(),
        npc.position().y(),
        npc.position().z(),
        npc.position().yaw(),
        npc.position().pitch(),
        EntityType.PLAYER,
        0,
        Vec3d.ZERO,
        npc.position().yaw()
      );
      player.networkHandler.sendPacket(packet);
    };
  }

  @Override
  public @NotNull OutboundPacket<World, ServerPlayerEntity, ItemStack, Object> createEntityRemovePacket() {
    return (player, npc) -> {
      EntitiesDestroyS2CPacket packet = new EntitiesDestroyS2CPacket(npc.entityId());
      player.networkHandler.sendPacket(packet);
    };
  }

  @Override
  public @NotNull OutboundPacket<World, ServerPlayerEntity, ItemStack, Object> createPlayerInfoPacket(
    @NotNull PlayerInfoAction action) {
    return (player, npc) -> npc.settings().profileResolver().resolveNpcProfile(player, npc).thenAcceptAsync(profile -> {
      if (action == PlayerInfoAction.REMOVE_PLAYER) {
        // just remove the player from the tablist
        PlayerRemoveS2CPacket removePacket = new PlayerRemoveS2CPacket(List.of(profile.uniqueId()));
        player.networkHandler.sendPacket(removePacket);
        return;
      }

      var gameProfile = new GameProfile(profile.uniqueId(), profile.name());
      // convert the profile properties
      for (ProfileProperty property : profile.properties()) {
        gameProfile.getProperties()
          .put(property.name(), new Property(property.name(), property.value(), property.signature()));
      }
      var packet = new PlayerListS2CPacket(ADD_ACTIONS, new ArrayList<>());
      ((PlayerListS2CPacketExt) packet).npclib_setEntries(
        List.of(new PlayerListS2CPacket.Entry(profile.uniqueId(),
          gameProfile,
          false,
          20,
          GameMode.CREATIVE,
          null,
          null)));

      player.networkHandler.sendPacket(packet);
    });
  }


  @Override
  public @NotNull OutboundPacket<World, ServerPlayerEntity, ItemStack, Object> createRotationPacket(float yaw,
    float pitch) {
    return (player, npc) -> {
      // head rotation (https://wiki.vg/Protocol#Entity_Head_Look) & rotation (https://wiki.vg/Protocol#Player_Rotation)
      // TODO yaw pitch richtig berechnen
      EntitySetHeadYawS2CPacket headLookPacket = new EntitySetHeadYawS2CPacket(player, (byte) 0);
      ((EntitySetHeadYawS2CPacketExt) headLookPacket).npclib_setEntityId(npc.entityId());
      EntityS2CPacket rotationPacket = new EntityS2CPacket.Rotate(npc.entityId(), (byte) 0, (byte) 0, true);
      player.networkHandler.sendPacket(headLookPacket);
      player.networkHandler.sendPacket(rotationPacket);
    };
  }

  @Override
  public @NotNull OutboundPacket<World, ServerPlayerEntity, ItemStack, Object> createAnimationPacket(
    @NotNull EntityAnimation animation) {
    return (player, npc) -> {
      var packet = new EntityAnimationS2CPacket(player, animation.id());
      //workaround
      ((EntityAnimationS2CPacketExt) packet).npclib_setEntityId(npc.entityId());
      player.networkHandler.sendPacket(packet);
    };
  }

  @Override
  public @NotNull OutboundPacket<World, ServerPlayerEntity, ItemStack, Object> createEquipmentPacket(
    @NotNull ItemSlot slot, @NotNull ItemStack item) {
    return (player, npc) -> {
      // get the meta of the item to send
      EquipmentSlot equipmentSlot = ITEM_SLOT_CONVERTER.get(slot);
      var list = Collections.singletonMap(equipmentSlot, item)
        .entrySet()
        .stream()
        .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
        .toList();

      // send the packet
      EntityEquipmentUpdateS2CPacket packet = new EntityEquipmentUpdateS2CPacket(npc.entityId(), list);
      player.networkHandler.sendPacket(packet);
    };
  }

  @Override
  public @NotNull OutboundPacket<World, ServerPlayerEntity, ItemStack, Object> createCustomPayloadPacket(
    @NotNull String channelId, byte[] payload) {
    return (player, npc) -> {
      //TODO?
      //PluginMessagePacket packet = new CustomPayloadS2CPacket(channelId, payload);
      //player.sendPacket(packet);
    };
  }

  @Override
  public @NotNull <T, O> OutboundPacket<World, ServerPlayerEntity, ItemStack, Object> createEntityMetaPacket(
    @NotNull EntityMetadataFactory<T, O> metadata, @NotNull T value) {
    return (player, npc) -> {
      // create the entity meta
      PlatformVersionAccessor versionAccessor = npc.platform().versionAccessor();
      EntityMetadata<O> entityMetadata = metadata.create(value, versionAccessor);

      // check if the meta is available
      if (!entityMetadata.available()) {
        return;
      }

      Map<Integer, DataTracker.SerializedEntry<?>> metadataEntries = new HashMap<>();
      metadataEntries.put(entityMetadata.index(), createMetadataEntry(entityMetadata.type(), entityMetadata.value()));

      // add all dependant metas
      for (EntityMetadataFactory<T, Object> relatedMetadata : metadata.relatedMetadata()) {
        EntityMetadata<Object> related = relatedMetadata.create(value, versionAccessor);
        if (related.available()) {
          metadataEntries.put(related.index(), createMetadataEntry(related.type(), related.value()));
        }
      }

      List<DataTracker.SerializedEntry<?>> sortedMetadataEntries = metadataEntries.entrySet().stream()
        .sorted(Map.Entry.comparingByKey()) // Sort by index (key)
        .map(Map.Entry::getValue)          // Extract the values
        .collect(Collectors.toList());    // Collect to a list

      var packet = new EntityTrackerUpdateS2CPacket(npc.entityId(), sortedMetadataEntries);
      player.networkHandler.sendPacket(packet);
    };
  }

  private static @NotNull DataTracker.SerializedEntry<?> createMetadataEntry(@NotNull Type type,
    @NotNull Object value) {
    // check if we need to convert the value before creating the meta object
    Map.Entry<Type, UnaryOperator<Object>> converter = SERIALIZER_CONVERTERS.get(type);
    if (converter != null) {
      type = converter.getKey();
      value = converter.getValue().apply(value);
    }

    // get the meta factory which is converting the type
    Function<Object, DataTracker.SerializedEntry<?>> metaFactory = META_ENTRY_FACTORY.get(type);
    if (metaFactory == null) {
      // unable to handle that
      throw new IllegalArgumentException("Unsupported type: " + type);
    }

    // create the meta entry
    return metaFactory.apply(value);
  }

  @Override
  public void initialize(@NotNull Platform<World, ServerPlayerEntity, ItemStack, Object> platform) {

  }
}
