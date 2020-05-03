package com.github.juliarn.npc.util;


import com.github.juliarn.npc.modifier.NPCModifier;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

public class EntityMetadata<I, O> {

    public static final EntityMetadata<Boolean, Byte> SNEAKING = new EntityMetadata<>(
            0,
            Byte.class,
            Collections.emptyList(),
            input -> (byte) (input ? 0x02 : 0)
    );

    public static final EntityMetadata<Boolean, Byte> SKIN_LAYERS = new EntityMetadata<>(
            10,
            Byte.class,
            Arrays.asList(9, 9, 10, 14, 14, 15),
            input -> (byte) (input ? 0xff : 0)
    );

    private final int baseIndex;

    private final Class<O> outputType;

    private final Collection<Integer> shiftVersions;

    private final Function<I, O> mapper;

    EntityMetadata(int baseIndex, Class<O> outputType, Collection<Integer> shiftVersions, Function<I, O> mapper) {
        this.baseIndex = baseIndex;
        this.outputType = outputType;
        this.shiftVersions = shiftVersions;
        this.mapper = mapper;
    }

    public int getIndex() {
        return this.baseIndex + Math.toIntExact(this.shiftVersions.stream().filter(minor -> NPCModifier.MINECRAFT_VERSION >= minor).count());
    }

    public int getBaseIndex() {
        return baseIndex;
    }

    public Class<O> getOutputType() {
        return outputType;
    }

    public Collection<Integer> getShiftVersions() {
        return shiftVersions;
    }

    public Function<I, O> getMapper() {
        return mapper;
    }

}
