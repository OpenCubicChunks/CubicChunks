/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient.ICPacketPlayer;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient.ICPacketPlayerDigging;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient.ICPacketPlayerTryUseItemOnBlock;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient.ICPacketTabComplete;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient.ICPacketUpdateSign;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient.ICPacketVehicleMove;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.core.server.VanillaNetworkHandler;
import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.network.play.client.CPacketUpdateSign;
import net.minecraft.network.play.client.CPacketVehicleMove;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayServer.class)
public class MixinNetHandlerPlayServer {

    @Shadow public EntityPlayerMP player;

    @Shadow private Vec3d targetPos;

    @Shadow private int teleportId;

    @Inject(method = "processCustomPayload",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;"
                             + "Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V"))
    public void preprocessPacket(CPacketCustomPayload packet, CallbackInfo ci) {
        if (!CubicChunksConfig.allowVanillaClients || !"MC|Brand".equals(packet.getChannelName())) {
            return;
        }
        PacketBuffer packetbuffer = packet.getBufferData();
        if (packetbuffer.readString(32767).contains("Geyser")) {
            VanillaNetworkHandler.addBedrockPlayer(this.player);

            if (CubicChunksConfig.vanillaClients.horizontalSlices && CubicChunksConfig.vanillaClients.horizontalSlicesBedrockOnly) {
                //notify vanilla handler, because otherwise bedrock players will log in at the wrong location as the client brand
                //is sent later in the handshake than the first call to updatePlayerPosition
                WorldServer world = (WorldServer) player.world;
                if (!((ICubicWorld) world).isCubicWorld()) {
                    return;
                }
                VanillaNetworkHandler vanillaHandler = ((ICubicWorldInternal.Server) world).getVanillaNetworkHandler();
                vanillaHandler.updatePlayerPosition((PlayerCubeMap) world.getPlayerChunkMap(), this.player,
                        new CubePos(Coords.blockToCube(player.posX), Coords.blockToCube(player.posY), Coords.blockToCube(player.posZ)));
            }
        }
        packetbuffer.resetReaderIndex(); // reset buffer position in case another mod tries to access the client brand the same way
    }

    @Inject(method = "processPlayerDigging",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;"
                            + "Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V"))
    public void preprocessPacket(CPacketPlayerDigging packetIn, CallbackInfo ci) {
        WorldServer world = (WorldServer) player.world;
        if (!((ICubicWorld) world).isCubicWorld()) {
            return;
        }
        VanillaNetworkHandler vanillaHandler = ((ICubicWorldInternal.Server) world).getVanillaNetworkHandler();
        boolean hasCC = vanillaHandler.hasCubicChunks(player);
        if (!hasCC) {
            ((ICPacketPlayerDigging) packetIn).setPosition(vanillaHandler.modifyPositionC2S(packetIn.getPosition(), player));
        }
    }


    @Inject(method = "processPlayer",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;"
                            + "Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V"))
    public void preprocessPacket(CPacketPlayer packet, CallbackInfo ci) {
        WorldServer world = (WorldServer) player.world;
        if (!((ICubicWorld) world).isCubicWorld()) {
            return;
        }
        VanillaNetworkHandler vanillaHandler = ((ICubicWorldInternal.Server) world).getVanillaNetworkHandler();
        boolean hasCC = vanillaHandler.hasCubicChunks(player);
        if (!hasCC) {
            ICPacketPlayer p = (ICPacketPlayer) packet;
            BlockPos offset = vanillaHandler.getC2SOffset(player);
            p.setX(p.getX() - offset.getX());
            p.setY(p.getY() - offset.getY());
            p.setZ(p.getZ() - offset.getZ());
        }
    }


    @Inject(method = "processTryUseItemOnBlock",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;"
                            + "Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V"))
    private void preprocessPacket(CPacketPlayerTryUseItemOnBlock packetIn, CallbackInfo ci) {
        WorldServer world = (WorldServer) player.world;
        if (!((ICubicWorld) world).isCubicWorld()) {
            return;
        }
        VanillaNetworkHandler vanillaHandler = ((ICubicWorldInternal.Server) world).getVanillaNetworkHandler();
        boolean hasCC = vanillaHandler.hasCubicChunks(player);
        if (!hasCC) {
            ((ICPacketPlayerTryUseItemOnBlock) packetIn).setPosition(vanillaHandler.modifyPositionC2S(packetIn.getPos(), player));
        }
    }


    @Inject(method = "processTabComplete",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;"
                            + "Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V"))
    private void preprocessPacket(CPacketTabComplete packetIn, CallbackInfo ci) {
        WorldServer world = (WorldServer) player.world;
        if (!((ICubicWorld) world).isCubicWorld()) {
            return;
        }
        VanillaNetworkHandler vanillaHandler = ((ICubicWorldInternal.Server) world).getVanillaNetworkHandler();
        boolean hasCC = vanillaHandler.hasCubicChunks(player);
        if (!hasCC) {
            BlockPos targetBlock = packetIn.getTargetBlock();
            if (targetBlock != null) {
                ((ICPacketTabComplete) packetIn).setTargetBlock(vanillaHandler.modifyPositionC2S(targetBlock, player));
            }
        }
    }


    @Inject(method = "processUpdateSign",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;"
                            + "Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V"))
    private void preprocessPacket(CPacketUpdateSign packetIn, CallbackInfo ci) {
        WorldServer world = (WorldServer) player.world;
        if (!((ICubicWorld) world).isCubicWorld()) {
            return;
        }
        VanillaNetworkHandler vanillaHandler = ((ICubicWorldInternal.Server) world).getVanillaNetworkHandler();
        boolean hasCC = vanillaHandler.hasCubicChunks(player);
        if (!hasCC) {
            ((ICPacketUpdateSign) packetIn).setPos(vanillaHandler.modifyPositionC2S(packetIn.getPosition(), player));
        }
    }


    @Inject(method = "processVehicleMove",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;"
                            + "Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V"))
    private void preprocessPacket(CPacketVehicleMove packetIn, CallbackInfo ci) {
        WorldServer world = (WorldServer) player.world;
        if (!((ICubicWorld) world).isCubicWorld()) {
            return;
        }
        VanillaNetworkHandler vanillaHandler = ((ICubicWorldInternal.Server) world).getVanillaNetworkHandler();
        boolean hasCC = vanillaHandler.hasCubicChunks(player);
        if (!hasCC) {
            ICPacketVehicleMove p = (ICPacketVehicleMove) packetIn;
            BlockPos offset = vanillaHandler.getC2SOffset(player);
            p.setX(packetIn.getX() - offset.getX());
            p.setY(packetIn.getY() - offset.getY());
            p.setZ(packetIn.getZ() - offset.getZ());
        }
    }

    @Inject(method = "processConfirmTeleport",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;"
                    + "Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V"), cancellable = true)
    public void preprocessTeleportConfirm(CPacketConfirmTeleport packetIn, CallbackInfo ci) {
        if (!CubicChunksConfig.allowVanillaClients) {
            return;
        }
        WorldServer world = (WorldServer) player.world;
        if (!((ICubicWorld) world).isCubicWorld()) {
            return;
        }
        VanillaNetworkHandler vanillaHandler = ((ICubicWorldInternal.Server) world).getVanillaNetworkHandler();
        boolean hasCC = vanillaHandler.hasCubicChunks(player);
        if (!hasCC) {
            if (vanillaHandler.receiveOffsetUpdateConfirm(player, packetIn.getTeleportId())) {
                ci.cancel();
            }
        }
    }

    @ModifyVariable(method = "sendPacket", at = @At("HEAD"), argsOnly = true)
    private Packet<?> onSendPacket(Packet<?> packetIn) {
        if (!CubicChunksConfig.allowVanillaClients) {
            return packetIn;
        }
        World world = this.player.world;
        if (!((ICubicWorld) world).isCubicWorld()) {
            return packetIn;
        }
        VanillaNetworkHandler vanillaHandler = ((ICubicWorldInternal.Server) world).getVanillaNetworkHandler();
        if (packetIn instanceof IPositionPacket) {
            if (!vanillaHandler.hasCubicChunks(player)) {
                BlockPos targetOffset = vanillaHandler.getS2COffset(player);
                // we have to sometimes copy the packet because MC may attempt to send the same packet object
                // to multiple players
                if (((IPositionPacket) packetIn).hasPosOffset()) {
                    packetIn = copyPacket(packetIn);
                }
                ((IPositionPacket) packetIn).setPosOffset(targetOffset);
                return packetIn;
            } else if (((IPositionPacket) packetIn).hasPosOffset()) {
                return copyPacket(packetIn);
            }
        }
        return packetIn;
    }

    private Packet<?> copyPacket(Packet<?> packetIn) {
        // TODO: make this faster
        return VanillaNetworkHandler.copyPacket(packetIn);
    }
}
