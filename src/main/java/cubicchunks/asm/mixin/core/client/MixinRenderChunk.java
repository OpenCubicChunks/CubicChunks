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
package cubicchunks.asm.mixin.core.client;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import cubicchunks.client.CubeProviderClient;
import cubicchunks.client.IRenderChunk;
import cubicchunks.client.RenderVariables;
import cubicchunks.util.Coords;
import cubicchunks.world.ICubicWorldClient;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
/**
 * Fixes renderEntities crashing when rendering cubes
 * that are not at existing array index in chunk.getEntityLists(),
 * <p>
 * Allows to render cubes outside of 0..256 height range.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(RenderChunk.class)
public abstract class MixinRenderChunk implements IRenderChunk {
    
    @Shadow @Final public World world;
    @Shadow @Final private BlockPos.MutableBlockPos position;
    
    /** Warning! Field mixed across mixins: 
     * {@link cubicchunks.asm.mixin.selectable.client.MixinRenderChunk_OptifineSpecific} and this. */
    private Cube[] cubeCache;
    /** Warning! Field mixed across mixins: 
     * {@link cubicchunks.asm.mixin.selectable.client.MixinRenderChunk_OptifineSpecific} and this. */
    private Chunk[] chunkCache;
    
    @ModifyConstant(method = "setPosition", constant = @Constant(intValue = 16))
    public int onSetPosition(int oldValue) {
        return RenderVariables.getRenderChunkSize();
    }
    
    @ModifyConstant(method = "rebuildChunk", constant = @Constant(intValue = 15))
    public int onRebuildChunk(int oldValue) {
        return RenderVariables.getRenderChunkMaxPos();
    }
    
    @ModifyConstant(method = "getDistanceSq", constant = @Constant(doubleValue = 8.0D))
    public double onDistanceSq(double oldValue) {
        return RenderVariables.getRenderChunkCenterPos();
    }
    
    @Override
    public boolean hasEntities() {
        boolean hasEntities = false;
        ICubicWorldClient cworld = (ICubicWorldClient) world;
        int renderChunkCubeSize = RenderVariables.getRenderChunkSize() / Cube.SIZE;
        if (cworld.isCubicWorld()) {
            if (cubeCache == null) {
                cubeCache = new Cube[1 << RenderVariables.getRenderChunkPosShitBit() * 3];
                CubeProviderClient cubeProvider = cworld.getCubeCache();
                int cubePosStartX = Coords.blockToCube(position.getX());
                int cubePosStartY = Coords.blockToCube(position.getY());
                int cubePosStartZ = Coords.blockToCube(position.getZ());
                int index = 0;
                for (int cubePosX = cubePosStartX; cubePosX < cubePosStartX + renderChunkCubeSize; cubePosX++)
                    for (int cubePosY = cubePosStartY; cubePosY < cubePosStartY + renderChunkCubeSize; cubePosY++)
                        for (int cubePosZ = cubePosStartZ; cubePosZ < cubePosStartZ + renderChunkCubeSize; cubePosZ++) {
                            Cube cube = cubeProvider.getCube(cubePosX, cubePosY, cubePosZ);
                            cubeCache[index++] = cube;
                            if (!hasEntities && cube.getEntityContainer().size() != 0)
                                hasEntities = true;
                        }
                return hasEntities;
            }
            for (Cube cube : cubeCache) {
                if (cube.getEntityContainer().size() != 0) {
                    return true;
                }
            }
        } else {
            int blockPosStartY = position.getY();
            if (blockPosStartY < 0 || blockPosStartY >= world.getHeight()) {
                return false;
            }
            int cubePosStartY = Coords.blockToCube(blockPosStartY);
            if (chunkCache == null) {
                chunkCache = new Chunk[1 << RenderVariables.getRenderChunkPosShitBit() * 2];
                int chunkPosStartX = Coords.blockToCube(position.getX());
                int chunkPosStartZ = Coords.blockToCube(position.getZ());
                int index = 0;
                for (int chunkPosX = chunkPosStartX; chunkPosX < chunkPosStartX + renderChunkCubeSize; chunkPosX++)
                    for (int chunkPosZ = chunkPosStartZ; chunkPosZ < chunkPosStartZ + renderChunkCubeSize; chunkPosZ++) {
                        Chunk chunk = world.getChunkFromChunkCoords(chunkPosX, chunkPosZ);
                        chunkCache[index++] = chunk;
                        for (int cubePosY = cubePosStartY; cubePosY < cubePosStartY + renderChunkCubeSize && cubePosY < 16; cubePosY++) {
                            if (!hasEntities && chunk.getEntityLists()[cubePosY].size() != 0)
                                hasEntities = true;
                        }
                    }
                return hasEntities;
            }
            for (Chunk chunk : chunkCache) {
                for (int cubePosY = cubePosStartY; cubePosY < cubePosStartY + renderChunkCubeSize && cubePosY < 16; cubePosY++) {
                    if (chunk.getEntityLists()[cubePosY].size() != 0)
                        return true;
                }
            }
        }
        return hasEntities;
    }
}
