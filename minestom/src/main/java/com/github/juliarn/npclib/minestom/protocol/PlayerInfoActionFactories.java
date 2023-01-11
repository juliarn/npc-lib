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

package com.github.juliarn.npclib.minestom.protocol;

import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.profile.Profile;
import com.github.juliarn.npclib.api.profile.ProfileProperty;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.extensions.Extension;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.PlayerInfoPacket;
import org.jetbrains.annotations.NotNull;

final class PlayerInfoActionFactories {

  private PlayerInfoActionFactories() {
    throw new UnsupportedOperationException();
  }

  private static @NotNull CompletableFuture<Profile.Resolved> resolveProfile(
    @NotNull Npc<?, Player, ?, ?> npc,
    @NotNull Player player
  ) {
    return npc.settings().profileResolver().resolveNpcProfile(player, npc);
  }

  public static final class AddPlayerFactory implements PlayerInfoActionFactory {

    @Override
    public @NotNull CompletableFuture<Map.Entry<PlayerInfoPacket.Action, PlayerInfoPacket.Entry>> buildAction(
      @NotNull Npc<Instance, Player, ItemStack, Extension> npc,
      @NotNull Player player
    ) {
      return resolveProfile(npc, player).thenApply(profile -> {
        // convert the profile properties
        List<PlayerInfoPacket.AddPlayer.Property> properties = new ArrayList<>();
        for (ProfileProperty property : profile.properties()) {
          PlayerInfoPacket.AddPlayer.Property prop = new PlayerInfoPacket.AddPlayer.Property(
            property.name(),
            property.value(),
            property.signature());
          properties.add(prop);
        }

        // build the action
        PlayerInfoPacket.Entry addPlayerAction = new PlayerInfoPacket.AddPlayer(
          profile.uniqueId(),
          profile.name(),
          properties,
          GameMode.CREATIVE,
          20,
          null,
          null);
        return new AbstractMap.SimpleImmutableEntry<>(PlayerInfoPacket.Action.ADD_PLAYER, addPlayerAction);
      });
    }
  }

  public static final class RemovePlayerFactory implements PlayerInfoActionFactory {

    @Override
    public @NotNull CompletableFuture<Map.Entry<PlayerInfoPacket.Action, PlayerInfoPacket.Entry>> buildAction(
      @NotNull Npc<Instance, Player, ItemStack, Extension> npc,
      @NotNull Player player
    ) {
      return resolveProfile(npc, player).thenApply(profile -> {
        PlayerInfoPacket.Entry removePlayerAction = new PlayerInfoPacket.RemovePlayer(profile.uniqueId());
        return new AbstractMap.SimpleImmutableEntry<>(PlayerInfoPacket.Action.REMOVE_PLAYER, removePlayerAction);
      });
    }
  }
}
