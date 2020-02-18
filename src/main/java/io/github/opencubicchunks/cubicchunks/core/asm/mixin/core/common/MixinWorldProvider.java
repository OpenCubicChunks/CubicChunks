/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.core.world.SpawnPlaceFinder;
import io.github.opencubicchunks.cubicchunks.core.world.WorldSavedCubicChunksData;
import io.github.opencubicchunks.cubicchunks.core.world.provider.ICubicWorldProvider;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.util.NotCubicChunksWorldException;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.api.worldgen.VanillaCompatibilityGeneratorProviderBase;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.gen.IChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(WorldProvider.class)
public abstract class MixinWorldProvider implements ICubicWorldProvider {

    @Shadow protected World world;

    @Shadow protected boolean nether;

    @Shadow public abstract DimensionType getDimensionType();

    @Shadow public abstract IChunkGenerator createChunkGenerator();

    @Shadow(remap = false) public abstract int getActualHeight();

    private boolean getActualHeightForceOriginalFlag = false;

    /**
     * @return world height
     * @reason return the real world height instead of hardcoded 256
     * @author Barteks2x
     */
    @Overwrite(remap = false)
    public int getHeight() {
        return ((ICubicWorld) world).getMaxHeight();
    }

    @Inject(method = "getActualHeight", at = @At("HEAD"), cancellable = true, remap = false)
    private void getActualHeight(CallbackInfoReturnable<Integer> cir) {
        if (world == null || !((ICubicWorld) world).isCubicWorld() || !(world.getWorldType() instanceof ICubicWorldType)) {
            return;
        }
        cir.setReturnValue(((ICubicWorld) world).getMaxGenerationHeight());
    }

    @Override
    public int getOriginalActualHeight() {
        try {
            getActualHeightForceOriginalFlag = true;
            return getActualHeight();
        } finally {
            getActualHeightForceOriginalFlag = false;
        }
    }

    @Nullable
    @Override
    public ICubeGenerator createCubeGenerator() {
        if (!((ICubicWorld) world).isCubicWorld()) {
            throw new NotCubicChunksWorldException();
        }
        if (world.getWorldType() instanceof ICubicWorldType
                && ((ICubicWorldType) world.getWorldType()).hasCubicGeneratorForWorld(world)) {
            return ((ICubicWorldType) world.getWorldType()).createCubeGenerator(world);
        }
        WorldSavedCubicChunksData savedData =
                (WorldSavedCubicChunksData) world.getPerWorldStorage().getOrLoadData(WorldSavedCubicChunksData.class, "cubicChunksData");
        return VanillaCompatibilityGeneratorProviderBase.REGISTRY.getValue(savedData.compatibilityGeneratorType)
                .provideGenerator(this.createChunkGenerator(), world);
    }

    @Inject(method = "getRandomizedSpawnPoint", at = @At(value = "HEAD"), cancellable = true, remap = false)
    private void findRandomizedSpawnPoint(CallbackInfoReturnable<BlockPos> cir) {
        if (((ICubicWorld) world).isCubicWorld()) {
            cir.setReturnValue(SpawnPlaceFinder.getRandomizedSpawnPoint(world));
            cir.cancel();
        }
    }

    @Inject(method = "canCoordinateBeSpawn", at = @At("HEAD"), cancellable = true)
    private void canCoordinateBeSpawnCC(int x, int z, CallbackInfoReturnable<Boolean> cir) {
        if (!((ICubicWorld) world).isCubicWorld()) {
            return;
        }
        cir.cancel();
        BlockPos blockpos = new BlockPos(x, 64, z);

        if (this.world.getBiome(blockpos).ignorePlayerSpawnSuitability()) {
            cir.setReturnValue(true);
        } else {
            BlockPos top = SpawnPlaceFinder.getTopBlockBisect(world, blockpos);
            if (top == null) {
                cir.setReturnValue(false);
            } else {
                cir.setReturnValue(this.world.getBlockState(top).getBlock() == Blocks.GRASS);
            }
        }
    }

    @Override public World getWorld() {
        return world;
    }
}
