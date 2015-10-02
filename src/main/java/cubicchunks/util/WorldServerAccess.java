/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 Tall Worlds
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

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class WorldServerAccess {
	private static final Field ws_pendingTickListEntriesHashSet = 
			ReflectionHelper.findField(WorldServer.class, "pendingTickListEntriesHashSet", "field_73064_N");
	private static final Field ws_pendingTickListEntriesThisTick = 
			ReflectionHelper.findField(WorldServer.class, "pendingTickListEntriesThisTick", "field_94579_S");
	
	public static final List getPendingTickListEntriesThisTick(WorldServer ws) {
		return ReflectionUtil.get(ws, ws_pendingTickListEntriesThisTick, List.class);
	}
	
	public static final HashSet getPendingTickListEntriesHashSet(WorldServer ws) {
		return ReflectionUtil.get(ws, ws_pendingTickListEntriesHashSet, HashSet.class);
	}
}
