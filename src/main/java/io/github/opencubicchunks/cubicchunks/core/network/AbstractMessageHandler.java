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
package io.github.opencubicchunks.cubicchunks.core.network;

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client.INetHandlerPlayClient;
import io.github.opencubicchunks.cubicchunks.core.util.SideUtils;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IThreadListener;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class AbstractMessageHandler<T extends IMessage> implements IMessageHandler<T, IMessage> {

    public abstract void handleClientMessage(World world, EntityPlayer player, T message, MessageContext ctx);

    public abstract void handleServerMessage(EntityPlayer player, T message, MessageContext ctx);

    @Nullable @Override
    public final IMessage onMessage(T message, MessageContext ctx) {
        try {
            // converting to method reference will break on dedicated server
            @SuppressWarnings("Convert2MethodRef")
            IThreadListener taskQueue = SideUtils.<IThreadListener>getForSide(
                    () -> () -> Minecraft.getMinecraft(),
                    () -> () -> FMLCommonHandler.instance().getMinecraftServerInstance()
            );
            if (!taskQueue.isCallingFromMinecraftThread()) {
                taskQueue.addScheduledTask(() -> onMessage(message, ctx));
                return null;
            }
            World mainWorld = SideUtils.getForSide(
                    () -> ClientAccessProxy::getWorld,
                    () -> () -> FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0)
            );
            if (mainWorld == null) {
                CubicChunks.LOGGER.warn("Received packet when world doesn't exist!");
                return null; // there is no world, so we received packet after quitting world. Ignore it.
            }
            EntityPlayer player = SideUtils.getForSide(ctx,
                    () -> ClientAccessProxy::getPlayer,
                    () -> c -> c.getServerHandler().player
            );
            if (ctx.side.isClient()) {
                handleClientMessage(mainWorld, player, message, ctx);
            } else {
                handleServerMessage(player, message, ctx);
            }
            return null;
        } catch (Throwable t) {
            // catch *EVERYTHING* because Minecraft is dumb and will only print the stacktrace and continue
            // catch all and ask forge to shut down
            CubicChunks.LOGGER.catching(t);
            FMLCommonHandler.instance().exitJava(-1, false);
            throw t;
        }
    }

    private static class ClientAccessProxy {
        static EntityPlayer getPlayer(MessageContext c) {
            return c.side.isClient() ? Minecraft.getMinecraft().player : c.getServerHandler().player;
        }

        @Nullable static World getWorld() {
            return Minecraft.getMinecraft().getConnection() == null ? null :
                    ((INetHandlerPlayClient) Minecraft.getMinecraft().getConnection()).getWorld();
        }
    }
}
