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
package cubicchunks.util;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AddressTools {

    public static int getLocalAddress(int localX, int localY, int localZ) {
        return (Bits.packUnsignedToInt(localX, 4, 0)
                | Bits.packUnsignedToInt(localZ, 4, 4)
                | Bits.packUnsignedToInt(localY, 4, 8));
    }

    public static int getLocalAddress(int localX, int localZ) {
        return (Bits.packUnsignedToInt(localX, 4, 0)
                | Bits.packUnsignedToInt(localZ, 4, 4));
    }

    /**
     * Unpacks localX from packed address. Works for both, x/y/z and x/z version
     */
    public static int getLocalX(int localAddress) {
        return Bits.unpackUnsigned(localAddress, 4, 0);
    }

    /**
     * Unpacks localY from packed address.
     */
    public static int getLocalY(int localAddress) {
        return Bits.unpackUnsigned(localAddress, 4, 8);
    }

    /**
     * Unpacks localZ from packed address. Works for both, x/y/z and x/z version
     */
    public static int getLocalZ(int localAddress) {
        return Bits.unpackUnsigned(localAddress, 4, 4);
    }

    public static int getLocalAddress(BlockPos pos) {
        return getLocalAddress(pos.getX(), pos.getY(), pos.getZ());
    }
}
