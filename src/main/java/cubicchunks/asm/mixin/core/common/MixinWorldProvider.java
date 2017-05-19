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
package cubicchunks.asm.mixin.core.common;

import cubicchunks.world.ICubicWorld;
import cubicchunks.world.NotCubicChunksWorldException;
import cubicchunks.world.SpawnPlaceFinder;
import cubicchunks.world.provider.ICubicWorldProvider;
import cubicchunks.world.type.ICubicWorldType;
import cubicchunks.worldgen.generator.ICubeGenerator;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.IChunkGenerator;
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
public class MixinWorldProvider implements ICubicWorldProvider {

    @Shadow protected World world;

    @Shadow protected boolean hasNoSky;

    /**
     * @reason return the real world height instead of hardcoded 256
     * @author Barteks2x
     */
    // @Overwrite() - overwrite doesn't support unobfuscated methods
    public int getHeight() {
        return cubicWorld().getMaxHeight();
    }

    /**
     * @reason return the real world height instead of hardcoded 256
     * @author Barteks2x
     */
    // @Overwrite() - overwrite doesn't support unobfuscated methods
    public int getActualHeight() {
        return hasNoSky ? 128 : getHeight();
    }

    @Nullable @Override public ICubeGenerator createCubeGenerator() {
        if (!cubicWorld().isCubicWorld()) {
            throw new NotCubicChunksWorldException();
        }
        return ((ICubicWorldType) cubicWorld().getWorldType()).createCubeGenerator(cubicWorld());
    }

    /**
     * Return Double.NaN to remove void fog and fix night vision potion below Y=0.
     * <p>
     * In EntityRenderer.updateFogColor entity Y position is multiplied by value returned by this method.
     * <p>
     * If this method returns any real number - then the void fog factor can be <= 0. But if this method returns NaN -
     * the result is always NaN. And Minecraft enables void fog only of the value is < 1. And since any comparison with
     * NaN returns false - void fog is effectively disabled.
     */
    @Inject(method = "getVoidFogYFactor", at = @At(value = "HEAD"), cancellable = true)
    private void getVoidFogYFactor_injectReplace(CallbackInfoReturnable<Double> cir) {
        if (cubicWorld().isCubicWorld()) {
            cir.setReturnValue(Double.NaN);
            cir.cancel();
        }
    }

    @Inject(method = "getRandomizedSpawnPoint", at = @At(value = "HEAD"), cancellable = true, remap = false)
    private void findRandomizedSpawnPoint(CallbackInfoReturnable<BlockPos> cir) {
        if (cubicWorld().isCubicWorld()) {
            cir.setReturnValue(new SpawnPlaceFinder().getRandomizedSpawnPoint(cubicWorld()));
            cir.cancel();
        }
    }

/*
    @Override
    @Deprecated
    public boolean canDropChunk(int x, int z) {
        return true;
    }

    @Override
    public boolean canCoordinateBeSpawn(int x, int z) {
        //TODO: DONT USE WORLD.getGroundAboveSeaLevel()
        BlockPos blockpos = new BlockPos(x, 0, z);
        return this.world.getBiome(blockpos).ignorePlayerSpawnSuitability() ||
                this.world.getGroundAboveSeaLevel(blockpos).getBlock() == Blocks.GRASS;
    }
*/
    private ICubicWorld cubicWorld() {
        return (ICubicWorld) world;
    }
}
