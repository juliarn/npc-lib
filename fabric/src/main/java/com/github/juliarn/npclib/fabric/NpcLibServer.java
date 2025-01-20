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

package com.github.juliarn.npclib.fabric;

import static net.minecraft.server.command.CommandManager.literal;

import com.github.juliarn.npclib.api.NpcActionController;
import com.github.juliarn.npclib.api.Platform;
import com.github.juliarn.npclib.api.Position;
import com.github.juliarn.npclib.api.profile.Profile;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class NpcLibServer implements DedicatedServerModInitializer {

  public static final String MOD_ID = "npc-lib";
  private static MinecraftServer server;

  public static MinecraftServer getServer() {
    return server;
  }

  private final Platform<World, ServerPlayerEntity, ItemStack, Object> platform = FabricPlatform
    .minestomNpcPlatformBuilder()
    .actionController(NpcActionController.Builder::build)
    .extension(this)
    .build();

  @Override
  public void onInitializeServer() {
    ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
      NpcLibServer.server = server;
    });
    CommandRegistrationCallback.EVENT.register(
      (dispatcher, registryAccess, environment) -> dispatcher.register(literal("foo")
        .executes(context -> {
          // For versions below 1.19, replace "Text.literal" with "new LiteralText".
          // For versions below 1.20, remode "() ->" directly.
          context.getSource().sendFeedback(() -> Text.literal("Called /foo with no arguments"), false);

          var yo = platform
            .newNpcBuilder()
            .position(Position.position(0, 100, 0, "yo"))
            .profile(Profile.unresolved("outplay")).thenAccept(builder -> {
              var npc = builder.buildAndTrack();
              System.out.println("YO" + npc);
            });

          return 1;
        })));
  }
}
