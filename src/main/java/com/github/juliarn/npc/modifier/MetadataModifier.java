package com.github.juliarn.npc.modifier;

import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.github.juliarn.npc.NPC;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A modifier for modifying the metadata of a player.
 */
public class MetadataModifier extends NPCModifier {

  /**
   * The queued metadata.
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
   * Queues the change of a specific metadata.
   *
   * @param metadata The modifier which should get changed.
   * @param value    The new value of the metadata.
   * @param <I>      The input type of the meta modifier.
   * @param <O>      The output type of the meta modifier.
   * @return The same instance of this class, for chaining.
   */
  @NotNull
  public <I, O> MetadataModifier queue(@NotNull EntityMetadata<I, O> metadata, @NotNull I value) {
    if (!metadata.getAvailabilitySupplier().get()) {
      return this;
    }

    for (EntityMetadata<I, Object> relatedMetadata : metadata.getRelatedMetadata()) {
      if (!relatedMetadata.getAvailabilitySupplier().get()) {
        continue;
      }
      this.queue(relatedMetadata.getIndex(), relatedMetadata.getMapper().apply(value),
          relatedMetadata.getOutputType());
    }
    return this
        .queue(metadata.getIndex(), metadata.getMapper().apply(value), metadata.getOutputType());
  }

  /**
   * Queues the change of a specific metadata.
   *
   * @param index The index of the metadata to change.
   * @param value The new value of the metadata.
   * @param clazz The class of the output type.
   * @param <T>   The type of the value.
   * @return The same instance of this class, for chaining.
   */
  @NotNull
  public <T> MetadataModifier queue(int index, @NotNull T value, @NotNull Class<T> clazz) {
    return this
        .queue(index, value, MINECRAFT_VERSION < 9 ? null : WrappedDataWatcher.Registry.get(clazz));
  }

