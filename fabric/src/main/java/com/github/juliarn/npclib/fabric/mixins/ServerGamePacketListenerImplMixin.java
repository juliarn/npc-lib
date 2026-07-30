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

package com.github.juliarn.npclib.fabric.mixins;

import com.github.juliarn.npclib.fabric.controller.FabricActionControllerEvents;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

  @Shadow
  public abstract ServerPlayer getPlayer();

  @Inject(
    method = "handlePlayerInput",
    at = @At(
      value = "INVOKE",
      shift = At.Shift.AFTER,
      target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V"
    )
  )
  public void npc_lib$handlePlayerInput(ServerboundPlayerInputPacket packet, CallbackInfo ci) {
    var player = this.getPlayer();
    var sentInput = packet.input();
    var lastInput = player.getLastClientInput();
    if (lastInput.shift() != sentInput.shift()) {
      var invoker = FabricActionControllerEvents.SERVER_PLAYER_TOGGLE_SNEAK.invoker();
      invoker.toggleSneak(player, sentInput.shift());
    }
  }

  @Inject(method = "handleAnimate", at = @At("TAIL"))
  public void npc_lib$handleAnimate(ServerboundSwingPacket packet, CallbackInfo ci) {
    var player = this.getPlayer();
    var invoker = FabricActionControllerEvents.SERVER_PLAYER_HAND_SWING.invoker();
    invoker.swingHand(player);
  }

  @Inject(
    method = "handleMovePlayer",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;getYRot(F)F"
    ),
    slice = @Slice(
      from = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;hasClientLoaded()Z"),
      to = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;updateAwaitingTeleport()Z")
    )
  )
  public void npc_lib$handleMovePlayer(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
    // ensure that the packet contains at least position and rotation
    var hasPos = packet.hasPosition();
    var hasRot = packet.hasRotation();
    if (!hasPos && !hasRot) {
      return;
    }

    // get the new target position, null in case it's not provided by the packet
    var player = this.getPlayer();
    var posToX = packet.getX(player.getX());
    var posToY = packet.getY(player.getY());
    var posToZ = packet.getZ(player.getZ());
    var posTo = hasPos ? new Vec3(posToX, posToY, posToZ) : null;

    // get the new target rotation, null in case it's not provided by the packet
    var rotToX = packet.getXRot(player.getXRot());
    var rotToY = packet.getYRot(player.getYRot());
    var rotTo = hasRot ? new Vec2(rotToX, rotToY) : null;

    // invoke the player move event
    var invoker = FabricActionControllerEvents.SERVER_PLAYER_MOVE.invoker();
    invoker.move(player, posTo, rotTo);
  }

  @Inject(
    method = "handleMoveVehicle",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/world/entity/Entity;getControllingPassenger()Lnet/minecraft/world/entity/LivingEntity;"
    )
  )
  public void npc_lib$handleMoveVehicle(ServerboundMoveVehiclePacket packet, CallbackInfo ci) {
    var player = this.getPlayer();
    var rootVehicle = player.getRootVehicle();

    // get the target position, set to null in case it didn't change
    var posFrom = rootVehicle.position();
    var posTo = packet.position();
    if (posTo.equals(posFrom)) {
      posTo = null;
    }

    // get the target rotation, set to null in case it didn't change
    var rotFrom = rootVehicle.getRotationVector();
    var rotTo = new Vec2(packet.xRot(), packet.yRot());
    if (rotTo.equals(rotFrom)) {
      rotTo = null;
    }

    // fire player move event in case position or rotation changed
    if (posTo != null || rotTo != null) {
      var invoker = FabricActionControllerEvents.SERVER_PLAYER_MOVE.invoker();
      invoker.move(player, posTo, rotTo);
    }
  }

  @Inject(
    method = "handleInteract",
    at = @At(
      value = "INVOKE",
      shift = At.Shift.AFTER,
      target = "Lnet/minecraft/server/level/ServerPlayer;setShiftKeyDown(Z)V"
    ),
    cancellable = true
  )
  public void npc_lib$handleInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
    var player = this.getPlayer();
    var invoker = FabricActionControllerEvents.SERVER_PLAYER_ENTITY_INTERACT.invoker();
    if (invoker.interact(packet.entityId(), false, player, packet.hand())) {
      ci.cancel();
    }
  }

  @Inject(
    method = "handleAttack",
    at = @At(
      value = "INVOKE",
      shift = At.Shift.AFTER,
      target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;hasClientLoaded()Z"
    ),
    cancellable = true
  )
  public void npc_lib$handleAttack(ServerboundAttackPacket packet, CallbackInfo ci) {
    var player = this.getPlayer();
    var invoker = FabricActionControllerEvents.SERVER_PLAYER_ENTITY_INTERACT.invoker();
    if (invoker.interact(packet.entityId(), true, player, InteractionHand.MAIN_HAND)) {
      ci.cancel();
    }
  }
}
