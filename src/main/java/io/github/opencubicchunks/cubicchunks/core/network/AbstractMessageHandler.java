/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
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
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Abstract implementation od IMessageHandler that makes EntityPlayer available.
 * It also has separate methods for handling messages serverside and clientside.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class AbstractMessageHandler<T extends IMessage> implements IMessageHandler<T, IMessage> {

    // TODO: return void
    /**
     * Handle a message received on the client side
     *
     * @return a message to send back to the Server, or null if no reply is necessary
     */
    @Nullable @SideOnly(Side.CLIENT)
    public abstract IMessage handleClientMessage(EntityPlayer player, T message, MessageContext ctx);

    /**
     * Handle a message received on the server side
     *
     * @return a message to send back to the Client, or null if no reply is necessary
     */
    @Nullable
    public abstract IMessage handleServerMessage(EntityPlayer player, T message, MessageContext ctx);

    /*
    * Here is where I parse the side and get the player to pass on to the abstract methods.
    * This way it is immediately clear which side received the packet without having to
    * remember or check on which side it was registered and the player is immediately
    * available without a lengthy syntax.
    */
    @Nullable @Override
    public IMessage onMessage(T message, MessageContext ctx) {
        try {
            // due to compile-time issues, FML will crash if you try to use Minecraft.getMinecraft() here,
            // even when you restrict this code to the client side and before the code is ever accessed;
            // a solution is to use proxy classes to get the player.
            if (ctx.side.isClient()) {
                // the only reason to check side here is to use our more aptly named handling methods
                // client side proxy will return the client side EntityPlayer
                return handleClientMessage(CubicChunks.proxy.getPlayerEntity(ctx), message, ctx);
            }
            // server side proxy will return the server side EntityPlayer
            return handleServerMessage(CubicChunks.proxy.getPlayerEntity(ctx), message, ctx);
        } catch (ThreadQuickExitException ex) {
            // ignore
            return null;
        }
    }
}
