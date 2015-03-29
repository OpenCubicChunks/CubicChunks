/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 * 
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package cubicchunks.accessors;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.ScheduledBlockTick;
import net.minecraft.world.WorldClient;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ServerChunkCache;
import cubicchunks.server.ServerCubeCache;

public class WorldServerAccessor {
	
	private static Field fieldScheduledTicksTreeSet;
	private static Field fieldScheduledTicksHashSet;
	private static Field fieldScheduledTicksNextTick;
	private static Field fieldChunkProvider;
	private static Field fieldPlayerManager;
	
	static {
		try {
			fieldScheduledTicksTreeSet = WorldServer.class.getDeclaredField("pendingTickListEntriesTreeSet");
			fieldScheduledTicksTreeSet.setAccessible(true);
			
			fieldScheduledTicksHashSet = WorldServer.class.getDeclaredField("pendingTickListEntriesHashSet");
			fieldScheduledTicksHashSet.setAccessible(true);
			
			fieldScheduledTicksNextTick = WorldServer.class.getDeclaredField("pendingTickListEntriesThisTick");
			fieldScheduledTicksNextTick.setAccessible(true);
			
			fieldChunkProvider = WorldServer.class.getDeclaredField("theChunkProviderServer");
			fieldChunkProvider.setAccessible(true);
			
			fieldPlayerManager = WorldServer.class.getDeclaredField("thePlayerManager");
			fieldPlayerManager.setAccessible(true);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static TreeSet<ScheduledBlockTick> getScheduledTicksTreeSet(WorldServer worldServer) {
		try {
			return (TreeSet<ScheduledBlockTick>)fieldScheduledTicksTreeSet.get(worldServer);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static Set<ScheduledBlockTick> getScheduledTicksHashSet(WorldServer worldServer) {
		try {
			return (Set<ScheduledBlockTick>)fieldScheduledTicksHashSet.get(worldServer);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<ScheduledBlockTick> getScheduledTicksThisTick(WorldServer worldServer) {
		try {
			return (List<ScheduledBlockTick>)fieldScheduledTicksNextTick.get(worldServer);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static ServerCubeCache getChunkProvider(WorldClient world) {
		try {
			return (ServerCubeCache)fieldChunkProvider.get(world);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static void setChunkCache(WorldServer world, ServerChunkCache val) {
		try {
			fieldChunkProvider.set(world, val);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static PlayerManager getPlayerManager(WorldClient world) {
		try {
			return (PlayerManager)fieldPlayerManager.get(world);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static void setPlayerManager(WorldServer world, PlayerManager val) {
		try {
			fieldPlayerManager.set(world, val);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
}
