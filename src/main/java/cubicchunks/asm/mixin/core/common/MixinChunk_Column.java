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

import com.google.common.base.Predicate;
import cubicchunks.world.ICubeProvider;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.IHeightMap;
import cubicchunks.world.column.CubeMap;
import cubicchunks.world.column.IColumn;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = Chunk.class, priority = 2000)
@Implements(@Interface(iface = IColumn.class, prefix = "chunk$"))
public abstract class MixinChunk_Column implements IColumn {

    // WARNING: WHEN YOU RENAME ANY OF THESE 2 FIELDS RENAME CORRESPONDING FIELDS IN MixinChunk_Cubes
    private CubeMap cubeMap;
    private IHeightMap opacityIndex;

    @Shadow @Final public int zPosition;

    @Shadow @Final public int xPosition;

    @Shadow @Final private World world;

    @Shadow public boolean unloaded;

    @Override public int getZ() {
        return this.zPosition;
    }


    @Override public int getX() {
        return this.xPosition;
    }


    @Shadow public abstract IBlockState setBlockState(BlockPos arg1, IBlockState arg2);

    @Intrinsic public IBlockState chunk$setBlockState(BlockPos arg1, IBlockState arg2) {
        return setBlockState(arg1, arg2);
    }


    @Shadow public abstract int getLightFor(EnumSkyBlock arg1, BlockPos arg2);

    @Intrinsic public int chunk$getLightFor(EnumSkyBlock arg1, BlockPos arg2) {
        return getLightFor(arg1, arg2);
    }


    @Shadow public abstract void setLightFor(EnumSkyBlock arg1, BlockPos arg2, int arg3);

    @Intrinsic public void chunk$setLightFor(EnumSkyBlock arg1, BlockPos arg2, int arg3) {
        setLightFor(arg1, arg2, arg3);
    }


    @Shadow public abstract int getLightSubtracted(BlockPos arg1, int arg2);

    @Intrinsic public int chunk$getLightSubtracted(BlockPos arg1, int arg2) {
        return getLightSubtracted(arg1, arg2);
    }


    @Shadow public abstract void addEntity(Entity arg1);

    @Intrinsic public void chunk$addEntity(Entity arg1) {
        addEntity(arg1);
    }


    @Shadow public abstract void removeEntity(Entity arg1);

    @Intrinsic public void chunk$removeEntity(Entity arg1) {
        removeEntity(arg1);
    }


    @Shadow public abstract void removeEntityAtIndex(Entity arg1, int arg2);

    @Intrinsic public void chunk$removeEntityAtIndex(Entity arg1, int arg2) {
        removeEntityAtIndex(arg1, arg2);
    }


    @Shadow public abstract boolean canSeeSky(BlockPos arg1);

    @Intrinsic public boolean chunk$canSeeSky(BlockPos arg1) {
        return canSeeSky(arg1);
    }


    @Shadow public abstract TileEntity getTileEntity(BlockPos arg1, Chunk.EnumCreateEntityType arg2);

    @Intrinsic public TileEntity chunk$getTileEntity(BlockPos arg1, Chunk.EnumCreateEntityType arg2) {
        return getTileEntity(arg1, arg2);
    }


    @Shadow public abstract void addTileEntity(TileEntity arg1);

    @Intrinsic public void chunk$addTileEntity(TileEntity arg1) {
        addTileEntity(arg1);
    }


    @Shadow public abstract void addTileEntity(BlockPos arg1, TileEntity arg2);

    @Intrinsic public void chunk$addTileEntity(BlockPos arg1, TileEntity arg2) {
        addTileEntity(arg1, arg2);
    }


    @Shadow public abstract void removeTileEntity(BlockPos arg1);

    @Intrinsic public void chunk$removeTileEntity(BlockPos arg1) {
        removeTileEntity(arg1);
    }


    @Shadow public abstract void onChunkLoad();

    @Intrinsic public void chunk$onChunkLoad() {
        onChunkLoad();
    }


    @Shadow public abstract void onChunkUnload();

    @Intrinsic public void chunk$onChunkUnload() {
        onChunkUnload();
    }


    @Shadow public abstract void getEntitiesWithinAABBForEntity(Entity arg1, AxisAlignedBB arg2, List arg3, Predicate arg4);

    @Intrinsic public void chunk$getEntitiesWithinAABBForEntity(Entity arg1, AxisAlignedBB arg2, List arg3, Predicate arg4) {
        getEntitiesWithinAABBForEntity(arg1, arg2, arg3, arg4);
    }


    @Shadow public abstract void getEntitiesOfTypeWithinAAAB(Class arg1, AxisAlignedBB arg2, List arg3, Predicate arg4);

    @Intrinsic public void chunk$getEntitiesOfTypeWithinAAAB(Class arg1, AxisAlignedBB arg2, List arg3, Predicate arg4) {
        getEntitiesOfTypeWithinAAAB(arg1, arg2, arg3, arg4);
    }


    @Shadow public abstract boolean needsSaving(boolean arg1);

    @Intrinsic public boolean chunk$needsSaving(boolean arg1) {
        return needsSaving(arg1);
    }