  /**
   * Queues the change of a specific metadata.
   *
   * @param index      The index of the metadata to change.
   * @param value      The new value of the metadata.
   * @param serializer The serializer of the data watcher entry.
   * @param <T>        The output type of the meta modified.
   * @return The same instance of this class, for chaining.
   */
  @NotNull
  public <T> MetadataModifier queue(
      int index,
      @NotNull T value,
      @Nullable WrappedDataWatcher.Serializer serializer
  ) {
    this.metadata.add(serializer == null
        ? new WrappedWatchableObject(index, value)
        : new WrappedWatchableObject(
            new WrappedDataWatcher.WrappedDataWatcherObject(index, serializer), value));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send(@NotNull Iterable<? extends Player> players) {
    super.queueInstantly((targetNpc, target) -> {
      PacketContainer container = new PacketContainer(Server.ENTITY_METADATA);
      container.getIntegers().write(0, targetNpc.getEntityId());
      container.getWatchableCollectionModifier().write(0, this.metadata);
      return container;
    });
    super.send(players);
  }

  /**
   * A wrapper for entity metadata.
   *
   * @param <I> The input type of this metadata modifier.
   * @param <O> The output type of this metadata modifier.
   */
  public static class EntityMetadata<I, O> {

    /**
     * An entity metadata for modifying the sneaking state.
     */
    @SuppressWarnings("unchecked")
    public static final EntityMetadata<Boolean, Byte> SNEAKING = new EntityMetadata<>(
        0,
        Byte.class,
        Collections.emptyList(),
        input -> (byte) (input ? 0x02 : 0),
        // with 1.16+, we have to change the pose too to make the NPC sneak
        new EntityMetadata<>(
            6,
            (Class<Object>) EnumWrappers.getEntityPoseClass(),
            Collections.emptyList(),
            input -> (input ? EnumWrappers.EntityPose.CROUCHING : EnumWrappers.EntityPose.STANDING)
                .toNms(),
            () -> NPCModifier.MINECRAFT_VERSION >= 14));
    /**
     * An entity metadata for modifying the skin layer state.
     */
    public static final EntityMetadata<Boolean, Byte> SKIN_LAYERS = new EntityMetadata<>(
        10,
        Byte.class,
        Arrays.asList(9, 9, 10, 14, 14, 15, 17),
        input -> (byte) (input ? 0xff : 0));
    /**
     * An entity metadata for modifying the pose.
     */
    @SuppressWarnings("unchecked")
    public static final EntityMetadata<EnumWrappers.EntityPose, Object> POSE = new EntityMetadata<>(
        6,
        (Class<Object>) EnumWrappers.getEntityPoseClass(),
        Collections.emptyList(),
        EnumWrappers.EntityPose::toNms,
        () -> NPCModifier.MINECRAFT_VERSION >= 14);

    /**
     * The base index of the metadata in the data watcher object.
     */
    private final int baseIndex;
    /**
     * The output mapper class.
     */
    private final Class<O> outputType;
    /**
     * The mapper which maps the input value type to the writeable output type for the data watcher
     * object.
     */
    private final Function<I, O> mapper;
    /**
     * The versions in which the data watcher index was shifted and must be modified.
     */
    private final Collection<Integer> shiftVersions;
    /**
     * A supplier returning if the entity metadata is available for this server version.
     */
    private final Supplier<Boolean> availabilitySupplier;
    /**
     * The metadata which is related to this metadata, will be applied too if this metadata is
     * applied.
     */
    private final Collection<EntityMetadata<I, Object>> relatedMetadata;

    /**
     * Creates a new metadata instance.
     *
     * @param baseIndex            The base index of the metadata in the data watcher object.
     * @param outputType           The output mapper class.
     * @param shiftVersions        The versions in which the data watcher index was shifted and must
     *                             be modified.
     * @param mapper               The mapper which maps the input value type to the writeable
     *                             output type for the data watcher object.
     * @param availabilitySupplier A supplier returning if the entity metadata is available for this
     *                             server version.
     * @param relatedMetadata      The metadata which is related to this metadata, will be applied
     *                             too if this metadata is applied.
     */
    @SafeVarargs
    public EntityMetadata(int baseIndex, Class<O> outputType, Collection<Integer> shiftVersions,
        Function<I, O> mapper, Supplier<Boolean> availabilitySupplier,
        EntityMetadata<I, Object>... relatedMetadata) {
      this.baseIndex = baseIndex;
      this.outputType = outputType;
      this.shiftVersions = shiftVersions;
      this.mapper = mapper;
      this.availabilitySupplier = availabilitySupplier;
      this.relatedMetadata = Arrays.asList(relatedMetadata);
    }

    /**
     * Creates a new metadata instance.
     *
     * @param baseIndex       The base index of the metadata in the data watcher object.
     * @param outputType      The output mapper class.
     * @param shiftVersions   The versions in which the data watcher index was shifted and must be
     *                        modified.
     * @param mapper          The mapper which maps the input value type to the writeable output
     *                        type for the data watcher object.
     * @param relatedMetadata The metadata which is related to this metadata, will be applied too if
     *                        this metadata is applied.
     */
    @SafeVarargs
    public EntityMetadata(int baseIndex, Class<O> outputType, Collection<Integer> shiftVersions,
        Function<I, O> mapper, EntityMetadata<I, Object>... relatedMetadata) {
      this(baseIndex, outputType, shiftVersions, mapper, () -> true, relatedMetadata);
    }

    /**
     * Get the index in the data watcher object for the minecraft version of the current server
     * instance.
     *
     * @return the index in the data watcher object to modify.
     */
    public int getIndex() {
      return this.baseIndex + Math.toIntExact(
          this.shiftVersions.stream().filter(minor -> NPCModifier.MINECRAFT_VERSION >= minor)
              .count());
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
     * Get the mapper of this modifier converting the input type to a writeable object for a data
     * watcher.
     *
     * @return the mapper of this modifier converting the input type.
     */
    @NotNull
    public Function<I, O> getMapper() {
      return this.mapper;
    }

    /**
     * @return A supplier returning if the entity metadata is available for this server version.
     */
    @NotNull
    public Supplier<Boolean> getAvailabilitySupplier() {
      return this.availabilitySupplier;
    }

    /**
     * @return The metadata which is related to this metadata, will be applied too if this metadata
     * is applied.
     */
    @NotNull
    public Collection<EntityMetadata<I, Object>> getRelatedMetadata() {
      return this.relatedMetadata;
    }
  }
}
