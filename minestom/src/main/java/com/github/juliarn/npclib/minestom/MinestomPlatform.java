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

package com.github.juliarn.npclib.minestom;

import com.github.juliarn.npclib.api.NpcActionController;
import com.github.juliarn.npclib.api.Platform;
import com.github.juliarn.npclib.common.platform.CommonPlatform;
import com.github.juliarn.npclib.common.platform.CommonPlatformBuilder;
import com.github.juliarn.npclib.minestom.protocol.MinestomProtocolAdapter;
import net.minestom.server.entity.Player;
import net.minestom.server.extensions.Extension;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class MinestomPlatform extends CommonPlatformBuilder<Instance, Player, ItemStack, Extension> {

  private MinestomPlatform() {
    this.extensionRequired = false;
  }

  public static @NotNull Platform.Builder<Instance, Player, ItemStack, Extension> minestomNpcPlatformBuilder() {
    return new MinestomPlatform();
  }

  @Override
  protected void prepareBuild() {
    // set the default task manager
    if (this.taskManager == null) {
      this.taskManager = MinestomPlatformTaskManager.taskManager();
    }

    // set the default version accessor
    if (this.versionAccessor == null) {
      this.versionAccessor = MinestomVersionAccessor.versionNameBased();
    }

    // set the default world accessor
    if (this.worldAccessor == null) {
      this.worldAccessor = MinestomWorldAccessor.uuidBased();
    }

    // set the default packet adapter
    if (this.packetAdapter == null) {
      this.packetAdapter = MinestomProtocolAdapter.minestomProtocolAdapter();
    }

    // set the default logger if no logger was provided
    if (this.logger == null) {
      this.logger = MinestomPlatformLogger.minestomLogger();
    }
  }

  @Override
  protected @NotNull Platform<Instance, Player, ItemStack, Extension> doBuild() {
    // check if we need an action controller
    NpcActionController actionController = null;
    if (this.actionControllerDecorator != null) {
      NpcActionController.Builder builder = MinestomActionController.actionControllerBuilder(
        this.eventBus,
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
