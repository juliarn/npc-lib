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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public final class LabyModExtension {

  private static final String EMOTE_ID_PROPERTY = "emote_id";
  private static final String STICKER_ID_PROPERTY = "sticker_id";

  private static final String EMOTE_API_MESSAGE_KEY = "emote_api";
  private static final String STICKER_API_MESSAGE_KEY = "sticker_api";

  private static final String LM_PLUGIN_CHANNEL = "labymod3:main";

  private static final int DEFAULT_BUFFER_ALLOCATION_BYTES = 128;

  private LabyModExtension() {
    throw new UnsupportedOperationException();
  }

  public static <W, P, I, E> @NotNull OutboundPacket<W, P, I, E> createEmotePacket(
    @NotNull PlatformPacketAdapter<W, P, I, E> packetAdapter,
    int... emoteIds
  ) {
    return (player, npc) -> {
      // construct the data we need to write
      JsonArray data = createIdJsonData(EMOTE_ID_PROPERTY, npc.profile().uniqueId(), emoteIds);
      byte[] payloadData = constructPayloadData(EMOTE_API_MESSAGE_KEY, data.toString());

      // create a new plugin message outbound packet and schedule the payload data
      packetAdapter.createCustomPayloadPacket(LM_PLUGIN_CHANNEL, payloadData).schedule(player, npc);
    };
  }

  public static <W, P, I, E> @NotNull OutboundPacket<W, P, I, E> createStickerPacket(
    @NotNull PlatformPacketAdapter<W, P, I, E> packetAdapter,
    int... stickerIds
  ) {
    return (player, npc) -> {
      // construct the data we need to write
      JsonArray data = createIdJsonData(STICKER_ID_PROPERTY, npc.profile().uniqueId(), stickerIds);
      byte[] payloadData = constructPayloadData(STICKER_API_MESSAGE_KEY, data.toString());

      // create a new plugin message outbound packet and schedule the payload data
      packetAdapter.createCustomPayloadPacket(LM_PLUGIN_CHANNEL, payloadData).schedule(player, npc);
    };
  }

  private static byte[] constructPayloadData(@NotNull String apiMessageKey, @NotNull String data) {
    ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_BUFFER_ALLOCATION_BYTES);

    // put the message key, then the data
    buffer = BufferUtil.putString(buffer, apiMessageKey);
    buffer = BufferUtil.putString(buffer, data);

    // get the buffer content
    return BufferUtil.extractData(buffer);
  }

  private static @NotNull JsonArray createIdJsonData(@NotNull String idProperty, @NotNull UUID npcId, int... ids) {
    // put the id api data
    JsonArray array = new JsonArray();
    for (int id : ids) {
      JsonObject object = new JsonObject();
      object.addProperty(idProperty, id);
      object.addProperty("uuid", npcId.toString());
      array.add(object);
    }

    return array;
  }
}