    @Shadow public abstract BlockPos getPrecipitationHeight(BlockPos arg1);

    @Intrinsic public BlockPos chunk$getPrecipitationHeight(BlockPos arg1) {
        return getPrecipitationHeight(arg1);
    }


    @Shadow public abstract int getHeight(BlockPos arg1);

    @Intrinsic public int chunk$getHeight(BlockPos arg1) {
        return getHeight(arg1);
    }


    @Shadow public abstract int getHeightValue(int arg1, int arg2);

    @Intrinsic public int chunk$getHeightValue(int arg1, int arg2) {
        return getHeightValue(arg1, arg2);
    }


    @Shadow public abstract int getTopFilledSegment();

    @Intrinsic public int chunk$getTopFilledSegment() {
        return getTopFilledSegment();
    }


    @Shadow public abstract int getBlockLightOpacity(BlockPos arg1);

    @Intrinsic public int chunk$getBlockLightOpacity(BlockPos arg1) {
        return getBlockLightOpacity(arg1);
    }


    @Shadow public abstract IBlockState getBlockState(BlockPos arg1);

    @Intrinsic public IBlockState chunk$getBlockState(BlockPos arg1) {
        return getBlockState(arg1);
    }


    @Shadow public abstract IBlockState getBlockState(int arg1, int arg2, int arg3);

    @Intrinsic public IBlockState chunk$getBlockState(int arg1, int arg2, int arg3) {
        return getBlockState(arg1, arg2, arg3);
    }


    @Override public Cube getLoadedCube(int cubeY) {
        return getCubicWorld().getCubeCache().getLoadedCube(xPosition, cubeY, zPosition);
    }


    @Override public Cube getCube(int cubeY) {
        return getCubicWorld().getCubeCache().getCube(xPosition, cubeY, zPosition);
    }


    @Override public void addCube(Cube cube) {
        this.cubeMap.put(cube);
    }


    @Override public Cube removeCube(int cubeY) {
        return this.cubeMap.remove(cubeY);
    }


    @Override public boolean hasLoadedCubes() {
        return !cubeMap.isEmpty();
    }


    @Override public void markSaved() {
        this.setModified(false);
    }


    @Override public ICubicWorld getCubicWorld() {
        return (ICubicWorld) this.world;
    }


    @Override public void markUnloaded(boolean unloaded) {
        this.unloaded = unloaded;
    }


    @Shadow public abstract void onTick(boolean arg1);

    @Intrinsic public void chunk$onTick(boolean arg1) {
        onTick(arg1);
    }


    @Shadow public abstract boolean getAreLevelsEmpty(int arg1, int arg2);

    @Intrinsic public boolean chunk$getAreLevelsEmpty(int arg1, int arg2) {
        return getAreLevelsEmpty(arg1, arg2);
    }


    @Shadow public abstract byte[] getBiomeArray();

    @Intrinsic public byte[] chunk$getBiomeArray() {
        return getBiomeArray();
    }


    @Shadow public abstract void resetRelightChecks();

    @Intrinsic public void chunk$resetRelightChecks() {
        resetRelightChecks();
    }


    @Shadow public abstract void enqueueRelightChecks();

    @Intrinsic public void chunk$enqueueRelightChecks() {
        enqueueRelightChecks();
    }


    @Shadow public abstract int[] getHeightMap();

    @Intrinsic public int[] chunk$getHeightMap() {
        return getHeightMap();
    }


    @Shadow public abstract Map getTileEntityMap();

    @Intrinsic public Map chunk$getTileEntityMap() {
        return getTileEntityMap();
    }


    @Shadow public abstract void setModified(boolean arg1);

    @Intrinsic public void chunk$setModified(boolean arg1) {
        setModified(arg1);
    }


    @Shadow public abstract void setLastSaveTime(long arg1);

    @Intrinsic public void chunk$setLastSaveTime(long arg1) {
        setLastSaveTime(arg1);
    }


    @Shadow public abstract int getLowestHeight();

    @Intrinsic public int chunk$getLowestHeight() {
        return getLowestHeight();
    }


    @Shadow public abstract long getInhabitedTime();

    @Intrinsic public long chunk$getInhabitedTime() {
        return getInhabitedTime();
    }


    @Shadow public abstract void setInhabitedTime(long arg1);

    @Intrinsic public void chunk$setInhabitedTime(long arg1) {
        setInhabitedTime(arg1);
    }

    @Shadow public abstract ChunkPos getChunkCoordIntPair();

    @Override public ChunkPos getPos() {
        return getChunkCoordIntPair();
    }


    @Override public boolean shouldTick() {
        for (Cube cube : cubeMap) {
            if (cube.getTickets().shouldTick()) {
                return true;
            }
        }
        return false;
    }


    @Override public IHeightMap getOpacityIndex() {
        return this.opacityIndex;
    }


    @Override public Collection getLoadedCubes() {
        return this.cubeMap.all();
    }


    @Override public Iterable getLoadedCubes(int startY, int endY) {
        return this.cubeMap.cubes(startY, endY);
    }
}
