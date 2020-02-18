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
package io.github.opencubicchunks.cubicchunks.core.util;

import io.github.opencubicchunks.cubicchunks.core.network.AbstractClientMessageHandler;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.network.AbstractClientMessageHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketUtils {

    private static final int MASK_6 = (1 << 6) - 1;
    private static final int MASK_7 = (1 << 7) - 1;

    public static void write(ByteBuf buf, BlockPos pos) {
        writeSignedVarInt(buf, pos.getX());
        writeSignedVarInt(buf, pos.getY());
        writeSignedVarInt(buf, pos.getZ());
    }

    public static BlockPos readBlockPos(ByteBuf buf) {
        return new BlockPos(readSignedVarInt(buf), readSignedVarInt(buf), readSignedVarInt(buf));
    }

    public static void write(ByteBuf buf, CubePos pos) {
        writeSignedVarInt(buf, pos.getX());
        writeSignedVarInt(buf, pos.getY());
        writeSignedVarInt(buf, pos.getZ());
    }

    public static CubePos readCubePos(ByteBuf buf) {
        return new CubePos(readSignedVarInt(buf), readSignedVarInt(buf), readSignedVarInt(buf));
    }

    /**
     * Writes signed int with variable length encoding, using at most 5 bytes.
     *
     * Unlike vanilla/forge one, this ensures that value and ~value are written using the same amount of bytes
     *
     * @param buf byte buffer
     * @param i integer to write
     */
    public static void writeSignedVarInt(ByteBuf buf, int i) {
        int signBit = (i >>> 31) << 6;
        int val = i < 0 ? ~i : i;
        assert val >= 0;

        writeVarIntByte(buf, (val & MASK_6) | signBit, (val >>= 6) > 0);
        while (val > 0) {
            writeVarIntByte(buf, (val & MASK_7), (val >>= 7) > 0);
        }
    }

    /**
     * Reads signed int with variable length encoding, using at most 5 bytes.
     *
     * @see PacketUtils#writeSignedVarInt(ByteBuf, int)
     * @param buf byte buffer
     * @return read integer
     */
    public static int readSignedVarInt(ByteBuf buf) {
        int val = 0;
        int b = buf.readUnsignedByte();
        boolean sign = ((b >> 6) & 1) != 0;

        val |= b & MASK_6;
        int shift = 6;
        while ((b & 0x80) != 0) {
            if (shift > Integer.SIZE) {
                throw new RuntimeException("VarInt too big");
            }
            b = buf.readUnsignedByte();
            val |= (b & MASK_7) << shift;
            shift += 7;
        }
        return sign ? ~val : val;
    }

    private static void writeVarIntByte(ByteBuf buf, int i, boolean hasMore) {
        buf.writeByte(i | (hasMore ? 0x80 : 0));
    }
}
