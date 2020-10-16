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

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToCube;
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToLocal;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.util.NotCubicChunksWorldException;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldSettings;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.GameRules;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;
import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Contains implementation of {@link ICubicWorld} interface.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(World.class)
@Implements(@Interface(iface = ICubicWorld.class, prefix = "world$"))
public abstract class MixinWorld implements ICubicWorldInternal {

    // these have to be here because of mixin limitation, they are used by MixinWorldServer
    @Shadow public abstract ISaveHandler getSaveHandler();
    @Shadow public abstract boolean isAreaLoaded(BlockPos blockpos1, BlockPos blockpos2);
    @Shadow public abstract boolean isAreaLoaded(BlockPos center, int radius);

    @Shadow protected IChunkProvider chunkProvider;
    @Shadow @Final @Mutable public WorldProvider provider;
    @Shadow @Final public Random rand;
    @Shadow @Final public boolean isRemote;
    @Shadow @Final public Profiler profiler;
    @Shadow @Final @Mutable protected ISaveHandler saveHandler;
    @Shadow protected boolean findingSpawnPoint;
    @Shadow protected WorldInfo worldInfo;
    @Shadow protected int updateLCG;

    @Shadow protected abstract boolean isChunkLoaded(int i, int i1, boolean allowEmpty);

    @Nullable private LightingManager lightingManager;
    protected boolean isCubicWorld;
    protected int minHeight = 0, maxHeight = 256, fakedMaxHeight = 0;
    private int minGenerationHeight = 0, maxGenerationHeight = 256;

    @Shadow public abstract boolean isValid(BlockPos pos);

    @Shadow public abstract GameRules getGameRules();

    @Shadow public abstract boolean isRaining();

    @Shadow public abstract boolean isThundering();

    @Shadow public abstract boolean isRainingAt(BlockPos position);

    @Shadow public abstract DifficultyInstance getDifficultyForLocation(BlockPos pos);

    @Shadow public abstract BlockPos getPrecipitationHeight(BlockPos pos);

    @Shadow public abstract boolean isAreaLoaded(StructureBoundingBox box);

    @Shadow public abstract boolean canBlockFreezeNoWater(BlockPos pos);

    @Shadow public abstract boolean setBlockState(BlockPos pos, IBlockState state);

    @Shadow public abstract boolean canSnowAt(BlockPos pos, boolean checkLight);

    @Shadow public abstract boolean isBlockLoaded(BlockPos pos);

    @Shadow public abstract Biome getBiome(BlockPos pos);

    @Shadow public abstract boolean isBlockLoaded(BlockPos pos, boolean allowEmpty);

    @Shadow public abstract boolean isOutsideBuildHeight(BlockPos pos);

    @Shadow public abstract Chunk getChunk(BlockPos pos);

    @Shadow public abstract boolean canSeeSky(BlockPos pos);

    @Shadow public abstract void setLightFor(EnumSkyBlock type, BlockPos pos, int lightValue);

    protected void initCubicWorld(IntRange heightRange, IntRange generationRange) {
        ((ICubicWorldSettings) worldInfo).setCubic(true);
        // Set the world height boundaries to their highest and lowest values respectively
        this.minHeight = heightRange.getMin();
        this.maxHeight = heightRange.getMax();
        this.fakedMaxHeight = this.maxHeight;

        this.minGenerationHeight = generationRange.getMin();
        this.maxGenerationHeight = generationRange.getMax();

        //has to be created early so that creating BlankCube won't crash
        this.lightingManager = new LightingManager((World) (Object) this);
    }

    @Override public boolean isCubicWorld() {
        return this.isCubicWorld;
    }

    @Override public int getMinHeight() {
        return this.minHeight;
    }

    @Override public int getMaxHeight() {
        return this.maxHeight;
    }

    @Override public int getMinGenerationHeight() {
        return this.minGenerationHeight;
    }

    @Override public int getMaxGenerationHeight() {
        return this.maxGenerationHeight;
    }

    @Override public ICubeProviderInternal getCubeCache() {
        if (!this.isCubicWorld()) {
            throw new NotCubicChunksWorldException();
        }
        return (ICubeProviderInternal) this.chunkProvider;
    }

    @Override public LightingManager getLightingManager() {
        if (!this.isCubicWorld()) {
            throw new NotCubicChunksWorldException();
        }
        assert this.lightingManager != null;
        return this.lightingManager;
    }

