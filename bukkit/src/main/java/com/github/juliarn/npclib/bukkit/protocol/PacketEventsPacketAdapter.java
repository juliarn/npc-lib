/*
 * This file is part of npc-lib, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022 Julian M., Pasqual K. and contributors
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

import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.Platform;
import com.github.juliarn.npclib.api.PlatformVersionAccessor;
import com.github.juliarn.npclib.api.Position;
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
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract;
import com.github.retrooper.packetevents.event.simple.PacketPlayReceiveEvent;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataType;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import com.github.retrooper.packetevents.util.TimeStampMode;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
import com.google.common.collect.ImmutableMap;
import com.google.gson.reflect.TypeToken;
import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import io.github.retrooper.packetevents.adventure.serializer.legacy.LegacyComponentSerializer;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

final class PacketEventsPacketAdapter implements PlatformPacketAdapter<World, Player, ItemStack, Plugin> {

  static final PacketEventsPacketAdapter INSTANCE = new PacketEventsPacketAdapter();

  private static final PacketEventsSettings PACKET_EVENTS_SETTINGS = new PacketEventsSettings()
    .debug(false)
    .bStats(true)
    .checkForUpdates(false)
    .readOnlyListeners(true)
    .timeStampMode(TimeStampMode.NONE);

  private static final Type OPTIONAL_CHAT_COMPONENT_TYPE = TypeToken.getParameterized(
    Optional.class,
    net.kyori.adventure.text.Component.class
  ).getType();

  private static final EnumMap<ItemSlot, EquipmentSlot> ITEM_SLOT_CONVERTER;
  private static final EnumMap<InteractionHand, InteractNpcEvent.Hand> HAND_CONVERTER;
  private static final EnumMap<PlayerInfoAction, WrapperPlayServerPlayerInfo.Action> PLAYER_INFO_ACTION_CONVERTER;
  private static final EnumMap<EntityAnimation, WrapperPlayServerEntityAnimation.EntityAnimationType> ENTITY_ANIMATION_CONVERTER;
  private static final EnumMap<EntityPose, com.github.retrooper.packetevents.protocol.entity.pose.EntityPose> ENTITY_POSE_CONVERTER;

  // serializer converters for metadata
  private static final Map<Type, EntityDataType<?>> ENTITY_DATA_TYPE_LOOKUP;
  private static final Map<Type, BiFunction<PlatformVersionAccessor, Object, Map.Entry<Type, Object>>> SERIALIZER_CONVERTERS;

  static {
    // associate item slots actions with their respective packet events enum
    ITEM_SLOT_CONVERTER = new EnumMap<>(ItemSlot.class);
    ITEM_SLOT_CONVERTER.put(ItemSlot.MAIN_HAND, EquipmentSlot.MAIN_HAND);
    ITEM_SLOT_CONVERTER.put(ItemSlot.OFF_HAND, EquipmentSlot.OFF_HAND);
    ITEM_SLOT_CONVERTER.put(ItemSlot.FEET, EquipmentSlot.BOOTS);
    ITEM_SLOT_CONVERTER.put(ItemSlot.LEGS, EquipmentSlot.LEGGINGS);
    ITEM_SLOT_CONVERTER.put(ItemSlot.CHEST, EquipmentSlot.CHEST_PLATE);
    ITEM_SLOT_CONVERTER.put(ItemSlot.HEAD, EquipmentSlot.HELMET);

    // associate hand actions with their respective packet events enum
    HAND_CONVERTER = new EnumMap<>(InteractionHand.class);
    HAND_CONVERTER.put(InteractionHand.MAIN_HAND, InteractNpcEvent.Hand.MAIN_HAND);
    HAND_CONVERTER.put(InteractionHand.OFF_HAND, InteractNpcEvent.Hand.OFF_HAND);

    // associate player info actions with their respective packet events enum
    PLAYER_INFO_ACTION_CONVERTER = new EnumMap<>(PlayerInfoAction.class);
    PLAYER_INFO_ACTION_CONVERTER.put(PlayerInfoAction.ADD_PLAYER, WrapperPlayServerPlayerInfo.Action.ADD_PLAYER);
    PLAYER_INFO_ACTION_CONVERTER.put(PlayerInfoAction.REMOVE_PLAYER, WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER);

    // associate entity animations with their respective packet events enum
    ENTITY_ANIMATION_CONVERTER = new EnumMap<>(EntityAnimation.class);
    ENTITY_ANIMATION_CONVERTER.put(
      EntityAnimation.SWING_MAIN_ARM,
      WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM);
    ENTITY_ANIMATION_CONVERTER.put(
      EntityAnimation.TAKE_DAMAGE,
      WrapperPlayServerEntityAnimation.EntityAnimationType.HURT);
    ENTITY_ANIMATION_CONVERTER.put(
      EntityAnimation.LEAVE_BED,
      WrapperPlayServerEntityAnimation.EntityAnimationType.WAKE_UP);
    ENTITY_ANIMATION_CONVERTER.put(
      EntityAnimation.SWING_OFF_HAND,
      WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFF_HAND);
    ENTITY_ANIMATION_CONVERTER.put(
      EntityAnimation.CRITICAL_EFFECT,
      WrapperPlayServerEntityAnimation.EntityAnimationType.CRITICAL_HIT);
    ENTITY_ANIMATION_CONVERTER.put(
      EntityAnimation.MAGIC_CRITICAL_EFFECT,
      WrapperPlayServerEntityAnimation.EntityAnimationType.MAGIC_CRITICAL_HIT);

    // associate entity poses with their respective packet events enum
    ENTITY_POSE_CONVERTER = new EnumMap<>(EntityPose.class);
    ENTITY_POSE_CONVERTER.put(
      EntityPose.STANDING,
      com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.STANDING);
    ENTITY_POSE_CONVERTER.put(
      EntityPose.FALL_FLYING,
      com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.FALL_FLYING);
    ENTITY_POSE_CONVERTER.put(
      EntityPose.SLEEPING,
      com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.SLEEPING);
    ENTITY_POSE_CONVERTER.put(
      EntityPose.SWIMMING,
      com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.SWIMMING);
    ENTITY_POSE_CONVERTER.put(
      EntityPose.SPIN_ATTACK,
      com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.SPIN_ATTACK);
    ENTITY_POSE_CONVERTER.put(
      EntityPose.CROUCHING,
      com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.CROUCHING);
    ENTITY_POSE_CONVERTER.put(
      EntityPose.LONG_JUMPING,
      com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.LONG_JUMPING);
    ENTITY_POSE_CONVERTER.put(
      EntityPose.DYING,
      com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.DYING);

    // meta serializers
    //noinspection SuspiciousMethodCalls
    SERIALIZER_CONVERTERS = ImmutableMap.<Type, BiFunction<PlatformVersionAccessor, Object, Map.Entry<Type, Object>>>builder()
      .put(EntityPose.class, ($, value) -> new AbstractMap.SimpleImmutableEntry<>(
        com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.class,
        ENTITY_POSE_CONVERTER.get(value)))
      .put(
        TypeToken.getParameterized(Optional.class, Component.class).getType(),
        (versionAccessor, value) -> {
          //noinspection unchecked
          Optional<Component> optionalComponent = (Optional<Component>) value;
          // check if the display name is wrapped in a component
          if (versionAccessor.atLeast(1, 13, 0)) {
            // construct the entry
            return new AbstractMap.SimpleImmutableEntry<>(
              OPTIONAL_CHAT_COMPONENT_TYPE,
              optionalComponent.map(component -> {
                // build the component based on the given input
                if (component.rawMessage() != null) {
                  return LegacyComponentSerializer.legacySection().deserialize(component.rawMessage());
                } else {
                  return GsonComponentSerializer.gson().deserializeOrNull(component.encodedJsonMessage());
                }
              }));
          } else {
            return new AbstractMap.SimpleImmutableEntry<>(String.class, optionalComponent
              .map(component -> Objects.requireNonNull(
                component.rawMessage(),
                "Versions older than 1.13 don't support json component"))
              .orElse(null));
          }
        })
      .build();

    ENTITY_DATA_TYPE_LOOKUP = ImmutableMap.<Type, EntityDataType<?>>builder()
      .put(byte.class, EntityDataTypes.BYTE)
      .put(int.class, EntityDataTypes.INT)
      .put(float.class, EntityDataTypes.FLOAT)
      .put(boolean.class, EntityDataTypes.BOOLEAN)
      .put(String.class, EntityDataTypes.STRING)
      .put(OPTIONAL_CHAT_COMPONENT_TYPE, EntityDataTypes.OPTIONAL_COMPONENT)
      .put(com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.class, EntityDataTypes.ENTITY_POSE)
      .build();
  }

  // lazy initialized, then never null again
  private ServerVersion serverVersion;
  private PlayerManager packetPlayerManager;

  private static Location npcLocation(@NotNull Npc<?, ?, ?, ?> npc) {
    Position pos = npc.position();
    return new Location(pos.x(), pos.y(), pos.z(), pos.yaw(), pos.pitch());
  }

  private static @NotNull EntityData createEntityData(
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
    }

    return new EntityData(index, ENTITY_DATA_TYPE_LOOKUP.get(type), value);
  }

  @Override
  public @NotNull OutboundPacket<World, Player, ItemStack, Plugin> createEntitySpawnPacket() {
    return (player, npc) -> {
      // SpawnPlayer (https://wiki.vg/Protocol#Spawn_Player)
      Location location = npcLocation(npc);
      PacketWrapper<?> wrapper = new WrapperPlayServerSpawnPlayer(npc.entityId(), npc.profile().uniqueId(), location);

      // send the packet without notifying any listeners
      this.packetPlayerManager.sendPacketSilently(player, wrapper);
    };
  }

  @Override
  public @NotNull OutboundPacket<World, Player, ItemStack, Plugin> createEntityRemovePacket() {
    return (player, npc) -> {
      // DestroyEntities (https://wiki.vg/Protocol#Destroy_Entities)
      PacketWrapper<?> wrapper = new WrapperPlayServerDestroyEntities(npc.entityId());
      this.packetPlayerManager.sendPacketSilently(player, wrapper);
    };
  }

  @Override
  public @NotNull OutboundPacket<World, Player, ItemStack, Plugin> createPlayerInfoPacket(
    @NotNull PlayerInfoAction action
  ) {
    return (player, npc) -> npc.settings().profileResolver().resolveNpcProfile(player, npc).thenAcceptAsync(profile -> {
      // convert the profile to a UserProfile
      UserProfile userProfile = new UserProfile(profile.uniqueId(), profile.name());
      for (ProfileProperty property : profile.properties()) {
        TextureProperty textureProperty = new TextureProperty(property.name(), property.value(), property.signature());
        userProfile.getTextureProperties().add(textureProperty);
      }

      // create the player profile data
      WrapperPlayServerPlayerInfo.PlayerData playerData = new WrapperPlayServerPlayerInfo.PlayerData(
        null,
        userProfile,
        GameMode.CREATIVE,
        20);

      // PlayerInfo (https://wiki.vg/Protocol#Player_Info)
      WrapperPlayServerPlayerInfo.Action playerInfoAction = PLAYER_INFO_ACTION_CONVERTER.get(action);
      WrapperPlayServerPlayerInfo wrapper = new WrapperPlayServerPlayerInfo(playerInfoAction, playerData);

      // send the packet without notifying any listeners
      this.packetPlayerManager.sendPacketSilently(player, wrapper);
    });
  }

  @Override
  public @NotNull OutboundPacket<World, Player, ItemStack, Plugin> createRotationPacket(float yaw, float pitch) {
    return (player, npc) -> {
      Position pos = npc.position();

      // head rotation (https://wiki.vg/Protocol#Entity_Head_Look)
      PacketWrapper<?> headRotation = new WrapperPlayServerEntityHeadLook(npc.entityId(), pos.yaw());

      // entity teleport (https://wiki.vg/Protocol#Entity_Teleport) or Player Rotation (https://wiki.vg/Protocol#Player_Rotation)
      PacketWrapper<?> rotation;
      if (this.serverVersion.isNewerThanOrEquals(ServerVersion.V_1_9)) {
        // mc 1.9: player rotation
        rotation = new WrapperPlayServerEntityRotation(npc.entityId(), pos.yaw(), pos.pitch(), true);
      } else {
        // mc 1.8: entity teleport
        rotation = new WrapperPlayServerEntityTeleport(npc.entityId(), npcLocation(npc), true);
      }

      // send the packet without notifying any listeners
      this.packetPlayerManager.sendPacketSilently(player, rotation);
      this.packetPlayerManager.sendPacketSilently(player, headRotation);
    };
  }

  @Override
  public @NotNull OutboundPacket<World, Player, ItemStack, Plugin> createAnimationPacket(
    @NotNull EntityAnimation animation
  ) {
    return (player, npc) -> {
      // EntityAnimation (https://wiki.vg/Protocol#Entity_Animation_.28clientbound.29)
      WrapperPlayServerEntityAnimation.EntityAnimationType animationType = ENTITY_ANIMATION_CONVERTER.get(animation);
      PacketWrapper<?> wrapper = new WrapperPlayServerEntityAnimation(npc.entityId(), animationType);

      // send the packet without notifying any listeners
      this.packetPlayerManager.sendPacketSilently(player, wrapper);
    };
  }

  @Override
  public @NotNull OutboundPacket<World, Player, ItemStack, Plugin> createEquipmentPacket(
    @NotNull ItemSlot slot,
    @NotNull ItemStack item
  ) {
    return (player, npc) -> {
      EquipmentSlot equipmentSlot = ITEM_SLOT_CONVERTER.get(slot);
      com.github.retrooper.packetevents.protocol.item.ItemStack is = SpigotReflectionUtil.decodeBukkitItemStack(item);

      // EntityEquipment (https://wiki.vg/Protocol#Entity_Equipment)
      Equipment equipment = new Equipment(equipmentSlot, is);
      PacketWrapper<?> wrapper = new WrapperPlayServerEntityEquipment(
        npc.entityId(),
        Collections.singletonList(equipment));

      // send the packet without notifying any listeners
      this.packetPlayerManager.sendPacketSilently(player, wrapper);
    };
  }

  @Override
  public @NotNull OutboundPacket<World, Player, ItemStack, Plugin> createCustomPayloadPacket(
    @NotNull String channelId,
    byte[] payload
  ) {
    return (player, npc) -> {
      // CustomPayload (https://wiki.vg/Protocol#Custom_Payload)
      PacketWrapper<?> wrapper = new WrapperPlayServerPluginMessage(channelId, payload);
      this.packetPlayerManager.sendPacketSilently(player, wrapper);
    };
  }

  @Override
  public @NotNull <T, O> OutboundPacket<World, Player, ItemStack, Plugin> createEntityMetaPacket(
    @NotNull T value,
    @NotNull EntityMetadataFactory<T, O> metadata
  ) {
    return (player, npc) -> {
      // create the entity meta
      PlatformVersionAccessor versionAccessor = npc.platform().versionAccessor();
      EntityMetadata<O> entityMetadata = metadata.create(value, versionAccessor);

      // check if the meta is available
      if (!entityMetadata.available()) {
        return;
      }

      // construct the meta we want to send out
      List<EntityData> entityData = new ArrayList<>();
      entityData.add(createEntityData(
        entityMetadata.index(),
        entityMetadata.type(),
        entityMetadata.value(),
        versionAccessor));

      // add ll dependant metas
      for (EntityMetadataFactory<T, Object> relatedMetadata : metadata.relatedMetadata()) {
        EntityMetadata<Object> related = relatedMetadata.create(value, versionAccessor);
        if (related.available()) {
          entityData.add(createEntityData(related.index(), related.type(), related.value(), versionAccessor));
        }
      }

      // EntityMetadata (https://wiki.vg/Protocol#Entity_Metadata)
      PacketWrapper<?> wrapper = new WrapperPlayServerEntityMetadata(npc.entityId(), entityData);
      this.packetPlayerManager.sendPacketSilently(player, wrapper);
    };
  }

  @Override
  public void initialize(@NotNull Platform<World, Player, ItemStack, Plugin> platform) {
    // build and initialize the packet events api
    PacketEventsAPI<Plugin> packetEventsApi = SpigotPacketEventsBuilder.buildNoCache(
      platform.extension(),
      PACKET_EVENTS_SETTINGS);
    packetEventsApi.init();

    // while I am not the biggest fan of that, it looks like
    // that packet events is using the instance internally everywhere
    // instead of passing the created instance around, which leaves us
    // no choice than setting it as well :/
    PacketEvents.setAPI(packetEventsApi);

    // store the packet player manager & server version
    this.packetPlayerManager = packetEventsApi.getPlayerManager();
    this.serverVersion = packetEventsApi.getServerManager().getVersion();

    // add the packet listener
    packetEventsApi.getEventManager().registerListener(new NpcUsePacketAdapter(platform));
  }

  private static final class NpcUsePacketAdapter extends SimplePacketListenerAbstract {

    private final Platform<World, Player, ItemStack, Plugin> platform;

    public NpcUsePacketAdapter(@NotNull Platform<World, Player, ItemStack, Plugin> platform) {
      super(PacketListenerPriority.MONITOR);
      this.platform = platform;
    }

    @Override
    public void onPacketPlayReceive(@NotNull PacketPlayReceiveEvent event) {
      // check for an entity use packet
      Object player = event.getPlayer();
      if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
        WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);

        // get the associated npc from the tracked entities
        Npc<World, Player, ItemStack, Plugin> npc = this.platform.npcTracker().npcById(packet.getEntityId());
        if (npc != null) {
          // call the event
          switch (packet.getAction()) {
            case ATTACK:
              EventDispatcher.dispatch(this.platform, DefaultAttackNpcEvent.attackNpc(npc, player));
              break;
            case INTERACT:
              InteractNpcEvent.Hand hand = HAND_CONVERTER.get(packet.getHand());
              EventDispatcher.dispatch(this.platform, DefaultInteractNpcEvent.interactNpc(npc, player, hand));
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
}
