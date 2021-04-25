package com.github.juliarn.npc.modifier;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.github.juliarn.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An NPCModifier queues packets for NPC modification which can then be send to players via the {@link NPCModifier#send(Player...)} method.
 *
 * @see AnimationModifier
 * @see EquipmentModifier
 * @see MetadataModifier
 * @see RotationModifier
 * @see VisibilityModifier
 * @see LabyModModifier
 */
public class NPCModifier {
  /**
   * The minor version of the servers minecraft version.
   */
  public static final int MINECRAFT_VERSION = ProtocolLibrary.getProtocolManager().getMinecraftVersion().getMinor();

  /**
   * All queued packet containers.
   */
  private final List<PacketContainer> packetContainers = new CopyOnWriteArrayList<>();
  /**
   * The target npc.
   */
  protected NPC npc;

  /**
   * Creates a new npc modifier.
   *
   * @param npc The npc this modifier is for.
   */
  public NPCModifier(@NotNull NPC npc) {
    this.npc = npc;
  }

  /**
   * Creates and adds a new packet container to the packet queue.
   *
   * @param packetType The type of the packet.
   * @return The created packet container.
   */
  @NotNull
  protected PacketContainer newContainer(@NotNull PacketType packetType) {
    return this.newContainer(packetType, true);
  }

  /**
   * Creates and adds a new packet container to the packet queue.
   *
   * @param packetType   The type of the packet.
   * @param withEntityId If the first integer if the packet is the entity id.
   * @return The created packet container.
   */
  protected PacketContainer newContainer(@NotNull PacketType packetType, boolean withEntityId) {
    PacketContainer packetContainer = new PacketContainer(packetType);
    if (withEntityId) {
      packetContainer.getIntegers().write(0, this.npc.getEntityId());
    }

    this.packetContainers.add(packetContainer);
    return packetContainer;
  }

  /**
   * Get the last container in the packet queue or null if there is no container.
   *
   * @return the last container in the packet queue or {@code null} if there is no container.
   */
  protected PacketContainer lastContainer() {
    return this.packetContainers.isEmpty() ? null : this.packetContainers.get(this.packetContainers.size() - 1);
  }

  /**
   * Get the last container in the packet queue or {@code def} if there is no container.
   *
   * @param def The container to return if there is no last container in the queue.
   * @return the last container in the packet queue or {@code def} if there is no container.
   */
  protected PacketContainer lastContainer(PacketContainer def) {
    final PacketContainer container = this.lastContainer();
    return container == null ? def : container;
  }

  /**
   * Sends the queued modifications to all players
   */
  public void send() {
    this.send(Bukkit.getOnlinePlayers());
  }

  /**
   * Sends the queued modifications to all players
   *
   * @param createClone If a copy of each packet container should be done before sending.
   */
  public void send(boolean createClone) {
    this.send(Bukkit.getOnlinePlayers(), createClone);
  }

  /**
   * Sends the queued modifications to all given {@code players}.
   *
   * @param players The receivers of the packet.
   */
  public void send(@NotNull Iterable<? extends Player> players) {
    this.send(players, false);
  }

  /**
   * Sends the queued modifications to all given {@code players}.
   *
   * @param players     The receivers of the packet.
   * @param createClone If a copy of each packet container should be done before sending.
   */
  public void send(@NotNull Iterable<? extends Player> players, boolean createClone) {
    players.forEach(player -> {
      try {
        for (PacketContainer packetContainer : this.packetContainers) {
          ProtocolLibrary.getProtocolManager().sendServerPacket(player, createClone ? packetContainer.shallowClone() : packetContainer);
        }
      } catch (InvocationTargetException exception) {
        exception.printStackTrace();
      }
    });
    this.packetContainers.clear();
  }

  /**
   * Sends the queued modifications to certain players
   *
   * @param targetPlayers the players which should see the modification
   */
  public void send(@NotNull Player... targetPlayers) {
    this.send(Arrays.asList(targetPlayers));
  }

  /**
   * Sends the queued modifications to certain players
   *
   * @param targetPlayers the players which should see the modification
   * @param createClone   If a copy of each packet container should be done before sending.
   */
  public void send(boolean createClone, @NotNull Player... targetPlayers) {
    this.send(Arrays.asList(targetPlayers), createClone);
  }
}
