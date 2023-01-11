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

package com.github.juliarn.npclib.bukkit;

import com.github.juliarn.npclib.api.NpcActionController;
import com.github.juliarn.npclib.api.Platform;
import com.github.juliarn.npclib.api.log.PlatformLogger;
import com.github.juliarn.npclib.bukkit.protocol.BukkitProtocolAdapter;
import com.github.juliarn.npclib.common.platform.CommonPlatform;
import com.github.juliarn.npclib.common.platform.CommonPlatformBuilder;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class BukkitPlatform extends CommonPlatformBuilder<World, Player, ItemStack, Plugin> {

  private BukkitPlatform() {
  }

  public static @NotNull BukkitPlatform bukkitNpcPlatformBuilder() {
    return new BukkitPlatform();
  }

  @Override
  protected void prepareBuild() {
    // set the profile resolver to a native platform one if not given
    if (this.profileResolver == null) {
      this.profileResolver = BukkitProfileResolver.profileResolver();
    }

    // set the default task manager
    if (this.taskManager == null) {
      this.taskManager = BukkitPlatformTaskManager.taskManager(this.extension);
    }

    // set the default version accessor
    if (this.versionAccessor == null) {
      this.versionAccessor = BukkitVersionAccessor.versionAccessor();
    }

    // set the default world accessor
    if (this.worldAccessor == null) {
      this.worldAccessor = BukkitWorldAccessor.worldAccessor();
    }

    // set the default packet adapter
    if (this.packetAdapter == null) {
      this.packetAdapter = BukkitProtocolAdapter.packetAdapter();
    }

    // set the default logger if no logger was provided
    if (this.logger == null) {
      this.logger = PlatformLogger.fromJul(this.extension.getLogger());
    }
  }

  @Override
  protected @NotNull Platform<World, Player, ItemStack, Plugin> doBuild() {
    // check if we need an action controller
    NpcActionController actionController = null;
    if (this.actionControllerDecorator != null) {
      NpcActionController.Builder builder = BukkitActionController.actionControllerBuilder(
        this.extension,
        this.eventBus,
        this.versionAccessor,
        this.npcTracker);
      this.actionControllerDecorator.accept(builder);
      actionController = builder.build();
    }

    // build the platform
    return new CommonPlatform<>(
      this.debug,
      this.extension,
      this.logger,
      this.npcTracker,
      this.profileResolver,
      this.taskManager,
      actionController,
      this.versionAccessor,
      this.eventBus,
      this.worldAccessor,
      this.packetAdapter);
  }
}
