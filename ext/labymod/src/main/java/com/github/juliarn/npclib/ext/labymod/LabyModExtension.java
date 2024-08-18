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

package com.github.juliarn.npclib.ext.labymod;

import com.github.juliarn.npclib.api.protocol.OutboundPacket;
import com.github.juliarn.npclib.api.protocol.PlatformPacketAdapter;
import java.nio.ByteBuffer;
import java.util.function.UnaryOperator;
import org.jetbrains.annotations.NotNull;

public final class LabyModExtension {

  // full list of packet ids can be found here:
  // https://github.com/LabyMod/labymod4-server-api/blob/master/core/src/main/java/net/labymod/serverapi/core/LabyModProtocol.java
  private static final int EMOTE_PACKET_ID = 16;
  private static final int SPRAY_PACKET_ID = 17;
  private static final String LM_PLUGIN_CHANNEL = "labymod:neo";

  // a sensitive default for users that only send out a single sticker or emote via one packet
  private static final int DEFAULT_BUFFER_ALLOCATION_BYTES = 32;

  private LabyModExtension() {
    throw new UnsupportedOperationException();
  }

  // list of emote ids can be found here:
  // https://dev.labymod.net/pages/server/labymod/features/emotes
  public static <W, P, I, E> @NotNull OutboundPacket<W, P, I, E> createEmotePacket(
    @NotNull PlatformPacketAdapter<W, P, I, E> packetAdapter,
    int... emoteIds
  ) {
    return (player, npc) -> {
      byte[] payloadData = constructPayloadData(EMOTE_PACKET_ID, buffer -> {
        // put the amount of emotes to send into the buffer & write each emote into the buffer
        // an emote is also prefixed with the npc uuid which is always the target npc id in our case
        ByteBuffer target = BufferUtil.putVarInt(buffer, emoteIds.length);
        for (int emoteId : emoteIds) {
          target = BufferUtil.putUUID(target, npc.profile().uniqueId());
          target = BufferUtil.putVarInt(target, emoteId);
        }
        return target;
      });

      // create a new plugin message outbound packet and schedule the payload data
      packetAdapter.createCustomPayloadPacket(LM_PLUGIN_CHANNEL, payloadData).schedule(player, npc);
    };
  }

  // currently not supported by the LM api
  public static <W, P, I, E> @NotNull OutboundPacket<W, P, I, E> createStickerPacket(
    @NotNull PlatformPacketAdapter<W, P, I, E> packetAdapter,
    int... stickerIds
  ) {
    return (player, npc) -> {
    };
  }

  private static byte[] constructPayloadData(int packetId, @NotNull UnaryOperator<ByteBuffer> packetWriter) {
    ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_BUFFER_ALLOCATION_BYTES);

    // put the message key, then the data
    buffer = BufferUtil.putVarInt(buffer, packetId);
    buffer = packetWriter.apply(buffer);

    // get the buffer content
    return BufferUtil.extractData(buffer);
  }
}
