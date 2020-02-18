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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PacketBuffer.class)
public abstract class MixinPacketBufferBlockPosWrite {

    @Shadow public abstract long readLong();

    @Shadow public abstract ByteBuf writeLong(long p_writeLong_1_);

    @Shadow public abstract int readVarInt();

    @Shadow public abstract PacketBuffer writeVarInt(int input);

    /**
     * @return block position from packet buffer
     * @author Barteks2x
     * @reason BlockPos.toLong works only between y=-2048 and y=2047
     */
    @Overwrite
    public BlockPos readBlockPos() {
        long data = this.readLong();
        BlockPos pos = BlockPos.fromLong(data);
        if (pos.getY() == -2048) {
            return new BlockPos(pos.getX(), this.readVarInt(), pos.getZ());
        } else {
            return pos; 
        }
    }

    /**
     * @param pos block position to write
     * @return this
     * @author Barteks2x
     * @reason BlockPos.toLong works only between y=-2048 and y=2047
     */
    @Overwrite
    public PacketBuffer writeBlockPos(BlockPos pos) {
        int y = pos.getY();
        if (y <= 2047 && y >= -2047) {
            this.writeLong(pos.toLong());
        } else {
            this.writeLong(new BlockPos(pos.getX(), -2048, pos.getZ()).toLong());
            this.writeVarInt(y);
        }
        return (PacketBuffer) (Object) this;
    }
}
