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

import static cubicchunks.client.RenderConstants.*;

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
import cubicchunks.util.Coords;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.ICubicWorldClient;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
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

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(RenderChunk.class)
@Implements(@Interface(iface = IRenderChunk.class, prefix = "renderChunk$"))
public class MixinRenderChunk_OptifineSpecific implements IRenderChunk {

    @Shadow @Final public World world;
    @Shadow @Final private BlockPos.MutableBlockPos position;

    private Cube[] cubeCache;
    private Chunk[] chunkCache;

    @ModifyConstant(method = "makeChunkCacheOF", constant = @Constant(intValue = 16))
    public int onMakingChunkCacheOptifine(int oldValue) {
        return RENDER_CHUNK_SIZE;
    }

    @Inject(method = "isChunkRegionEmpty(Lnet/minecraft/util/math/BlockPos;)Z", at = @At(value = "HEAD"), cancellable = true, remap = false)
    public void isChunkRegionEmpty(BlockPos posIn, CallbackInfoReturnable<Boolean> cif) {
        ICubicWorldClient cworld = (ICubicWorldClient) world;
        cif.cancel();
        boolean isEmpty = true;
        if (cworld.isCubicWorld()) {
            if (cubeCache == null) {
                cubeCache = new Cube[1 << RENDER_CHUNK_SIZE_BIT_SHIFT_CHUNK_POS * 3];
                CubeProviderClient cubeProvider = cworld.getCubeCache();
                int cx0 = Coords.blockToCube(posIn.getX());
                int cy0 = Coords.blockToCube(posIn.getY());
                int cz0 = Coords.blockToCube(posIn.getZ());
                int index = 0;
                for (int cx = cx0; cx < cx0 + RENDER_CHUNK_SIZE_IN_CUBES; cx++)
                    for (int cy = cy0; cy < cy0 + RENDER_CHUNK_SIZE_IN_CUBES; cy++)
                        for (int cz = cz0; cz < cz0 + RENDER_CHUNK_SIZE_IN_CUBES; cz++) {
                            Cube cube = cubeProvider.getCube(cx, cy, cz);
                            cubeCache[index++] = cube;
                            if (!cube.isEmpty())
                                isEmpty = false;
                        }
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
            int y0 = posIn.getY();
            if (y0 < 0 || y0 >= world.getHeight()) {
                cif.setReturnValue(true);
                return;
            }
            if (chunkCache == null) {
                chunkCache = new Chunk[1 << RENDER_CHUNK_SIZE_BIT_SHIFT_CHUNK_POS * 2];
                int cx0 = Coords.blockToCube(posIn.getX());
                int cz0 = Coords.blockToCube(posIn.getZ());
                int index = 0;
                for (int cx = cx0; cx < cx0 + RENDER_CHUNK_SIZE_IN_CUBES; cx++)
                    for (int cz = cz0; cz < cz0 + RENDER_CHUNK_SIZE_IN_CUBES; cz++) {
                        Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
                        chunkCache[index++] = chunk;
                        if (isEmpty && chunk.isEmptyBetween(y0, y0 + RENDER_CHUNK_MAX_POS))
                            isEmpty = false;
                    }
                cif.setReturnValue(isEmpty);
                return;
            }
            for (Chunk chunk : chunkCache) {
                if (chunk.isEmptyBetween(y0, y0 + RENDER_CHUNK_MAX_POS)) {
                    isEmpty = false;
                    break;
                }
            }
        }
        cif.setReturnValue(isEmpty);
    }

    @Inject(method = "setPosition", at = @At(value = "RETURN"), cancellable = false)
    public void setPosition(int x, int y, int z, CallbackInfo ci) {
        this.cubeCache = null;
    }

    public boolean hasEntities() {
        boolean hasEntities = false;
        ICubicWorldClient cworld = (ICubicWorldClient) world;
        if (cworld.isCubicWorld()) {
            if (cubeCache == null) {
                cubeCache = new Cube[1 << RENDER_CHUNK_SIZE_BIT_SHIFT_CHUNK_POS * 3];
                CubeProviderClient cubeProvider = cworld.getCubeCache();
                int cx0 = Coords.blockToCube(position.getX());
                int cy0 = Coords.blockToCube(position.getY());
                int cz0 = Coords.blockToCube(position.getZ());
                int index = 0;
                for (int cx = cx0; cx < cx0 + RENDER_CHUNK_SIZE_IN_CUBES; cx++)
                    for (int cy = cy0; cy < cy0 + RENDER_CHUNK_SIZE_IN_CUBES; cy++)
                        for (int cz = cz0; cz < cz0 + RENDER_CHUNK_SIZE_IN_CUBES; cz++) {
                            Cube cube = cubeProvider.getCube(cx, cy, cz);
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
            int y0 = position.getY();
            if (y0 < 0 || y0 >= world.getHeight()) {
                return false;
            }
            int cy0 = Coords.blockToCube(y0);
            if (chunkCache == null) {
                chunkCache = new Chunk[1 << RENDER_CHUNK_SIZE_BIT_SHIFT_CHUNK_POS * 2];
                int cx0 = Coords.blockToCube(position.getX());
                int cz0 = Coords.blockToCube(position.getZ());
                int index = 0;
                for (int cx = cx0; cx < cx0 + RENDER_CHUNK_SIZE_IN_CUBES; cx++)
                    for (int cz = cz0; cz < cz0 + RENDER_CHUNK_SIZE_IN_CUBES; cz++) {
                        Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
                        chunkCache[index++] = chunk;
                        for (int cy = cy0; cy < cy0 + RENDER_CHUNK_SIZE_IN_CUBES && cy < 16; cy++) {
                            if (!hasEntities && chunk.getEntityLists()[cy].size() != 0)
                                hasEntities = true;
                        }
                    }
                return hasEntities;
            }
            for (Chunk chunk : chunkCache) {
                for (int cy = cy0; cy < cy0 + RENDER_CHUNK_SIZE_IN_CUBES && cy < 16; cy++) {
                    if (chunk.getEntityLists()[cy].size() != 0)
                        return true;
                }
            }
        }
        return hasEntities;
    }

    @Shadow private boolean needsUpdate;

    /**
     * @author Foghrye4
     * @reason Workaround to fix frame-freeze on block break
     */
    @Overwrite
    public void setNeedsUpdate(boolean immediate) {
        this.needsUpdate = true;
        this.cubeCache = null;
        this.chunkCache = null;
    }
}
