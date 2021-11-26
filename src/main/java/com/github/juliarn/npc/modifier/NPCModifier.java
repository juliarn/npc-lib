package com.github.juliarn.npc.modifier;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.github.juliarn.npc.NPC;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

/**
 * An NPCModifier queues packets for NPC modification which can then be send to players via the
 * {@link NPCModifier#send(Player...)} method.
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
  public static final int MINECRAFT_VERSION = MinecraftVersion.getCurrentVersion().getMinor();

  /**
   * All queued packet containers.
   */
  private final List<LazyPacket> packetContainers = new CopyOnWriteArrayList<>();
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
   * Queues the packet for sending.
   *
   * @param packet the packet to queue.
   * @since 2.7-SNAPSHOT
   */
  protected void queuePacket(@NotNull LazyPacket packet) {
    this.packetContainers.add(packet);
  }

  /**
   * Queues the packet instantly meaning that the packet will be build and the memorized instance of
   * the container will be sent to the target player(s). When using this method the target player
   * normally supplied to {@code provide} will be {@code null} as the packet will be sent in the
   * same form to all players.
   *
   * @param packet the packet to queue.
   * @since 2.7-SNAPSHOT
   */
  protected void queueInstantly(@NotNull LazyPacket packet) {
    PacketContainer container = packet.provide(this.npc, null);
    this.packetContainers.add(($, $1) -> container);
  }

  /**
   * Sends the queued modifications to all players
   */
  public void send() {
    this.send(Bukkit.getOnlinePlayers());
  }

  /**
   * Sends the queued modifications to all given {@code players}.
   *
   * @param players The receivers of the packet.
   */
  public void send(@NotNull Iterable<? extends Player> players) {
    players.forEach(player -> {
      try {
        for (LazyPacket packetContainer : this.packetContainers) {
          ProtocolLibrary.getProtocolManager().sendServerPacket(
              player,
              packetContainer.provide(this.npc, player));
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
   * Represents a packet which gets build lazily, normally before sending to a player.
   *
   * @since 2.7-SNAPSHOT
   */
  @FunctionalInterface
  public interface LazyPacket {

    /**
     * Builds the packet container which gets send to the player.
     *
     * @param targetNpc the npc of the modifier context the packet is build for.
     * @param target    the target player to whom the packet will be sent.
     * @return the constructed packet to send.
     */
    @NotNull PacketContainer provide(@NotNull NPC targetNpc, @UnknownNullability Player target);
  }
}
