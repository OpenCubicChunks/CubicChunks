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
import cubicchunks.worldgen.generator.vanilla.VanillaCompatibilityGenerator;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.gen.IChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
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

    @Shadow public abstract int getDimension();

    @Shadow public abstract DimensionType getDimensionType();

    @Shadow public abstract IChunkGenerator createChunkGenerator();

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
        // only give the real value for overworld, mods may use it scan height start in their teleporter code
        return nether ? 128 : getDimension() == 0 ? getHeight() : 256;
    }

    @Nullable @Override public ICubeGenerator createCubeGenerator() {
        if (!cubicWorld().isCubicWorld()) {
            throw new NotCubicChunksWorldException();
        }
        if (this.getDimensionType() == DimensionType.OVERWORLD) {
            return ((ICubicWorldType) cubicWorld().getWorldType()).createCubeGenerator(cubicWorld());
        }
        return new VanillaCompatibilityGenerator(this.createChunkGenerator(), cubicWorld());
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
