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
package cubicchunks.asm;

import cubicchunks.CubicChunkSystem;
import cubicchunks.CubicChunks;
import cubicchunks.util.ReflectionUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RegionRenderCache;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public class AsmRender {
	private static CubicChunkSystem cc;

	private static MethodHandle getWorldRenderGlobal = getMethodHandleGetWorld();
	private static MethodHandle chunkX = getChunkXMethodHandle();
	private static MethodHandle chunkZ = getChunkZMethodHandle();
	private static MethodHandle chunkArray = getChunkArrayMethodHandle();

	private static MethodHandle getChunkXMethodHandle() {
		Field f = ReflectionHelper.findField(ChunkCache.class, "chunkX", "field_72818_a");
		f.setAccessible(true);
		try {
			MethodHandle mh = MethodHandles.lookup().unreflectGetter(f);
			return mh;
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	private static MethodHandle getChunkZMethodHandle() {
		Field f = ReflectionHelper.findField(ChunkCache.class, "chunkZ", "field_72816_b");
		f.setAccessible(true);
		try {
			MethodHandle mh = MethodHandles.lookup().unreflectGetter(f);
			return mh;
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	private static MethodHandle getChunkArrayMethodHandle() {
		Field f = ReflectionHelper.findField(ChunkCache.class, "chunkArray", "field_72817_c");
		f.setAccessible(true);
		try {
			MethodHandle mh = MethodHandles.lookup().unreflectGetter(f);
			return mh;
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	private static MethodHandle getMethodHandleGetWorld() {
		Field f = ReflectionUtil.findFieldNonStatic(RenderGlobal.class, WorldClient.class);
		f.setAccessible(true);
		try {
			MethodHandle mh = MethodHandles.lookup().unreflectGetter(f);
			return mh;
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	public static final boolean updateChunkPositions(ViewFrustum _this) {
		return cc.frustumViewUpdateChunkPositions(_this);
	}

	public static final ClassInheritanceMultiMap getEntityList(Chunk chunk, int y) {
		return cc.getEntityStore(chunk, y);
	}

	public static final RenderChunk getRenderChunk(ViewFrustum vf, BlockPos pos) {
		return cc.getChunkSectionRenderer(vf, pos);
	}

	public static final void registerChunkSystem(CubicChunkSystem cc) {
		AsmRender.cc = cc;
	}

	public static final IBlockState blockFromCache(RegionRenderCache cache, BlockPos pos) throws Throwable {
		if(cache.getWorldType() != CubicChunks.CC_WORLD_TYPE) {
			return null;
		}
		int i = (pos.getX() >> 4) - (Integer)chunkX.invoke(cache);
		int j = (pos.getZ() >> 4) - (Integer)chunkZ.invoke(cache);
		return ((Chunk[][])chunkArray.invoke(cache))[i][j].getBlockState(pos);
	}
}
