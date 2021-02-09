package com.github.juliarn.npc.modifier;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.github.juliarn.npc.NPC;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * A modifier for modifying the meta data of a player.
 */
public class MetadataModifier extends NPCModifier {
  /**
   * The queued meta data.
   */
  private final List<WrappedWatchableObject> metadata = new ArrayList<>();

  /**
   * Creates a new modifier.
   *
   * @param npc The npc this modifier is for.
   * @see NPC#metadata()
   */
  @ApiStatus.Internal
  public MetadataModifier(@NotNull NPC npc) {
    super(npc);
  }

  /**
   * Queues the change of a specific meta data.
   *
   * @param metadata The modifier which should get changed.
   * @param value    The new value of the meta data.
   * @param <I>      The input type of the meta modifier.
   * @param <O>      The output type of the meta modifier.
   * @return The same instance of this class, for chaining.
   */
  @NotNull
  public <I, O> MetadataModifier queue(@NotNull EntityMetadata<I, O> metadata, @NotNull I value) {
    return this.queue(metadata.getIndex(), metadata.getMapper().apply(value), metadata.getOutputType());
  }

  /**
   * Queues the change of a specific meta data.
   *
   * @param index The index of the meta data to change.
   * @param value The new value of the meta data.
   * @param clazz The class of the output type.
   * @param <T>   The output type of the meta modified.
   * @return The same instance of this class, for chaining.
   */
  @NotNull
  public <T> MetadataModifier queue(int index, @NotNull T value, @NotNull Class<T> clazz) {
    return this.queue(index, value, MINECRAFT_VERSION < 9 ? null : WrappedDataWatcher.Registry.get(clazz));
  }

  /**
   * Queues the change of a specific meta data.
   *
   * @param index      The index of the meta data to change.
   * @param value      The new value of the meta data.
   * @param serializer The serializer of the data watcher entry.
   * @param <T>        The output type of the meta modified.
   * @return The same instance of this class, for chaining.
   */
  @NotNull
  public <T> MetadataModifier queue(int index, @NotNull T value, @Nullable WrappedDataWatcher.Serializer serializer) {
    this.metadata.add(serializer == null ? new WrappedWatchableObject(
      index,
      value
    ) : new WrappedWatchableObject(
      new WrappedDataWatcher.WrappedDataWatcherObject(index, serializer),
      value
    ));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send(@NotNull Iterable<? extends Player> players, boolean createClone) {
    PacketContainer packetContainer = super.newContainer(PacketType.Play.Server.ENTITY_METADATA);
    packetContainer.getWatchableCollectionModifier().write(0, this.metadata);
    super.send(players, createClone);
  }

  /**
   * A wrapper for entity meta data.
   *
   * @param <I> The input type of this meta modifier.
   * @param <O> The output type of this meta modifier.
   */
  public static class EntityMetadata<I, O> {
    /**
     * An entity meta data for modifying the sneaking state.
     */
    public static final EntityMetadata<Boolean, Byte> SNEAKING = new EntityMetadata<>(
      0,
      Byte.class,
      Collections.emptyList(),
      input -> (byte) (input ? 0x02 : 0)
    );
    /**
     * An entity meta data for modifying the skin layer state.
     */
    public static final EntityMetadata<Boolean, Byte> SKIN_LAYERS = new EntityMetadata<>(
      10,
      Byte.class,
      Arrays.asList(9, 9, 10, 14, 14, 15),
      input -> (byte) (input ? 0xff : 0)
    );

    /**
     * The base index of the meta in the data watcher object.
     */
    private final int baseIndex;
    /**
     * The output mapper class.
     */
    private final Class<O> outputType;
    /**
     * The mapper which maps the input value type to the
     * writeable output type for the data watcher object.
     */
    private final Function<I, O> mapper;
    /**
     * The versions in which the data watcher index was shifted and must be modified.
     */
    private final Collection<Integer> shiftVersions;

    /**
     * Creates a new meta data instance.
     *
     * @param baseIndex     The base index of the meta in the data watcher object.
     * @param outputType    The output mapper class.
     * @param shiftVersions The versions in which the data watcher index was shifted and must be modified.
     * @param mapper        The mapper which maps the input value type to the
     *                      writeable output type for the data watcher object.
     */
    public EntityMetadata(int baseIndex, Class<O> outputType, Collection<Integer> shiftVersions, Function<I, O> mapper) {
      this.baseIndex = baseIndex;
      this.outputType = outputType;
      this.shiftVersions = shiftVersions;
      this.mapper = mapper;
    }

    /**
     * Get the index in the data watcher object for the minecraft version of the current
     * server instance.
     *
     * @return the index in the data watcher object to modify.
     */
    public int getIndex() {
      return this.baseIndex + Math.toIntExact(this.shiftVersions.stream().filter(minor -> NPCModifier.MINECRAFT_VERSION >= minor).count());
    }

    /**
     * Get the type of the output value.
     *
     * @return the type of the output value.
     */
    @NotNull
    public Class<O> getOutputType() {
      return this.outputType;
    }

    /**
     * Get the mapper of this modifier converting the input type to
     * a writeable object for a data watcher.
     *
     * @return the mapper of this modifier converting the input type.
     */
    @NotNull
    public Function<I, O> getMapper() {
      return this.mapper;
    }
  }
}
