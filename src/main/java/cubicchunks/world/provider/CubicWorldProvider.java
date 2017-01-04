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
package cubicchunks.world.provider;

import cubicchunks.world.ICubicWorld;
import cubicchunks.world.type.ICubicWorldType;
import cubicchunks.worldgen.generator.DummyChunkGenerator;
import cubicchunks.worldgen.generator.ICubeGenerator;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkGenerator;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * CubicChunks WorldProvider for Overworld.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CubicWorldProvider extends WorldProvider implements ICubicWorldProvider {

    @Override
    public int getHeight() {
        return getCubicWorld().getMaxHeight();
    }

    @Override
    public int getActualHeight() {
        return hasNoSky ? 128 : getHeight();
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
    @Override
    public double getVoidFogYFactor() {
        return Double.NaN;
    }

    @Override
    @Deprecated
    public IChunkGenerator createChunkGenerator() {
        return new DummyChunkGenerator(this.world);
    }

    @Nullable @Override
    public ICubeGenerator createCubeGenerator() {
        // We need to assume that its an ICubicWorldType...
        // There is really nothing else we can do as a non-overworld porvider
        // that works with a vanilla world type would have overriden this method.
        if (!(this.world.getWorldType() instanceof ICubicWorldType)) {
            throw new IllegalStateException(
                    "Cubic world provider does not override createCubeGenerator() and the world type is not ICubicWorldType!");
        }
        return ((ICubicWorldType) this.world.getWorldType())
                .createCubeGenerator(getCubicWorld());
    }

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

    @Override
    public BlockPos getRandomizedSpawnPoint() {
        //TODO: uses getTopSolidOrLiquidBlock() ... not good
        BlockPos ret = this.world.getSpawnPoint();

        boolean isAdventure = world.getWorldInfo().getGameType() == GameType.ADVENTURE;
        int spawnFuzz;
        if (this.world instanceof WorldServer) {
            spawnFuzz = world.getWorldType().getSpawnFuzz((WorldServer) this.world, this.world.getMinecraftServer());
        } else {
            spawnFuzz = 1;
        }
        int border = MathHelper.floor(world.getWorldBorder().getClosestDistance(ret.getX(), ret.getZ()));
        if (border < spawnFuzz) {
            spawnFuzz = border;
        }

        if (!hasNoSky() && !isAdventure && spawnFuzz != 0) {
            if (spawnFuzz < 2) {
                spawnFuzz = 2;
            }
            int spawnFuzzHalf = spawnFuzz / 2;
            ret = getTSOLBFixed(ret.add(world.rand.nextInt(spawnFuzzHalf) - spawnFuzz, 0, world.rand.nextInt(spawnFuzzHalf) - spawnFuzz));
        }

        return ret;
    }

    private BlockPos getTSOLBFixed(BlockPos pos) {
        Chunk chunk = world.getChunkFromBlockCoords(pos);
        BlockPos blockpos;
        BlockPos blockpos1;

        int startY = chunk.getHeight(pos);
        startY = startY < getCubicWorld().getMinHeight() ? 80 : startY;

        for (blockpos = new BlockPos(pos.getX(), startY, pos.getZ()); blockpos.getY() >= 0; blockpos = blockpos1) {
            blockpos1 = blockpos.down();
            IBlockState state = chunk.getBlockState(blockpos1);

            if (state.getMaterial().blocksMovement() && !state.getBlock().isLeaves(state, world, blockpos1) && !state.getBlock()
                    .isFoliage(world, blockpos1)) {
                break;
            }
        }

        return blockpos;
    }

    public ICubicWorld getCubicWorld() {
        return (ICubicWorld) world;
    }
}
