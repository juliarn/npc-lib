/*
 * This file is part of npc-lib, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022-present Julian M., Pasqual K. and contributors
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
import com.github.juliarn.npclib.api.event.InteractNpcEvent;
import com.github.juliarn.npclib.api.profile.Profile;
import com.github.juliarn.npclib.api.profile.ProfileProperty;
import com.github.juliarn.npclib.api.protocol.OutboundPacket;
import com.github.juliarn.npclib.api.protocol.PlatformPacketAdapter;
import com.github.juliarn.npclib.api.protocol.enums.EntityAnimation;
import com.github.juliarn.npclib.api.protocol.enums.EntityPose;
import com.github.juliarn.npclib.api.protocol.enums.ItemSlot;
import com.github.juliarn.npclib.api.protocol.meta.EntityMetadataFactory;
import com.github.juliarn.npclib.common.event.DefaultAttackNpcEvent;
import com.github.juliarn.npclib.common.event.DefaultInteractNpcEvent;
import com.github.juliarn.npclib.fabric.controller.FabricActionControllerEvents;
import com.github.juliarn.npclib.fabric.util.FabricUtil;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import io.leangen.geantyref.TypeFactory;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class FabricProtocolAdapter
  implements PlatformPacketAdapter<ServerLevel, ServerPlayer, ItemStack, Object> {

  private static final FabricProtocolAdapter INSTANCE = new FabricProtocolAdapter();

  private static final Type OPTIONAL_CHAT_COMPONENT_TYPE = TypeFactory.parameterizedClass(
    Optional.class,
    net.minecraft.network.chat.Component.class);

  private static final EnumMap<EntityPose, Pose> ENTITY_POSE_CONVERTER;
  private static final EnumMap<ItemSlot, EquipmentSlot> ITEM_SLOT_CONVERTER;
  private static final EnumMap<InteractionHand, InteractNpcEvent.Hand> HAND_CONVERTER;

  private static final Map<Type, EntityDataFactory> META_ENTRY_FACTORY;
  private static final Map<Type, Map.Entry<Type, UnaryOperator<Object>>> SERIALIZER_CONVERTERS;

  private static final EnumSet<ClientboundPlayerInfoUpdatePacket.Action> ADD_ACTIONS = EnumSet.of(
    ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_HAT,
    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME
  );

  static {
    ITEM_SLOT_CONVERTER = new EnumMap<>(ItemSlot.class);
    ITEM_SLOT_CONVERTER.put(ItemSlot.MAIN_HAND, EquipmentSlot.MAINHAND);
    ITEM_SLOT_CONVERTER.put(ItemSlot.OFF_HAND, EquipmentSlot.OFFHAND);
    ITEM_SLOT_CONVERTER.put(ItemSlot.FEET, EquipmentSlot.FEET);
    ITEM_SLOT_CONVERTER.put(ItemSlot.LEGS, EquipmentSlot.LEGS);
    ITEM_SLOT_CONVERTER.put(ItemSlot.CHEST, EquipmentSlot.CHEST);
    ITEM_SLOT_CONVERTER.put(ItemSlot.HEAD, EquipmentSlot.HEAD);

    ENTITY_POSE_CONVERTER = new EnumMap<>(EntityPose.class);
    ENTITY_POSE_CONVERTER.put(EntityPose.STANDING, Pose.STANDING);
    ENTITY_POSE_CONVERTER.put(EntityPose.FALL_FLYING, Pose.FALL_FLYING);
    ENTITY_POSE_CONVERTER.put(EntityPose.SLEEPING, Pose.SLEEPING);
    ENTITY_POSE_CONVERTER.put(EntityPose.SWIMMING, Pose.SWIMMING);
    ENTITY_POSE_CONVERTER.put(EntityPose.SPIN_ATTACK, Pose.SPIN_ATTACK);
    ENTITY_POSE_CONVERTER.put(EntityPose.CROUCHING, Pose.CROUCHING);
    ENTITY_POSE_CONVERTER.put(EntityPose.LONG_JUMPING, Pose.LONG_JUMPING);
    ENTITY_POSE_CONVERTER.put(EntityPose.DYING, Pose.DYING);
    ENTITY_POSE_CONVERTER.put(EntityPose.CROAKING, Pose.CROAKING);
    ENTITY_POSE_CONVERTER.put(EntityPose.USING_TONGUE, Pose.USING_TONGUE);
    ENTITY_POSE_CONVERTER.put(EntityPose.ROARING, Pose.ROARING);
    ENTITY_POSE_CONVERTER.put(EntityPose.SNIFFING, Pose.SNIFFING);
    ENTITY_POSE_CONVERTER.put(EntityPose.EMERGING, Pose.EMERGING);
    ENTITY_POSE_CONVERTER.put(EntityPose.DIGGING, Pose.DIGGING);
    ENTITY_POSE_CONVERTER.put(EntityPose.SLIDING, Pose.SLIDING);
    ENTITY_POSE_CONVERTER.put(EntityPose.SHOOTING, Pose.SHOOTING);
    ENTITY_POSE_CONVERTER.put(EntityPose.INHALING, Pose.INHALING);

    HAND_CONVERTER = new EnumMap<>(InteractionHand.class);
    HAND_CONVERTER.put(InteractionHand.MAIN_HAND, InteractNpcEvent.Hand.MAIN_HAND);
    HAND_CONVERTER.put(InteractionHand.OFF_HAND, InteractNpcEvent.Hand.OFF_HAND);

    SERIALIZER_CONVERTERS = HashMap.newHashMap(2);
    //noinspection SuspiciousMethodCalls
    SERIALIZER_CONVERTERS.put(EntityPose.class, Map.entry(Pose.class, ENTITY_POSE_CONVERTER::get));
    SERIALIZER_CONVERTERS.put(
      TypeFactory.parameterizedClass(Optional.class, com.github.juliarn.npclib.api.protocol.chat.Component.class),
      Map.entry(
        OPTIONAL_CHAT_COMPONENT_TYPE,
        value -> {
          //noinspection unchecked
          var optionalComponent = (Optional<com.github.juliarn.npclib.api.protocol.chat.Component>) value;
          return optionalComponent.map(component -> {
            // build the component based on the given input
            String rawMessage = component.rawMessage();
            if (rawMessage != null) {
              return Component.literal(rawMessage);
            } else {
              var encodedJson = Objects.requireNonNull(component.encodedJsonMessage());
              var encodedJsonTree = JsonParser.parseString(encodedJson);
              var context = FabricUtil.getServer().registryAccess().createSerializationContext(JsonOps.INSTANCE);
              return ComponentSerialization.CODEC
                .decode(context, encodedJsonTree)
                .getOrThrow(IllegalArgumentException::new)
                .getFirst();
            }
          });
        }
      ));

    META_ENTRY_FACTORY = HashMap.newHashMap(7);
    META_ENTRY_FACTORY.put(Byte.class, (index, value) -> new SynchedEntityData.DataValue<>(
      index,
      EntityDataSerializers.BYTE,
      (byte) value));
    META_ENTRY_FACTORY.put(Integer.class, (index, value) -> new SynchedEntityData.DataValue<>(
      index,
      EntityDataSerializers.INT,
      (int) value));
    META_ENTRY_FACTORY.put(Float.class, (index, value) -> new SynchedEntityData.DataValue<>(
      index,
      EntityDataSerializers.FLOAT,
      (float) value));
    META_ENTRY_FACTORY.put(Boolean.class, (index, value) -> new SynchedEntityData.DataValue<>(
      index,
      EntityDataSerializers.BOOLEAN,
      (boolean) value));
    META_ENTRY_FACTORY.put(String.class, (index, value) -> new SynchedEntityData.DataValue<>(
      index,
      EntityDataSerializers.STRING,
      (String) value));
    META_ENTRY_FACTORY.put(Pose.class, (index, value) -> new SynchedEntityData.DataValue<>(
      index,
      EntityDataSerializers.POSE,
      (Pose) value));
    //noinspection unchecked
    META_ENTRY_FACTORY.put(OPTIONAL_CHAT_COMPONENT_TYPE, (index, value) -> new SynchedEntityData.DataValue<>(
      index,
      EntityDataSerializers.OPTIONAL_COMPONENT,
      (Optional<Component>) value));
  }

  private FabricProtocolAdapter() {
  }

  public static @NotNull PlatformPacketAdapter<ServerLevel, ServerPlayer, ItemStack, Object> fabricProtocolAdapter() {
    return INSTANCE;
  }

  private static @NotNull SynchedEntityData.DataValue<?> createDataValue(
    @NotNull Type type,
    @NotNull Object value,
    int index
  ) {
    // check if we need to convert the value before creating the meta object
    var converter = SERIALIZER_CONVERTERS.get(type);
    if (converter != null) {
      type = converter.getKey();
      value = converter.getValue().apply(value);
    }

    // get the meta factory which is converting the type
    var metaFactory = META_ENTRY_FACTORY.get(type);
    if (metaFactory == null) {
      // unable to handle that
      throw new IllegalArgumentException("Unsupported type: " + type);
    }

    // create the meta entry
    return metaFactory.create(index, value);
  }

  @Override
  public @NotNull OutboundPacket<ServerLevel, ServerPlayer, ItemStack, Object> createEntitySpawnPacket() {
    return (player, npc) -> {
      var pos = npc.position();
      var packet = new ClientboundAddEntityPacket(
        npc.entityId(),
        npc.profile().uniqueId(),
        pos.x(),
        pos.y(),
        pos.z(),
        pos.pitch(),
        pos.yaw(),
        EntityType.PLAYER,
        0,
        Vec3.ZERO,
        pos.yaw());
      player.connection.send(packet);
    };
  }

  @Override
  public @NotNull OutboundPacket<ServerLevel, ServerPlayer, ItemStack, Object> createEntityRemovePacket() {
    return (player, npc) -> {
      var packet = new ClientboundRemoveEntitiesPacket(npc.entityId());
      player.connection.send(packet);
    };
  }

  @Override
  public @NotNull OutboundPacket<ServerLevel, ServerPlayer, ItemStack, Object> createPlayerInfoRemovePacket() {
    return (player, npc) -> {
      var packet = new ClientboundPlayerInfoRemovePacket(List.of(npc.profile().uniqueId()));
      player.connection.send(packet);
    };
  }

  @Override
  public @NotNull OutboundPacket<ServerLevel, ServerPlayer, ItemStack, Object> createPlayerInfoAddPacket(
    @NotNull Profile.Resolved profile
  ) {
    return (player, npc) -> {
      var gameProfileProperties = profile.properties().stream().collect(Collectors.collectingAndThen(
        Multimaps.toMultimap(
          ProfileProperty::name,
          pp -> new Property(pp.name(), pp.value(), pp.signature()),
          HashMultimap::create),
        PropertyMap::new));
      var gameProfile = new GameProfile(profile.uniqueId(), profile.name(), gameProfileProperties);

      var updatePacket = new ClientboundPlayerInfoUpdatePacket(ADD_ACTIONS, List.of());
      updatePacket.entries = List.of(new ClientboundPlayerInfoUpdatePacket.Entry(
        profile.uniqueId(),
        gameProfile,
        false,
        20,
        GameType.CREATIVE,
        null,
        true,
        0,
        null));
      player.connection.send(updatePacket);
    };
  }

  @Override
  public @NotNull OutboundPacket<ServerLevel, ServerPlayer, ItemStack, Object> createRotationPacket(
    float yaw,
    float pitch
  ) {
    return (player, npc) -> {
      var headLookPacket = new ClientboundRotateHeadPacket(player, Mth.packDegrees(yaw));
      headLookPacket.entityId = npc.entityId();
      player.connection.send(headLookPacket);

      var rotationPacket = new ClientboundMoveEntityPacket.Rot(
        npc.entityId(),
        Mth.packDegrees(yaw),
        Mth.packDegrees(pitch),
        true);
      player.connection.send(rotationPacket);
    };
  }

  @Override
  public @NotNull OutboundPacket<ServerLevel, ServerPlayer, ItemStack, Object> createAnimationPacket(
    @NotNull EntityAnimation animation
  ) {
    return (player, npc) -> {
      var packet = new ClientboundAnimatePacket(player, animation.id());
      packet.id = npc.entityId();
      player.connection.send(packet);
    };
  }

  @Override
  public @NotNull OutboundPacket<ServerLevel, ServerPlayer, ItemStack, Object> createEquipmentPacket(
    @NotNull ItemSlot slot,
    @NotNull ItemStack item
  ) {
    return (player, npc) -> {
      var equipmentSlot = ITEM_SLOT_CONVERTER.get(slot);
      var items = List.of(Pair.of(equipmentSlot, item));
      var packet = new ClientboundSetEquipmentPacket(npc.entityId(), items);
      player.connection.send(packet);
    };
  }

  @Override
  @SuppressWarnings("UnstableApiUsage")
  public @NotNull OutboundPacket<ServerLevel, ServerPlayer, ItemStack, Object> createCustomPayloadPacket(
    @NotNull String channelId,
    byte[] payload
  ) {
    return (player, npc) -> {
      // construct the custom payload to send
      var channelLocation = Identifier.parse(channelId);
      var payloadType = new CustomPacketPayload.Type<ByteArrayCustomPayload>(channelLocation);
      var customPayload = new ByteArrayCustomPayload(payloadType, payload);

      // ensure that the payload codec is registered for the payload type
      var payloadTypeRegistry = PayloadTypeRegistryImpl.CLIENTBOUND_PLAY;
      var registered = payloadTypeRegistry.get(channelLocation);
      if (registered == null) {
        payloadTypeRegistry.register(payloadType, ByteArrayCustomPayload.CODEC);
      }

      ServerPlayNetworking.send(player, customPayload);
    };
  }

  @Override
  public @NotNull <T, O> OutboundPacket<ServerLevel, ServerPlayer, ItemStack, Object> createEntityMetaPacket(
    @NotNull EntityMetadataFactory<T, O> metadata,
    @NotNull T value
  ) {
    return (player, npc) -> {
      // create the root entity metadata and check for the availability in the current version
      var versionAccessor = npc.platform().versionAccessor();
      var entityMetadata = metadata.create(value, versionAccessor);
      if (!entityMetadata.available()) {
        return;
      }

      // register the root entity metadata
      var entries = new ArrayList<SynchedEntityData.DataValue<?>>();
      entries.add(createDataValue(entityMetadata.type(), entityMetadata.value(), entityMetadata.index()));

      // add all dependant metas
      for (var relatedMetadata : metadata.relatedMetadata()) {
        var related = relatedMetadata.create(value, versionAccessor);
        if (related.available()) {
          entries.add(createDataValue(related.type(), related.value(), related.index()));
        }
      }

      // create & send the packet
      var packet = new ClientboundSetEntityDataPacket(npc.entityId(), entries);
      player.connection.send(packet);
    };
  }

  @Override
  public void initialize(@NotNull Platform<ServerLevel, ServerPlayer, ItemStack, Object> platform) {
    FabricActionControllerEvents.SERVER_PLAYER_ENTITY_INTERACT.register((entityId, player, actionType, hand) -> {
      var npc = platform.npcTracker().npcById(entityId);
      if (npc != null) {
        switch (actionType) {
          case ATTACK -> platform.eventManager().post(DefaultAttackNpcEvent.attackNpc(npc, player));
          case INTERACT -> {
            var convertedHand = HAND_CONVERTER.get(hand);
            platform.eventManager().post(DefaultInteractNpcEvent.interactNpc(npc, player, convertedHand));
          }
        }
        return true;
      }

      return false;
    });
  }
}
