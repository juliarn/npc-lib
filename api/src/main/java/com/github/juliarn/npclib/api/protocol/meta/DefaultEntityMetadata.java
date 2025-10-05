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

package com.github.juliarn.npclib.api.protocol.meta;

import com.github.juliarn.npclib.api.protocol.enums.EntityPose;
import com.github.juliarn.npclib.api.protocol.enums.EntityStatus;
import java.util.Collection;
import java.util.function.Function;

interface DefaultEntityMetadata {

  // https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Entity - see index 0
  EntityMetadataFactory<Collection<EntityStatus>, Byte> ENTITY_STATUS =
    EntityMetadataFactory.<Collection<EntityStatus>, Byte>metaFactoryBuilder()
      .baseIndex(0)
      .type(Byte.class)
      .inputConverter(entries -> {
        // if there are no entries the mask is always 0
        if (entries.isEmpty()) {
          return (byte) 0;
        }

        // calculate the bitmask to send
        byte entryMask = 0;
        for (EntityStatus entry : entries) {
          entryMask |= entry.bitmask();
        }

        return entryMask;
      }).build();

  // https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Entity - see index 6
  EntityMetadataFactory<EntityPose, EntityPose> ENTITY_POSE =
    EntityMetadataFactory.<EntityPose, EntityPose>metaFactoryBuilder()
      .baseIndex(6)
      .type(EntityPose.class)
      .inputConverter(Function.identity())
      .availabilityChecker(versionAccessor -> versionAccessor.atLeast(1, 14, 0))
      .build();

  // https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Entity - see index 0 and 6
  EntityMetadataFactory<Boolean, Byte> SNEAKING = EntityMetadataFactory.<Boolean, Byte>metaFactoryBuilder()
    .baseIndex(0)
    .type(Byte.class)
    .inputConverter(value -> (byte) (value ? 0x02 : 0x00))
    .addRelatedMetadata(EntityMetadataFactory.<Boolean, Object>metaFactoryBuilder()
      .baseIndex(6)
      .type(EntityPose.class)
      .inputConverter(value -> value ? EntityPose.CROUCHING : EntityPose.STANDING)
      .availabilityChecker(versionAccessor -> versionAccessor.atLeast(1, 14, 0))
      .build())
    .build();

  // https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Entity - see index 7
  EntityMetadataFactory<Boolean, Integer> SHAKING = EntityMetadataFactory.<Boolean, Integer>metaFactoryBuilder()
    .baseIndex(7)
    .type(Integer.class)
    .inputConverter(value -> value ? 250 : 0)
    .availabilityChecker(versionAccessor -> versionAccessor.atLeast(1, 17, 0))
    .build();

  // https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Living_Entity - see index 8
  EntityMetadataFactory<Boolean, Byte> USING_ITEM = EntityMetadataFactory.<Boolean, Byte>metaFactoryBuilder()
    .baseIndex(5)
    .type(Byte.class)
    .shiftBaseIndex(10, 0, 1)
    .shiftBaseIndex(14, 0, 1)
    .shiftBaseIndex(17, 0, 1)
    .inputConverter(value -> (byte) (value ? 0x01 : 0x00))
    .availabilityChecker(versionAccessor -> versionAccessor.atLeast(1, 9, 0))
    .build();

  // https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Living_Entity - see index 12
  EntityMetadataFactory<Integer, Integer> ARROW_COUNT = EntityMetadataFactory.<Integer, Integer>metaFactoryBuilder()
    .baseIndex(9)
    .type(Integer.class)
    .shiftBaseIndex(10, 0, 1)
    .shiftBaseIndex(14, 0, 1)
    .shiftBaseIndex(17, 0, 1)
    .inputConverter(value -> Math.max(0, value))
    .build();

  // https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Avatar - see index 16
  EntityMetadataFactory<Boolean, Byte> SKIN_LAYERS = EntityMetadataFactory.<Boolean, Byte>metaFactoryBuilder()
    .baseIndex(10)
    .type(Byte.class)
    .shiftBaseIndex(9, 0, 2)
    .shiftBaseIndex(10, 0, 1)
    .shiftBaseIndex(14, 0, 2)
    .shiftBaseIndex(15, 0, 1)
    .shiftBaseIndex(17, 0, 1)
    .shiftBaseIndex(21, 9, -1)
    .inputConverter(value -> (byte) (value ? 0xff : 0x00))
    .build();
}
