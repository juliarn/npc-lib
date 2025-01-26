/*
 * This file is part of npc-lib, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022-2025 Julian M., Pasqual K. and contributors
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

package com.github.juliarn.npclib.fabric.controller;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Internal event for notifying the action controller about player actions.
 */
@ApiStatus.Internal
public interface FabricActionControllerEvents {

  Event<ServerPlayerPreLevelChange> PRE_SERVER_PLAYER_LEVEL_CHANGE = EventFactory.createArrayBacked(
    ServerPlayerPreLevelChange.class,
    callbacks -> (player, oldLevel, newLevel) -> {
      for (var callback : callbacks) {
        callback.preLevelChange(player, oldLevel, newLevel);
      }
    }
  );

  Event<ServerPlayerToggleSneak> SERVER_PLAYER_TOGGLE_SNEAK = EventFactory.createArrayBacked(
    ServerPlayerToggleSneak.class,
    callbacks -> (player, sneak) -> {
      for (var callback : callbacks) {
        callback.toggleSneak(player, sneak);
      }
    }
  );

  Event<ServerPlayerHandSwing> SERVER_PLAYER_HAND_SWING = EventFactory.createArrayBacked(
    ServerPlayerHandSwing.class,
    callbacks -> player -> {
      for (var callback : callbacks) {
        callback.swingHand(player);
      }
    }
  );

  Event<ServerPlayerDisconnect> SERVER_PLAYER_DISCONNECT = EventFactory.createArrayBacked(
    ServerPlayerDisconnect.class,
    callbacks -> player -> {
      for (var callback : callbacks) {
        callback.disconnect(player);
      }
    }
  );

  Event<ServerPlayerMove> SERVER_PLAYER_MOVE = EventFactory.createArrayBacked(
    ServerPlayerMove.class,
    callbacks -> (player, posTo, rotTo) -> {
      for (var callback : callbacks) {
        callback.move(player, posTo, rotTo);
      }
    }
  );

  /**
   * Called before a server player changes the level.
   */
  @FunctionalInterface
  interface ServerPlayerPreLevelChange {

    void preLevelChange(@NotNull ServerPlayer player, @Nullable ServerLevel oldLevel, @NotNull ServerLevel newLevel);
  }

  /**
   * Called when a server player starts or stops sprinting.
   */
  @FunctionalInterface
  interface ServerPlayerToggleSneak {

    void toggleSneak(@NotNull ServerPlayer player, boolean sneaking);
  }

  /**
   * Called when a server player swings his hand (left click).
   */
  @FunctionalInterface
  interface ServerPlayerHandSwing {

    void swingHand(@NotNull ServerPlayer player);
  }

  /**
   * Called when a server player disconnects.
   */
  @FunctionalInterface
  interface ServerPlayerDisconnect {

    void disconnect(@NotNull ServerPlayer player);
  }

  /**
   * Called when a server player moves. Note that the pos and rot is only given if it was changed by the client.
   */
  @FunctionalInterface
  interface ServerPlayerMove {

    void move(@NotNull ServerPlayer player, @Nullable Vec3 posTo, @Nullable Vec2 rotTo);
  }
}
