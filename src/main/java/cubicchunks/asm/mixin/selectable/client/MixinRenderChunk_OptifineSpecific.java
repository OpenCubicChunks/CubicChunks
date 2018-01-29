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
import cubicchunks.util.Predicates;
import cubicchunks.world.ICubicWorldClient;
import cubicchunks.world.cube.Cube;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.BlockPos;
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
    private boolean cubeCacheOutdated;
    private boolean chunkCacheOutdated;

    @Inject(method = "<init>", at = @At(value = "RETURN"), cancellable = false)
    public void onConstruct(World worldIn, RenderGlobal renderGlobalIn, int indexIn, CallbackInfo ci) {
        cubeCache = new Cube[1 << RenderVariables.getRenderChunkPosShitBit() * 3];
        chunkCache = new Chunk[1 << RenderVariables.getRenderChunkPosShitBit() * 2];
        cubeCacheOutdated = true;
        chunkCacheOutdated = true;
    }

    @ModifyConstant(method = "makeChunkCacheOF", constant = @Constant(intValue = 16))
    public int onMakingChunkCacheOptifine(int oldValue) {
        return RenderVariables.getRenderChunkSize();
    }

    @Inject(method = "isChunkRegionEmpty(Lnet/minecraft/util/math/BlockPos;)Z", at = @At(value = "HEAD"), cancellable = true, remap = false)
    public void isChunkRegionEmpty(BlockPos posIn, CallbackInfoReturnable<Boolean> cif) {
        ICubicWorldClient cworld = (ICubicWorldClient) world;
        cif.cancel();
        if (cworld.isCubicWorld()) {
            boolean isEmpty = !this.testCubeCacheFor(Predicates.CUBE_NOT_EMPTY);
            cif.setReturnValue(isEmpty);
        } else {
            boolean isEmpty = !this.testChunkCacheFor(Predicates.CHUNK_NOT_EMPTY_AT);
            cif.setReturnValue(isEmpty);
        }
    }

    @Override
    public boolean hasEntities() {
        ICubicWorldClient cworld = (ICubicWorldClient) world;
        if (cworld.isCubicWorld()) {
            return this.testCubeCacheFor(Predicates.CUBE_HAS_ENTITIES);
        } else {
            return this.testChunkCacheFor(Predicates.CHUNK_HAS_ENTITIES_AT);
        }
    }

    private boolean testCubeCacheFor(Predicate<Cube> test) {
        boolean resultOfTest = false;
        ICubicWorldClient cworld = (ICubicWorldClient) world;
        int bitShift = RenderVariables.getRenderChunkPosShitBit();
        int mask = (1 << bitShift) - 1;
        if (cubeCacheOutdated) {
            CubeProviderClient cubeProvider = cworld.getCubeCache();
            int cubePosStartX = Coords.blockToCube(position.getX());
            int cubePosStartY = Coords.blockToCube(position.getY());
            int cubePosStartZ = Coords.blockToCube(position.getZ());
            for (int index = 0; index < cubeCache.length; index++) {
                int cubePosX = cubePosStartX + (index >> bitShift * 2);
                int cubePosY = cubePosStartY + ((index >> bitShift) & mask);
                int cubePosZ = cubePosStartZ + (index & mask);
                Cube cube = cubeProvider.getCube(cubePosX, cubePosY, cubePosZ);
                cubeCache[index] = cube;
                if (!resultOfTest && test.test(cube))
                    resultOfTest = true;
            }
            cubeCacheOutdated = false;
            return resultOfTest;
        }
        for (Cube cube : cubeCache) {
            if (test.test(cube)) {
                return true;
            }
        }
        return resultOfTest;
    }

    private boolean testChunkCacheFor(BiPredicate<Chunk, Integer> test) {
        boolean resultOfTest = false;
        int renderChunkCubeSize = RenderVariables.getRenderChunkSize() / Cube.SIZE;
        int blockPosStartY = position.getY();
        if (blockPosStartY < 0 || blockPosStartY >= world.getHeight()) {
            return false;
        }
        int cubePosStartY = Coords.blockToCube(blockPosStartY);
        if (chunkCacheOutdated) {
            int chunkPosStartX = Coords.blockToCube(position.getX());
            int chunkPosStartZ = Coords.blockToCube(position.getZ());
            int index = 0;
            for (int chunkPosX = chunkPosStartX; chunkPosX < chunkPosStartX + renderChunkCubeSize; chunkPosX++)
                for (int chunkPosZ = chunkPosStartZ; chunkPosZ < chunkPosStartZ + renderChunkCubeSize; chunkPosZ++) {
                    Chunk chunk = world.getChunkFromChunkCoords(chunkPosX, chunkPosZ);
                    chunkCache[index++] = chunk;
                    for (int cubePosY = cubePosStartY; cubePosY < cubePosStartY + renderChunkCubeSize && cubePosY < 16; cubePosY++) {
                        if (!resultOfTest && test.test(chunk, cubePosY))
                            resultOfTest = true;
                    }
                }
            chunkCacheOutdated = false;
            return resultOfTest;
        }
        for (Chunk chunk : chunkCache) {
            for (int cubePosY = cubePosStartY; cubePosY < cubePosStartY + renderChunkCubeSize && cubePosY < 16; cubePosY++) {
                if (test.test(chunk, cubePosY))
                    return true;
            }
        }

        return resultOfTest;
    }

    @Inject(method = "setPosition", at = @At(value = "RETURN"), cancellable = false)
    public void setPosition(int x, int y, int z, CallbackInfo ci) {
        cubeCacheOutdated = true;
        chunkCacheOutdated = true;
    }

    @Shadow private boolean needsUpdate;
    @Shadow private boolean needsImmediateUpdate;

    /**
     * @author Foghrye4
     * @reason Workaround to fix frame-freeze on block break
     */
    @Overwrite
    public void setNeedsUpdate(boolean immediate) {
        cubeCacheOutdated = true;
        chunkCacheOutdated = true;
        if (!ForgeModContainer.alwaysSetupTerrainOffThread) {
            if (this.needsUpdate)
                immediate |= this.needsImmediateUpdate;
            this.needsImmediateUpdate = immediate;
        }
        this.needsUpdate = true;
    }
}
