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
package cubicchunks.network;

import cubicchunks.util.IntRange;
import cubicchunks.util.PacketUtils;
import cubicchunks.world.CubicWorld;
import cubicchunks.world.CubicWorldClient;
import cubicchunks.world.type.CubicWorldType;
import io.netty.buffer.ByteBuf;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PacketCubicWorldData implements IMessage {

    private boolean isCubicWorld;
    private int minHeight;
    private int maxHeight;
    private int minGenerationHeight;
    private int maxGenerationHeight;

    public PacketCubicWorldData() {
    }

    public PacketCubicWorldData(WorldServer world) {
        this.minHeight = 0;
        this.maxHeight = 256;
        if (((CubicWorld) world).isCubicWorld()) {
            this.isCubicWorld = true;
            this.minHeight = ((CubicWorld) world).getMinHeight();
            this.maxHeight = ((CubicWorld) world).getMaxHeight();
            if (world.getWorldType() instanceof CubicWorldType) {
                CubicWorldType type = (CubicWorldType) world.getWorldType();
                IntRange range = type.calculateGenerationHeightRange(world);
                this.minGenerationHeight = range.getMin();
                this.maxGenerationHeight = range.getMax();
            } else {
                this.minGenerationHeight = 0;
                this.maxGenerationHeight = 256;
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.isCubicWorld = buf.readBoolean();
        this.minHeight = buf.readInt();
        this.maxHeight = buf.readInt();
        this.minGenerationHeight = buf.readInt();
        this.maxGenerationHeight = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.isCubicWorld);
        buf.writeInt(this.minHeight);
        buf.writeInt(this.maxHeight);
        buf.writeInt(this.minGenerationHeight);
        buf.writeInt(this.maxGenerationHeight);
    }

    public boolean isCubicWorld() {
        return this.isCubicWorld;
    }

    public int getMinHeight() {
        return this.minHeight;
    }

    public int getMaxHeight() {
        return this.maxHeight;
    }

    public int getMinGenerationHeight() {
        return minGenerationHeight;
    }

    public int getMaxGenerationHeight() {
        return maxGenerationHeight;
    }

    public static class Handler extends AbstractClientMessageHandler<PacketCubicWorldData> {

        @Nullable @Override
        public IMessage handleClientMessage(EntityPlayer player, PacketCubicWorldData message, MessageContext ctx) {
            PacketUtils.ensureMainThread(this, player, message, ctx);

            if (Minecraft.getMinecraft().getConnection() != null) {
                WorldClient world = Minecraft.getMinecraft().getConnection().clientWorldController;
                // initialize only if sending packet about cubic world, but not when already initialized
                if (message.isCubicWorld() && !((CubicWorld) world).isCubicWorld()) {
                    ((CubicWorldClient) world).initCubicWorldClient(
                            new IntRange(message.getMinHeight(), message.getMaxHeight()),
                            new IntRange(message.getMinGenerationHeight(), message.getMaxGenerationHeight())
                    );
                }
            }
            return null;
        }
    }
}
