package com.github.juliarn.npc.util;


import com.github.juliarn.npc.modifier.NPCModifier;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class EntityMetadata<T> {

    public static final EntityMetadata<Byte> POSE = new EntityMetadata<>(0, Byte.class, Collections.emptyList());
    public static final EntityMetadata<Byte> SKIN_LAYERS = new EntityMetadata<>(13, Byte.class, Arrays.asList(14, 14, 15));

    private final int baseIndex;

    private final Class<T> type;

    private final Collection<Integer> shiftVersions;

    EntityMetadata(int baseIndex, Class<T> type, Collection<Integer> shiftVersions) {
        this.baseIndex = baseIndex;
        this.type = type;
        this.shiftVersions = shiftVersions;
    }

    public int getIndex() {
        return this.baseIndex + Math.toIntExact(this.shiftVersions.stream().filter(minor -> NPCModifier.MINECRAFT_VERSION >= minor).count());
    }

    public int getBaseIndex() {
        return baseIndex;
    }

    public Class<T> getType() {
        return type;
    }

    public Collection<Integer> getShiftVersions() {
        return shiftVersions;
    }

}
