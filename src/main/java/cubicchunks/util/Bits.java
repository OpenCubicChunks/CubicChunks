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

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Bits {

    public static long packUnsignedToLong(int unsigned, int size, int offset) {
        // same as signed
        return packSignedToLong(unsigned, size, offset);
    }

    public static long packSignedToLong(int signed, int size, int offset) {
        long result = signed & getMask(size);
        return result << offset;
    }

    public static int packUnsignedToInt(int unsigned, int size, int offset) {
        // same as signed
        return packSignedToInt(unsigned, size, offset);
    }

    public static int packSignedToInt(int signed, int size, int offset) {
        int result = signed & getMask(size);
        return result << offset;
    }

    public static int unpackUnsigned(long packed, int size, int offset) {
        packed = packed >> offset;
        return (int) packed & getMask(size);
    }

    public static int unpackSigned(long packed, int size, int offset) {
        // first, scrollOffset to the far left and back so we can preserve the two's complement
        int complementOffset = 64 - offset - size;
        packed = packed << complementOffset >> complementOffset;

        // then unpack the integer
        packed = packed >> offset;
        return (int) packed;
    }

    public static int unpackUnsigned(int packed, int size, int offset) {
        packed = packed >> offset;
        return packed & getMask(size);
    }

    public static int unpackSigned(int packed, int size, int offset) {
        // first, scrollOffset to the far left and back so we can preserve the two's complement
        int complementOffset = 64 - offset - size;
        packed = packed << complementOffset >> complementOffset;

        // then unpack the integer
        packed = packed >> offset;
        return packed;
    }

    public static int getMask(int size) {
        // mask sizes of 0 and 32 are not allowed
        // we could allow them, but I don't want to add conditionals so this method stays very fast
        assert (size > 0 && size < 32);
        return 0xffffffff >>> (32 - size);
    }

    public static int getMinSigned(int size) {
        return -(1 << (size - 1));
    }

    public static int getMaxSigned(int size) {
        return (1 << (size - 1)) - 1;
    }

    public static int getMaxUnsigned(int size) {
        return (1 << size) - 1;
    }
}
