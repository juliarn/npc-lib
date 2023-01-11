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

package com.github.juliarn.npclib.bukkit.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.MinecraftKey;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedEnumEntityUseAction;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
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
import com.github.juliarn.npclib.common.util.EventDispatcher;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ProtocolLibPacketAdapter implements PlatformPacketAdapter<World, Player, ItemStack, Plugin> {

  static final ProtocolLibPacketAdapter INSTANCE = new ProtocolLibPacketAdapter();

  private static final Type OPTIONAL_COMPONENT_TYPE = TypeToken.getParameterized(
    Optional.class,
    MinecraftReflection.getIChatBaseComponentClass()
  ).getType();

  private static final ProtocolManager PROTOCOL_MANAGER = ProtocolLibrary.getProtocolManager();
  private static final MinecraftVersion SERVER_VERSION = MinecraftVersion.fromServerVersion(Bukkit.getVersion());

  private static final EnumMap<EntityPose, Object> ENTITY_POSE_CONVERTER;
  private static final EnumMap<ItemSlot, EnumWrappers.ItemSlot> ITEM_SLOT_CONVERTER;
  private static final EnumMap<EnumWrappers.Hand, InteractNpcEvent.Hand> HAND_CONVERTER;
  private static final EnumMap<PlayerInfoAction, EnumWrappers.PlayerInfoAction> PLAYER_INFO_ACTION_CONVERTER;

  // serializer converters for metadata
  private static final Map<Type, BiFunction<PlatformVersionAccessor, Object, Map.Entry<Type, Object>>> SERIALIZER_CONVERTERS;

  // static actions we need to send out for all player updates (since 1.19.3)
  private static final EnumSet<EnumWrappers.PlayerInfoAction> ADD_ACTIONS = EnumSet.of(
    EnumWrappers.PlayerInfoAction.ADD_PLAYER,
    EnumWrappers.PlayerInfoAction.UPDATE_LISTED,
    EnumWrappers.PlayerInfoAction.UPDATE_LATENCY,
    EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE,
    EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME);

  static {
    // associate item slots with their respective protocol lib enum
    ITEM_SLOT_CONVERTER = new EnumMap<>(ItemSlot.class);
    ITEM_SLOT_CONVERTER.put(ItemSlot.MAIN_HAND, EnumWrappers.ItemSlot.MAINHAND);
    ITEM_SLOT_CONVERTER.put(ItemSlot.OFF_HAND, EnumWrappers.ItemSlot.OFFHAND);
    ITEM_SLOT_CONVERTER.put(ItemSlot.FEET, EnumWrappers.ItemSlot.FEET);
    ITEM_SLOT_CONVERTER.put(ItemSlot.LEGS, EnumWrappers.ItemSlot.LEGS);
    ITEM_SLOT_CONVERTER.put(ItemSlot.CHEST, EnumWrappers.ItemSlot.CHEST);
    ITEM_SLOT_CONVERTER.put(ItemSlot.HEAD, EnumWrappers.ItemSlot.HEAD);

    // associate hands with their respective protocol lib enum
    HAND_CONVERTER = new EnumMap<>(EnumWrappers.Hand.class);
    HAND_CONVERTER.put(EnumWrappers.Hand.MAIN_HAND, InteractNpcEvent.Hand.MAIN_HAND);
    HAND_CONVERTER.put(EnumWrappers.Hand.OFF_HAND, InteractNpcEvent.Hand.OFF_HAND);

    // associate entity poses with their respective protocol lib enum (if possible, only present on 1.13+)
    ENTITY_POSE_CONVERTER = new EnumMap<>(EntityPose.class);
    if (EnumWrappers.getEntityPoseClass() != null) {
      // always present since 1.13
      ENTITY_POSE_CONVERTER.put(EntityPose.STANDING, EnumWrappers.EntityPose.STANDING.toNms());
      ENTITY_POSE_CONVERTER.put(EntityPose.FALL_FLYING, EnumWrappers.EntityPose.FALL_FLYING.toNms());
      ENTITY_POSE_CONVERTER.put(EntityPose.SLEEPING, EnumWrappers.EntityPose.SLEEPING.toNms());
      ENTITY_POSE_CONVERTER.put(EntityPose.SWIMMING, EnumWrappers.EntityPose.SWIMMING.toNms());
      ENTITY_POSE_CONVERTER.put(EntityPose.SPIN_ATTACK, EnumWrappers.EntityPose.SPIN_ATTACK.toNms());
      ENTITY_POSE_CONVERTER.put(EntityPose.CROUCHING, EnumWrappers.EntityPose.CROUCHING.toNms());
      ENTITY_POSE_CONVERTER.put(EntityPose.DYING, EnumWrappers.EntityPose.DYING.toNms());
      // poses of newer mc versions
      // 1.17+
      if (MinecraftVersion.CAVES_CLIFFS_1.atOrAbove()) {
        ENTITY_POSE_CONVERTER.put(EntityPose.LONG_JUMPING, EnumWrappers.EntityPose.LONG_JUMPING.toNms());
      }
      // 1.19+
      if (MinecraftVersion.WILD_UPDATE.atOrAbove()) {
        ENTITY_POSE_CONVERTER.put(EntityPose.CROAKING, EnumWrappers.EntityPose.CROAKING.toNms());
        ENTITY_POSE_CONVERTER.put(EntityPose.USING_TONGUE, EnumWrappers.EntityPose.USING_TONGUE.toNms());
        ENTITY_POSE_CONVERTER.put(EntityPose.ROARING, EnumWrappers.EntityPose.ROARING.toNms());
        ENTITY_POSE_CONVERTER.put(EntityPose.SNIFFING, EnumWrappers.EntityPose.SNIFFING.toNms());
        ENTITY_POSE_CONVERTER.put(EntityPose.EMERGING, EnumWrappers.EntityPose.EMERGING.toNms());
        ENTITY_POSE_CONVERTER.put(EntityPose.DIGGING, EnumWrappers.EntityPose.DIGGING.toNms());
      }
      // 1.19.3+
      if (MinecraftVersion.FEATURE_PREVIEW_UPDATE.atOrAbove()) {
        ENTITY_POSE_CONVERTER.put(EntityPose.SITTING, EnumWrappers.EntityPose.SITTING.toNms());
      }
    }

    // associate player info actions with their respective protocol lib enum
    PLAYER_INFO_ACTION_CONVERTER = new EnumMap<>(PlayerInfoAction.class);
    PLAYER_INFO_ACTION_CONVERTER.put(PlayerInfoAction.ADD_PLAYER, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
    PLAYER_INFO_ACTION_CONVERTER.put(PlayerInfoAction.REMOVE_PLAYER, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);

    // meta serializers
    //noinspection SuspiciousMethodCalls
    SERIALIZER_CONVERTERS = ImmutableMap.<Type, BiFunction<PlatformVersionAccessor, Object, Map.Entry<Type, Object>>>builder()
      .put(
        EntityPose.class,
        ($, value) -> new AbstractMap.SimpleEntry<>(
          EnumWrappers.getEntityPoseClass(),
          ENTITY_POSE_CONVERTER.get(value)))
      .put(
        TypeToken.getParameterized(Optional.class, Component.class).getType(),
        (versionAccess, value) -> {
          //noinspection unchecked
          Optional<Component> optionalComponent = (Optional<Component>) value;
          // check if the display name is wrapped in a component
          if (versionAccess.atLeast(1, 13, 0)) {
            // construct the entry
            return new AbstractMap.SimpleImmutableEntry<>(
              OPTIONAL_COMPONENT_TYPE,
              optionalComponent.map(component -> {
                // build the component based on the given input
                if (component.rawMessage() != null) {
                  return WrappedChatComponent.fromLegacyText(component.rawMessage());
                } else {
                  return WrappedChatComponent.fromJson(component.encodedJsonMessage());
                }
              }).map(WrappedChatComponent::getHandle));
          } else {
            return new AbstractMap.SimpleImmutableEntry<>(String.class, optionalComponent
              .map(component -> Objects.requireNonNull(
                component.rawMessage(),
                "Versions older than 1.13 don't support json component"))
              .orElse(null));
          }
        })
      .build();
  }

  private static @Nullable WrappedWatchableObject createWatchableObject(
    int index,
    @NotNull Type type,
    @NotNull Object value,
    @NotNull PlatformVersionAccessor versionAccessor
  ) {
    // pre-convert the value if needed
    BiFunction<PlatformVersionAccessor, Object, Map.Entry<Type, Object>> converter = SERIALIZER_CONVERTERS.get(type);
    if (converter != null) {
      Map.Entry<Type, Object> converted = converter.apply(versionAccessor, value);
      // re-assign the type and value
      type = converted.getKey();
      value = converted.getValue();

      // the value might not be accessible on the current version
      if (value == null) {
        return null;
      }
    }

    if (MinecraftVersion.COMBAT_UPDATE.atOrAbove()) {
      // mc 1.9: watchable object now contains a serializer for the type
      WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(
        ProtocolUtil.extractRawType(type),
        type instanceof ParameterizedType);
      // create the watchable object
      return new WrappedWatchableObject(new WrappedDataWatcher.WrappedDataWatcherObject(index, serializer), value);
    } else {
      // mc 1.8: watchable object id
      return new WrappedWatchableObject(index, value);
    }
  }

  @Override
  public @NotNull OutboundPacket<World, Player, ItemStack, Plugin> createEntitySpawnPacket() {
    return (player, npc) -> {
      // SpawnPlayer (https://wiki.vg/Protocol#Spawn_Player)
      PacketContainer container = new PacketContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN);

      // base information
      container.getIntegers().write(0, npc.entityId());
      container.getUUIDs().write(0, npc.profile().uniqueId());

      // position
      if (MinecraftVersion.COMBAT_UPDATE.atOrAbove()) {
        // mc 1.9: new position format (plain doubles)
        container.getDoubles()
          .write(0, npc.position().x())
          .write(1, npc.position().y())
          .write(2, npc.position().z());
      } else {
        // mc 1.8: old position format (rotation angles)
        container.getIntegers()
          .write(1, (int) Math.floor(npc.position().x() * 32.0D))
          .write(2, (int) Math.floor(npc.position().y() * 32.0D))
          .write(3, (int) Math.floor(npc.position().z() * 32.0D));
      }

      // rotation (angles)
      container.getBytes()
        .write(0, (byte) (npc.position().yaw() * 256F / 360F))
        .write(1, (byte) (npc.position().pitch() * 256F / 360F));

      // metadata if on an old server version (< 15)
      if (MinecraftVersion.VILLAGE_UPDATE.isAtLeast(SERVER_VERSION)) {
        container.getDataWatcherModifier().write(0, new WrappedDataWatcher());
      }

      // send the packet without notifying any bound packet listeners
      PROTOCOL_MANAGER.sendServerPacket(player, container, false);
    };
  }

  @Override
  public @NotNull OutboundPacket<World, Player, ItemStack, Plugin> createEntityRemovePacket() {
    return (player, npc) -> {
      // DestroyEntities (https://wiki.vg/Protocol#Destroy_Entities)
      PacketContainer container = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);

      // entity id
      if (MinecraftVersion.CAVES_CLIFFS_1.atOrAbove()) {
        // mc 1.17: entity ids is a list
        container.getIntLists().write(0, Lists.newArrayList(npc.entityId()));
      } else {
        // mc 1.8: entity ids is an int array
        container.getIntegerArrays().write(0, new int[]{npc.entityId()});
      }

      // send the packet without notifying any bound packet listeners
      PROTOCOL_MANAGER.sendServerPacket(player, container, false);
    };
  }

  @Override
  public @NotNull OutboundPacket<World, Player, ItemStack, Plugin> createPlayerInfoPacket(
    @NotNull PlayerInfoAction action
  ) {
    return (player, npc) -> npc.settings().profileResolver().resolveNpcProfile(player, npc).thenAcceptAsync(profile -> {
      // since 1.19.3 removing of players is handled in a separate packet
      if (action == PlayerInfoAction.REMOVE_PLAYER && MinecraftVersion.FEATURE_PREVIEW_UPDATE.atOrAbove()) {
        // PlayerRemove (https://wiki.vg/Protocol#Player_Remove)
        PacketContainer container = new PacketContainer(PacketType.Play.Server.PLAYER_INFO_REMOVE);

        // write the npc uuid to remove
        List<UUID> uuidsToRemove = Collections.singletonList(profile.uniqueId());
        container.getUUIDLists().write(0, uuidsToRemove);

        // send the packet without notifying any bound packet listeners
        PROTOCOL_MANAGER.sendServerPacket(player, container, false);
        return;
      }

      // PlayerInfo (https://wiki.vg/Protocol#Player_Info)
      PacketContainer container = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);

      // action
      int playerInfoDataIndex = 0;
      if (MinecraftVersion.FEATURE_PREVIEW_UPDATE.atOrAbove()) {
        // at this point the only way this could be called is because we want to register a new player
        playerInfoDataIndex = 1;
        container.getPlayerInfoActions().write(0, ADD_ACTIONS);
      } else {
        // old system, just add the translated action
        EnumWrappers.PlayerInfoAction playerInfoAction = PLAYER_INFO_ACTION_CONVERTER.get(action);
        container.getPlayerInfoAction().write(0, playerInfoAction);
      }

      // convert to a protocol lib profile
      WrappedGameProfile wrappedGameProfile = new WrappedGameProfile(profile.uniqueId(), profile.name());
      for (ProfileProperty prop : profile.properties()) {
        WrappedSignedProperty wrapped = new WrappedSignedProperty(prop.name(), prop.value(), prop.signature());
        wrappedGameProfile.getProperties().put(prop.name(), wrapped);
      }

      // add the player info data
      PlayerInfoData playerInfoData = new PlayerInfoData(
        profile.uniqueId(),
        20,
        false,
        EnumWrappers.NativeGameMode.CREATIVE,
        wrappedGameProfile,
        null,
        null);
      container.getPlayerInfoDataLists().write(playerInfoDataIndex, Lists.newArrayList(playerInfoData));

      // send the packet without notifying any bound packet listeners
      PROTOCOL_MANAGER.sendServerPacket(player, container, false);
    });
  }

  @Override
  public @NotNull OutboundPacket<World, Player, ItemStack, Plugin> createRotationPacket(float yaw, float pitch) {
    return (player, npc) -> {
      // pre-calculate the yaw and pitch angle values
      byte yawAngle = (byte) (yaw * 256F / 360F);
      byte pitchAngle = (byte) (pitch * 256F / 360F);

      // head rotation (https://wiki.vg/Protocol#Entity_Head_Look)
      PacketContainer headRotation = new PacketContainer(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
      headRotation.getBytes().write(0, yawAngle);
      headRotation.getIntegers().write(0, npc.entityId());

      // entity teleport (https://wiki.vg/Protocol#Entity_Teleport) or Player Rotation (https://wiki.vg/Protocol#Player_Rotation)
      PacketContainer rotation;
      if (MinecraftVersion.COMBAT_UPDATE.atOrAbove()) {
        // mc 1.9: player rotation
        rotation = new PacketContainer(PacketType.Play.Server.ENTITY_LOOK);
      } else {
        // mc 1.8: entity teleport
        rotation = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
        rotation.getIntegers()
          .write(1, (int) Math.floor(npc.position().x() * 32.0D))
          .write(2, (int) Math.floor(npc.position().y() * 32.0D))
          .write(3, (int) Math.floor(npc.position().z() * 32.0D));
      }

      // entity id
      rotation.getIntegers().write(0, npc.entityId());

      // rotation (angles)
      rotation.getBytes()
        .write(0, yawAngle)
        .write(1, pitchAngle);

      // ground status
      rotation.getBooleans().write(0, true);

      // send the packet without notifying any bound packet listeners
      PROTOCOL_MANAGER.sendServerPacket(player, rotation, false);
      PROTOCOL_MANAGER.sendServerPacket(player, headRotation, false);
    };
  }

  @Override
  public @NotNull OutboundPacket<World, Player, ItemStack, Plugin> createAnimationPacket(
    @NotNull EntityAnimation animation
  ) {
    return (player, npc) -> {
      // EntityAnimation (https://wiki.vg/Protocol#Entity_Animation_.28clientbound.29)
      PacketContainer container = new PacketContainer(PacketType.Play.Server.ANIMATION);

      // entity id & animation id
      container.getIntegers()
        .write(0, npc.entityId())
        .write(1, animation.id());

      // send the packet without notifying any bound packet listeners
      PROTOCOL_MANAGER.sendServerPacket(player, container, false);
    };
  }

  @Override
  public @NotNull OutboundPacket<World, Player, ItemStack, Plugin> createEquipmentPacket(
    @NotNull ItemSlot slot,
    @NotNull ItemStack item
  ) {
    return (player, npc) -> {
      // EntityEquipment (https://wiki.vg/Protocol#Entity_Equipment)
      PacketContainer container = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);

      // entity id
      container.getIntegers().write(0, npc.entityId());

      // item
      if (MinecraftVersion.NETHER_UPDATE.atOrAbove()) {
        // mc 1.16: item slot & item stack pairs
        EnumWrappers.ItemSlot itemSlot = ITEM_SLOT_CONVERTER.get(slot);
        container.getSlotStackPairLists().write(0, Lists.newArrayList(new Pair<>(itemSlot, item)));
      } else {
        if (MinecraftVersion.COMBAT_UPDATE.atOrAbove()) {
          // mc 1.9: item slot
          container.getItemSlots().write(0, ITEM_SLOT_CONVERTER.get(slot));
        } else {
          // mc 1.8: item slot id
          int slotId = slot.ordinal();
          if (slotId > 0) {
            // off-hand did not exist in 1.8, so all ids are shifted one down
            slotId -= 1;
          }

          container.getIntegers().write(1, slotId);
        }

        // the actual item
        container.getItemModifier().write(0, item);
      }

      // send the packet without notifying any bound packet listeners
      PROTOCOL_MANAGER.sendServerPacket(player, container, false);
    };
  }

  @Override
  public @NotNull OutboundPacket<World, Player, ItemStack, Plugin> createCustomPayloadPacket(
    @NotNull String channelId,
    byte[] payload
  ) {
    return (player, npc) -> {
      // CustomPayload (https://wiki.vg/Protocol#Custom_Payload)
      PacketContainer container = new PacketContainer(PacketType.Play.Server.CUSTOM_PAYLOAD);

      // channel id
      if (MinecraftVersion.AQUATIC_UPDATE.atOrAbove()) {
        // mc 1.13: channel id is now in the format of a resource location
        String[] parts = channelId.split(":", 2);
        MinecraftKey key = parts.length == 1 ? new MinecraftKey(channelId) : new MinecraftKey(parts[0], parts[1]);

        container.getMinecraftKeys().write(0, key);
      } else {
        // mc 1.8: channel id is a string
        container.getStrings().write(0, channelId);
      }

      // payload
      ByteBuf buffer = Unpooled.copiedBuffer(payload);
      Object wrappedSerializableBuffer = MinecraftReflection.getPacketDataSerializer(buffer);
      container.getModifier().withType(ByteBuf.class).write(0, wrappedSerializableBuffer);

      // send the packet without notifying any bound packet listeners
      PROTOCOL_MANAGER.sendServerPacket(player, container, false);
    };
  }

  @Override
  public @NotNull <T, O> OutboundPacket<World, Player, ItemStack, Plugin> createEntityMetaPacket(
    @NotNull T value,
    @NotNull EntityMetadataFactory<T, O> metadata
  ) {
    return (player, npc) -> {
      // create the entity meta
      PlatformVersionAccessor versionAcc = npc.platform().versionAccessor();
      EntityMetadata<O> entityMetadata = metadata.create(value, versionAcc);

      // check if the meta is available
      if (!entityMetadata.available()) {
        return;
      }

      // construct the meta we want to send out
      List<WrappedWatchableObject> watchableObjects = new ArrayList<>();
      watchableObjects.add(createWatchableObject(
        entityMetadata.index(),
        entityMetadata.type(),
        entityMetadata.value(),
        versionAcc));

      // add all dependant metas
      for (EntityMetadataFactory<T, Object> relatedMetadata : metadata.relatedMetadata()) {
        EntityMetadata<Object> related = relatedMetadata.create(value, versionAcc);
        if (related.available()) {
          // create & register the watchable object
          WrappedWatchableObject watchableObject = createWatchableObject(
            related.index(),
            related.type(),
            related.value(),
            versionAcc);
          if (watchableObject != null) {
            watchableObjects.add(watchableObject);
          }
        }
      }

      // EntityMetadata (https://wiki.vg/Protocol#Entity_Metadata)
      PacketContainer container = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);

      // entity id
      container.getIntegers().write(0, npc.entityId());

      // since 1.19.3 the metadata is wrapped in a specified object, we therefore need to convert all values
      if (MinecraftVersion.FEATURE_PREVIEW_UPDATE.atOrAbove()) {
        // convert the given values
        List<WrappedDataValue> wrappedDataValues = new ArrayList<>(watchableObjects.size());
        for (WrappedWatchableObject object : watchableObjects) {
          WrappedDataValue dataValue = new WrappedDataValue(
            object.getIndex(),
            object.getWatcherObject().getSerializer(),
            object.getRawValue());
          wrappedDataValues.add(dataValue);
        }

        // write the data values
        container.getDataValueCollectionModifier().write(0, wrappedDataValues);
      } else {
        // entity id & metadata
        container.getWatchableCollectionModifier().write(0, watchableObjects);
      }

      // send the packet without notifying any bound packet listeners
      PROTOCOL_MANAGER.sendServerPacket(player, container, false);
    };
  }

  @Override
  public void initialize(@NotNull Platform<World, Player, ItemStack, Plugin> platform) {
    PROTOCOL_MANAGER.addPacketListener(new NpcUsePacketAdapter(platform));
  }

  private static final class NpcUsePacketAdapter extends PacketAdapter {

    private final Platform<World, Player, ItemStack, Plugin> platform;

    public NpcUsePacketAdapter(@NotNull Platform<World, Player, ItemStack, Plugin> platform) {
      super(PacketAdapter.params(platform.extension(), PacketType.Play.Client.USE_ENTITY).optionAsync());
      this.platform = platform;
    }

    @Override
    public void onPacketReceiving(@NotNull PacketEvent event) {
      // get the entity id of the clicked entity
      Player player = event.getPlayer();
      PacketContainer packet = event.getPacket();
      int entityId = packet.getIntegers().read(0);

      // get the associated npc from the tracked entities
      Npc<World, Player, ItemStack, Plugin> npc = this.platform.npcTracker().npcById(entityId);
      if (npc != null) {
        // extract the used hand and interact action
        EnumWrappers.EntityUseAction action;
        EnumWrappers.Hand hand = EnumWrappers.Hand.MAIN_HAND;

        if (MinecraftVersion.CAVES_CLIFFS_1.atOrAbove()) {
          // mc 1.17: hand & action are now in an internal wrapper class
          WrappedEnumEntityUseAction useAction = packet.getEnumEntityUseActions().read(0);
          action = useAction.getAction();

          // the hand is not explicitly send for attacks (always the main hand)
          if (action != EnumWrappers.EntityUseAction.ATTACK) {
            hand = useAction.getHand();
          }
        } else {
          // mc 1.8: hand & action are fields in the packet (or the hand is not even present)
          action = packet.getEntityUseActions().read(0);

          // the hand is not explicitly send for attacks (always the main hand)
          if (action != EnumWrappers.EntityUseAction.ATTACK && MinecraftVersion.COMBAT_UPDATE.atOrAbove()) {
            // mc 1.9: hand is now a thing
            hand = packet.getHands().read(0);
          }
        }

        // call the event
        switch (action) {
          case ATTACK:
            EventDispatcher.dispatch(this.platform, DefaultAttackNpcEvent.attackNpc(npc, player));
            break;
          case INTERACT:
            InteractNpcEvent.Hand usedHand = HAND_CONVERTER.get(hand);
            EventDispatcher.dispatch(this.platform, DefaultInteractNpcEvent.interactNpc(npc, player, usedHand));
            break;
          default:
            // we don't handle INTERACT_AT as the client sends it alongside the interact packet (duplicate event call)
            break;
        }

        // don't pass the packet to the server
        event.setCancelled(true);
      }
    }
  }
}