    @Override
    public boolean testForCubes(CubePos start, CubePos end, Predicate<? super ICube> cubeAllowed) {
        // convert block bounds to chunk bounds
        int minCubeX = start.getX();
        int minCubeY = start.getY();
        int minCubeZ = start.getZ();
        int maxCubeX = end.getX();
        int maxCubeY = end.getY();
        int maxCubeZ = end.getZ();

        for (int cubeX = minCubeX; cubeX <= maxCubeX; cubeX++) {
            for (int cubeY = minCubeY; cubeY <= maxCubeY; cubeY++) {
                for (int cubeZ = minCubeZ; cubeZ <= maxCubeZ; cubeZ++) {
                    Cube cube = this.getCubeCache().getLoadedCube(cubeX, cubeY, cubeZ);
                    if (!cubeAllowed.test(cube)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override public Cube getCubeFromCubeCoords(int cubeX, int cubeY, int cubeZ) {
        return this.getCubeCache().getCube(cubeX, cubeY, cubeZ);
    }

    @Override public Cube getCubeFromBlockCoords(BlockPos pos) {
        return this.getCubeFromCubeCoords(blockToCube(pos.getX()), blockToCube(pos.getY()), blockToCube(pos.getZ()));
    }

    @Override public int getEffectiveHeight(int blockX, int blockZ) {
        return this.chunkProvider.provideChunk(blockToCube(blockX), blockToCube(blockZ))
                .getHeightValue(blockToLocal(blockX), blockToLocal(blockZ));
    }

    // suppress mixin warning when running with -Dmixin.checks.interfaces=true
    @Override public void tickCubicWorld() {
        // pretend this method doesn't exist
        throw new NoSuchMethodError("World.tickCubicWorld: Classes extending World need to implement tickCubicWorld in CubicChunks");
    }

    @Override public void fakeWorldHeight(int height) {
        this.fakedMaxHeight = height;
    }

    /**
     * Some mod's world generation will try to do their work over the whole world height.
     * This allows to fake the world height for them.
     *
     * @return world height
     * @author Barteks2x
     * @reason Optionally return fake height
     */
    @Overwrite
    public int getHeight() {
        if (fakedMaxHeight != 0) {
            return fakedMaxHeight;
        }
        return this.provider.getHeight();
    }


    @Inject(method = "checkLightFor", at = @At("HEAD"), cancellable = true)
    public void checkLightFor(EnumSkyBlock lightType, BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
        if (CubicChunksConfig.fastSimplifiedSkyLight && lightType == EnumSkyBlock.SKY) {
            if (!isAreaLoaded(pos, 1)) {
                ci.setReturnValue(false);
                return;
            }
            int max = canSeeSky(pos) ? 15 : 0;
            int opacity = getBlockState(pos).getLightOpacity((World) (Object) this, pos);
            for (EnumFacing value : EnumFacing.VALUES) {
                max = Math.max(max, (canSeeSky(pos.offset(value)) ? 15 : 0) - Math.max(1, opacity) * 4);
            }
            setLightFor(EnumSkyBlock.SKY, pos, Math.max(7, max));
            ci.setReturnValue(true);
            return;
        }
        if (!CubicChunksConfig.replaceLightRecheck || !isCubicWorld()) {
            return;
        }
        ci.setReturnValue(getLightingManager().checkLightFor(lightType, pos));
    }

    /**
     * @param pos block position
     * @param unusedTileEntity tile entity instance, unused
     * @param ci callback info
     *
     * @author Foghrye4
     * @reason Original {@link World#markChunkDirty(BlockPos, TileEntity)}
     *         called by TileEntities whenever they need to force Chunk to save
     *         valuable info they changed. Because now we store TileEntities in
     *         Cubes instead of Chunks, it will be quite reasonable to force
     *         Cubes to save themselves.
     */
    @Inject(method = "markChunkDirty", at = @At("HEAD"), cancellable = true)
    public void onMarkChunkDirty(BlockPos pos, TileEntity unusedTileEntity, CallbackInfo ci) {
        if (this.isCubicWorld()) {
            Cube cube = this.getCubeCache().getLoadedCube(CubePos.fromBlockCoords(pos));
            if (cube != null) {
                cube.markDirty();
            }
            ci.cancel();
        }
    }
/*
    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    public void onGetBlockState(BlockPos pos, CallbackInfoReturnable<IBlockState> ci) {
        if (this.isCubicWorld()) {
            if (this.isValid(pos))
                ci.setReturnValue(this.getCubeCache()
                        .getCube(Coords.blockToCube(pos.getX()), Coords.blockToCube(pos.getY()), Coords.blockToCube(pos.getZ()))
                        .getBlockState(pos));
            else
                ci.setReturnValue(Blocks.AIR.getDefaultState());
            ci.cancel();
        }
    }
*/
    /**
     * @param pos block position
     * @return blockstate at that position
     * @author Barteks2x
     * @reason Injection causes performance issues, overwrite for cubic chunks version
     */
    @Overwrite
    public IBlockState getBlockState(BlockPos pos) {
        if (this.isOutsideBuildHeight(pos)) { // TODO: maybe avoid height check for cubic chunks world?
            return Blocks.AIR.getDefaultState();
        }
        if (this.isCubicWorld) {
            ICube cube = ((ICubeProviderInternal) this.chunkProvider)
                    .getCube(Coords.blockToCube(pos.getX()), Coords.blockToCube(pos.getY()), Coords.blockToCube(pos.getZ()));
            return cube.getBlockState(pos);
        } else {
            Chunk chunk = this.getChunk(pos);
            return chunk.getBlockState(pos);
        }
    }

    @Inject(method = "getTopSolidOrLiquidBlock", at = @At("HEAD"), cancellable = true)
    private void getTopSolidOrLiquidBlockCubicChunks(BlockPos pos, CallbackInfoReturnable<BlockPos> cir) {
        if (!isCubicWorld()) {
            return;
        }
        cir.cancel();
        Chunk chunk = this.getChunk(pos);
        BlockPos currentPos = getPrecipitationHeight(pos);
        int minY = currentPos.getY() - 64;
        while (currentPos.getY() >= minY) {
            BlockPos nextPos = currentPos.down();
            IBlockState state = chunk.getBlockState(nextPos);

            if (state.getMaterial().blocksMovement()
                    && !state.getBlock().isLeaves(state, (IBlockAccess) this, nextPos)
                    && !state.getBlock().isFoliage((IBlockAccess) this, nextPos)) {
                break;
            }
            currentPos = nextPos;
        }
        cir.setReturnValue(currentPos);
    }

    @Override public boolean isBlockColumnLoaded(BlockPos pos) {
        return isBlockColumnLoaded(pos, true);
    }

    @Override public boolean isBlockColumnLoaded(BlockPos pos, boolean allowEmpty) {
        return this.isChunkLoaded(blockToCube(pos.getX()), blockToCube(pos.getZ()), allowEmpty);
    }
}
