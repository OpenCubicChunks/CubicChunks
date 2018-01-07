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
package cubicchunks.asm.mixin.selectable.client;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import cubicchunks.client.CubeProviderClient;
import cubicchunks.client.IRenderChunk;
import cubicchunks.client.RenderVariables;
import cubicchunks.util.Coords;
import cubicchunks.world.ICubicWorldClient;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.BlockPos;
/**
 * Fixes renderEntities crashing when rendering cubes
 * that are not at existing array index in chunk.getEntityLists(),
 * <p>
 * Allows to render cubes outside of 0..256 height range.
 */
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeModContainer;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(RenderChunk.class)
@Implements(@Interface(iface = IRenderChunk.class, prefix = "renderChunk$"))
public abstract class MixinRenderChunk_OptifineSpecific implements IRenderChunk {

    @Shadow @Final public World world;
    @Shadow @Final private BlockPos.MutableBlockPos position;

    private Cube[] cubeCache;
    private Chunk[] chunkCache;
    private boolean cacheOutdated;
    
    @Inject(method = "<init>", at = @At(value = "RETURN"), cancellable = false)
    public void onConstruct(World worldIn, RenderGlobal renderGlobalIn, int indexIn, CallbackInfo ci){
        cubeCache = new Cube[1 << RenderVariables.getRenderChunkPosShitBit() * 3];
        chunkCache = new Chunk[1 << RenderVariables.getRenderChunkPosShitBit() * 2];
        cacheOutdated = true;
    }

    @ModifyConstant(method = "makeChunkCacheOF", constant = @Constant(intValue = 16))
    public int onMakingChunkCacheOptifine(int oldValue) {
        return RenderVariables.getRenderChunkSize();
    }

    @Inject(method = "isChunkRegionEmpty(Lnet/minecraft/util/math/BlockPos;)Z", at = @At(value = "HEAD"), cancellable = true, remap = false)
    public void isChunkRegionEmpty(BlockPos posIn, CallbackInfoReturnable<Boolean> cif) {
        ICubicWorldClient cworld = (ICubicWorldClient) world;
        cif.cancel();
        int renderChunkCubeSize = RenderVariables.getRenderChunkSize() / Cube.SIZE;
        boolean isEmpty = true;
        if (cworld.isCubicWorld()) {
            if (cacheOutdated) {
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
                            if (!cube.isEmpty())
                                isEmpty = false;
                        }
                cacheOutdated = false;
                cif.setReturnValue(isEmpty);
                return;
            }
            for (Cube cube : cubeCache) {
                if (!cube.isEmpty()) {
                    isEmpty = false;
                    break;
                }
            }
        } else {
            int blockPosStartY = position.getY();
            if (blockPosStartY < 0 || blockPosStartY >= world.getHeight()) {
                cif.setReturnValue(true);
                return;
            }
            int maxPos = RenderVariables.getRenderChunkMaxPos();
            if (cacheOutdated) {
                int chunkPosStartX = Coords.blockToCube(position.getX());
                int chunkPosStartZ = Coords.blockToCube(position.getZ());
                int index = 0;
                for (int chunkPosX = chunkPosStartX; chunkPosX < chunkPosStartX + renderChunkCubeSize; chunkPosX++)
                    for (int chunkPosZ = chunkPosStartZ; chunkPosZ < chunkPosStartZ + renderChunkCubeSize; chunkPosZ++) {
                        Chunk chunk = world.getChunkFromChunkCoords(chunkPosX, chunkPosZ);
                        chunkCache[index++] = chunk;
                        if (isEmpty && chunk.isEmptyBetween(blockPosStartY, blockPosStartY + maxPos))
                            isEmpty = false;
                    }
                cacheOutdated = false;
                cif.setReturnValue(isEmpty);
                return;
            }
            for (Chunk chunk : chunkCache) {
                if (chunk.isEmptyBetween(blockPosStartY, blockPosStartY + maxPos)) {
                    isEmpty = false;
                    break;
                }
            }
        }
        cif.setReturnValue(isEmpty);
    }
    
    @Override
    public boolean hasEntities() {
        boolean hasEntities = false;
        ICubicWorldClient cworld = (ICubicWorldClient) world;
        int renderChunkCubeSize = RenderVariables.getRenderChunkSize() / Cube.SIZE;
        if (cworld.isCubicWorld()) {
            if (cacheOutdated) {
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
                cacheOutdated = false;
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
            if (cacheOutdated) {
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
                cacheOutdated = false;
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

    @Inject(method = "setPosition", at = @At(value = "RETURN"), cancellable = false)
    public void setPosition(int x, int y, int z, CallbackInfo ci) {
        cacheOutdated = true;
    }

    @Shadow private boolean needsUpdate;
    @Shadow private boolean needsImmediateUpdate;

    /**
     * @author Foghrye4
     * @reason Workaround to fix frame-freeze on block break
     */
    @Overwrite
    public void setNeedsUpdate(boolean immediate) {
        cacheOutdated = true;
        if (!ForgeModContainer.alwaysSetupTerrainOffThread) {
            if (this.needsUpdate)
                immediate |= this.needsImmediateUpdate;
            this.needsImmediateUpdate = immediate;
        }
        this.needsUpdate = true;
    }
}
