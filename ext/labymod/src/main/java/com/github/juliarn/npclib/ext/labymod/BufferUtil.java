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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

final class BufferUtil {

  // pre-computed var int byte lengths
  private static final int[] VAR_INT_BYTE_LENGTHS = new int[33];

  static {
    // pre-compute all var int byte lengths
    for (int i = 0; i <= 32; ++i) {
      VAR_INT_BYTE_LENGTHS[i] = (int) Math.ceil((31d - (i - 1)) / 7d);
    }
    // 0 is always one byte long
    VAR_INT_BYTE_LENGTHS[32] = 1;
  }

  private BufferUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull ByteBuffer ensureWriteable(@NotNull ByteBuffer buffer, int contentBytes) {
    if (buffer.remaining() >= contentBytes) {
      // enough space available in that buffer
      return buffer;
    } else {
      // not enough space - re-allocate a new buffer and return that
      int newSize = buffer.capacity() + contentBytes;
      ByteBuffer newBuffer = buffer.isDirect() ? ByteBuffer.allocateDirect(newSize) : ByteBuffer.allocate(newSize);

      // transfer the content of the old buffer into the new buffer
      buffer.flip();
      return newBuffer.put(buffer);
    }
  }

  public static @NotNull ByteBuffer putString(@NotNull ByteBuffer buffer, @NotNull String string) {
    // get the bytes of the buffer to write
    byte[] bytes = string.getBytes(StandardCharsets.UTF_8);

    // write the content
    ByteBuffer writeBuffer = putVarInt(buffer, bytes.length);
    writeBuffer = putBytes(writeBuffer, bytes);

    return writeBuffer;
  }

  public static @NotNull ByteBuffer putBytes(@NotNull ByteBuffer buffer, byte[] bytes) {
    // ensure that the target buffer has enough space to fit the bytes and write them
    ByteBuffer writeBuffer = ensureWriteable(buffer, bytes.length);
    return writeBuffer.put(bytes);
  }

  public static @NotNull ByteBuffer putVarInt(@NotNull ByteBuffer buffer, int value) {
    // ensure that the target buffer has enough space to fit the var int
    int varIntBytes = VAR_INT_BYTE_LENGTHS[Integer.numberOfLeadingZeros(value)];
    ByteBuffer writeBuffer = ensureWriteable(buffer, varIntBytes);

    // write the var int
    while (true) {
      if ((value & ~0x7F) == 0) {
        writeBuffer.put((byte) value);
        return writeBuffer;
      } else {
        writeBuffer.put((byte) ((value & 0x7F) | 0x80));
        value >>>= 7;
      }
    }
  }

  public static byte[] extractData(@NotNull ByteBuffer buffer) {
    if (buffer.hasArray()) {
      // buffer has a data array, use that
      byte[] data = buffer.array();
      return Arrays.copyOfRange(data, 0, buffer.position());
    } else {
      // store the old position to reset it
      int prevPos = buffer.position();

      // extract the data from the buffer
      byte[] data = new byte[prevPos];
      buffer.position(0);
      buffer.get(data).position(prevPos);

      // return the data we extracted
      return data;
    }
  }
}
