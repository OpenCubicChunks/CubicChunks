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
package cubicchunks.asm.mixin.fixes.common;

import cubicchunks.util.Bits;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PacketBuffer.class)
public abstract class MixinPacketBufferBlockPosWrite {

    // Bit pattern in vanilla
    // xxxxxxxx xxxxxxxx xxxxxxxx xxYYYYYY   YYYYYYzz zzzzzzzz zzzzzzzz zzzzzzzz
    // our bit pattern, where F is our new flag:
    // xxxxxxxx xxxxxxxx xxxxxxxx xxFyyyyy   yyyyyyzz zzzzzzzz zzzzzzzz zzzzzzzz

    // mostly copied from BlockPos
    private static final int XZ_SIZE = 26;
    private static final int Y_SIZE_VANILLA = 12;
    private static final int Y_SIZE_MOD = Y_SIZE_VANILLA - 1;

    private static final int FLAG_SHIFT = XZ_SIZE + Y_SIZE_MOD;
    private static final int Y_SHIFT = XZ_SIZE;
    private static final int X_SHIFT = XZ_SIZE + Y_SIZE_VANILLA;

    private static final int MAX_Y = (1 << Y_SIZE_MOD) - 1;
    private static final int MIN_Y = ~MAX_Y;

    @Shadow public abstract long readLong();

    @Shadow public abstract ByteBuf writeLong(long p_writeLong_1_);

    @Shadow public abstract int readVarInt();

    @Shadow public abstract PacketBuffer writeVarInt(int input);

    /**
     * @author Barteks2x
     * @reason BlockPos.toLong works only between y=-2048 and y=2047
     */
    @Overwrite
    public BlockPos readBlockPos() {
        long data = this.readLong();
        // if custom flag is 1 - there is more data coming
        if (((data >>> FLAG_SHIFT) & 1) != 0) {
            int yMSB = Bits.unpackSigned(this.readVarInt(), Integer.SIZE - Y_SIZE_MOD, 0);
            int x = Bits.unpackSigned(data, XZ_SIZE, X_SHIFT);
            int z = Bits.unpackSigned(data, XZ_SIZE, 0);
            int yLSB = Bits.unpackUnsigned(data, Y_SIZE_MOD, Y_SHIFT);
            int y = yLSB | yMSB << Y_SIZE_MOD;
            return new BlockPos(x, y, z);
        } else {
            return BlockPos.fromLong(data);
        }
    }

    /**
     * @author Barteks2x
     * @reason BlockPos.toLong works only between y=-2048 and y=2047
     */
    @Overwrite
    public PacketBuffer writeBlockPos(BlockPos pos) {
        int y = pos.getY();
        if (y <= MAX_Y && y >= MIN_Y) {
            this.writeLong(pos.toLong());
        } else {
            int yLSB_flagged = y & ((1 << Y_SIZE_MOD) - 1) | 1 << Y_SIZE_MOD;
            int yMSB = y >>> Y_SIZE_MOD;
            this.writeLong(new BlockPos(pos.getX(), yLSB_flagged, pos.getZ()).toLong());
            this.writeVarInt(yMSB);
        }
        return (PacketBuffer) (Object) this;
    }
}
