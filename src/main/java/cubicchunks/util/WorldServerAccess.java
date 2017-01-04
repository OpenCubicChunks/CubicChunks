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

import com.google.common.base.Throwables;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.WorldServer;

import java.lang.invoke.MethodHandle;
import java.util.HashSet;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WorldServerAccess {

    private static final MethodHandle ws_pendingTickListEntriesHashSet = ReflectionUtil.getFieldGetterHandle(WorldServer.class, "field_73064_N");
    private static final MethodHandle ws_pendingTickListEntriesThisTick = ReflectionUtil.getFieldGetterHandle(WorldServer.class, "field_94579_S");

    public static List<NextTickListEntry> getPendingTickListEntriesThisTick(WorldServer ws) {
        try {
            return (List<NextTickListEntry>) ws_pendingTickListEntriesThisTick.invoke(ws);
        } catch (Throwable throwable) {
            throw Throwables.propagate(throwable);
        }
    }

    public static HashSet<NextTickListEntry> getPendingTickListEntriesHashSet(WorldServer ws) {
        try {
            return (HashSet<NextTickListEntry>) ws_pendingTickListEntriesHashSet.invoke(ws);
        } catch (Throwable throwable) {
            throw Throwables.propagate(throwable);
        }
    }
}
