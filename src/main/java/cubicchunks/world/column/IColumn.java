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
package cubicchunks.world.column;

import com.google.common.base.Predicate;
import cubicchunks.util.Coords;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.IHeightMap;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IColumn {

    /**
     * Return Y position of the block directly above the top non-transparent block, or {@link Coords#NO_HEIGHT} + 1 if
     * there are no non-transparent blocks
     * <p>
     * CHECKED: 1.11-13.19.0.2148
     */
    int getHeight(BlockPos pos);

    /**
     * Return Y position of the block directly above the top non-transparent block, or {@link Coords#NO_HEIGHT} + 1 if
     * there are no non-transparent blocks
     * <p>
     * CHECKED: 1.11-13.19.0.2148
     */
    int getHeightValue(int localX, int localZ);

    @Deprecated
    // TODO: stop this method form being used by vanilla (any algorithms in vanilla that use it are be broken any way)
    // don't use this! It's only here because vanilla needs it
    // CHECKED: 1.11-13.19.0.2148
    int getTopFilledSegment();

    // CHECKED: 1.11-13.19.0.2148
    // this is here so that it's not hidden when updating
    int getBlockLightOpacity(BlockPos pos);

    /**
     * Retrieve the block state at the specified location
     *
     * @param pos target location
     *
     * @return The block state
     *
     * @see IColumn#getBlockState(int, int, int)
     * @see Cube#getBlockState(BlockPos)
     * <p>
     * CHECKED: 1.11-13.19.0.2148 - super calls the x/y/z version
     */
    IBlockState getBlockState(BlockPos pos);

    /**
     * Retrieve the block state at the specified location
     *
     * @param blockX block x position
     * @param blockY block y position
     * @param blockZ block z position
     *
     * @return The block state
     *
     * @see IColumn#getBlockState(BlockPos)
     * @see Cube#getBlockState(int, int, int)
     */
    IBlockState getBlockState(int blockX, int blockY, int blockZ);

    /**
     * Set the block state at the specified location
     *
     * @param pos target location
     * @param newstate target state of the block at that position
     *
     * @return The the old state of the block at the position, or null if there was no change
     * <p>
     * CHECKED: 1.11-13.19.0.2148
     */
    @Nullable IBlockState setBlockState(BlockPos pos, IBlockState newstate);

    /**
     * Retrieve the raw light level at the specified location
     *
     * @param type The type of light (sky or block light)
     * @param pos The position at which light should be checked
     *
     * @return the light level
     *
     * @see Cube#getLightFor(EnumSkyBlock, BlockPos)
     */
    int getLightFor(EnumSkyBlock type, BlockPos pos);

    /**
     * Set the raw light level at the specified location
     *
     * @param type The type of light (sky or block light)
     * @param pos The position at which light should be updated
     * @param value the light level
     *
     * @see Cube#setLightFor(EnumSkyBlock, BlockPos, int)
     */
    void setLightFor(EnumSkyBlock type, BlockPos pos, int value);

    /**
     * Retrieve actual light level at the specified location. This is the brightest of all types of light affecting this
     * block
     *
     * @param pos the target position
     * @param amount skylight falloff factor
     *
     * @return actual light level at this location
     */
    int getLightSubtracted(BlockPos pos, int amount);

    /**
     * Add an entity to this column
     *
     * @param entity entity to add
     */
    void addEntity(Entity entity);

    /**
     * Remove an entity from this column
     *
     * @param entityIn The entity to remove
     *
     * @see IColumn#removeEntityAtIndex(Entity, int)
     */
    void removeEntity(Entity entityIn);

    /**
     * Remove an entity from the cube at the specified location
     *
     * @param entity The entity to remove
     * @param cubeY cube y location
     */
    void removeEntityAtIndex(Entity entity, int cubeY);

    /**
     * Check whether the block at the specified location has a clear line of view towards the sky
     *
     * @param pos target location
     *
     * @return <code>true</code> if there is no block between this block and the sky (including this block),
     * <code>false</code> otherwise
     * <p>
     * CHECKED: 1.11-13.19.0.2148
     */
    boolean canSeeSky(BlockPos pos);

    /**
     * Retrieve the tile entity at the specified location
     *
     * @param pos target location
     * @param createType how fast the tile entity is needed
     *
     * @return the tile entity at the specified location, or <code>null</code> if there is no entity and
     * <code>createType</code> was not {@link net.minecraft.world.chunk.Chunk.EnumCreateEntityType#IMMEDIATE}
     *
     * @see Cube#getTileEntity(BlockPos, Chunk.EnumCreateEntityType)
     */
    @Nullable TileEntity getTileEntity(BlockPos pos, Chunk.EnumCreateEntityType createType);

    /**
     * Add a tile entity to this column
     *
     * @param tileEntity The tile entity to add
     *
     * @see Cube#addTileEntity(TileEntity)
     */
    void addTileEntity(TileEntity tileEntity);

    /**
     * Add a tile entity to this column at the specified location
     *
     * @param pos The target location
     * @param blockEntity The tile entity to add
     */
    void addTileEntity(BlockPos pos, TileEntity blockEntity);

    /**
     * Remove the tile entity at the specified location
     *
     * @param pos target location
     */
    void removeTileEntity(BlockPos pos);

    /**
     * Called when this column is finished loading
     */
    void onChunkLoad();

    /**
     * Called when this column is being unloaded
     */
    void onChunkUnload();

    /**
     * Retrieve all matching entities within a specific area of the world
     *
     * @param exclude don't include this entity in the results
     * @param queryBox section of the world being checked
     * @param out list to which found entities should be added
     * @param predicate filter to match entities against
     * <p>
     * CHECKED: 1.11-13.19.0.2148
     */
    void getEntitiesWithinAABBForEntity(@Nullable Entity exclude, AxisAlignedBB queryBox, List<Entity> out,
            @Nullable Predicate<? super Entity> predicate);

    /**
     * Retrieve all matching entities of the specified type within a specific area of the world
     *
     * @param entityType the type of entity to retrieve
     * @param queryBox section of the world being checked
     * @param out list to which found entities should be added
     * @param predicate filter to match entities against
     * @param <T> type parameter for the class of entities being searched for
     * <p>
     * CHECKED: 1.11-13.19.0.2148
     */
    <T extends Entity> void getEntitiesOfTypeWithinAAAB(Class<? extends T> entityType, AxisAlignedBB queryBox, List<T> out,
            @Nullable Predicate<? super T> predicate);

    /**
     * Check whether this column needs to be written back to disk for persistence
     *
     * @param flag unused
     *
     * @return <code>true</code> if there were modifications since the time this column was loaded from disk,
     * <code>false</code> otherwise
     */
    boolean needsSaving(boolean flag);

    /**
     * Retrieve lowest block still affected by rain and snow
     *
     * @param pos The target block column to check
     *
     * @return The lowest block at the same x and z coordinates that is still hit by rain and snow
     */
    //TODO: Actual precipitation heightmap, currently skylight heightmap is used which triggers an old MC alpha bug
    BlockPos getPrecipitationHeight(BlockPos pos);

    /**
     * Tick this column
     *
     * @param tryToTickFaster Whether costly calculations should be skipped in order to catch up with ticks
     */
    void onTick(boolean tryToTickFaster);

    /**
     * See if there is any blocks in the specified section of the world. Note that while parameters are block
     * coordinates, the check is actually aligned to cubes.
     *
     * @param minBlockY Lower end of the section being checked
     * @param maxBlockY Upper end of the section being checked
     *
     * @return <code>true</code> if there is only air blocks in the checked cubes, <code>false</code> otherwise
     */
    boolean getAreLevelsEmpty(int minBlockY, int maxBlockY);

    /**
     * Returns internal biome ID array
     */
    byte[] getBiomeArray();

    // TODO: lighting should not be done in Column
    @Deprecated void resetRelightChecks();

    //TODO: enqueueRelightChecks() must die! (it should be in its own lighting system or at least only in Cube)
    @Deprecated void enqueueRelightChecks();

    @Deprecated // TODO: only used by IONbtReader and IONbtWriter ... and those are super broken
    //       add throw new UnsupportedOperationException();
    int[] getHeightMap();

    /**
     * Retrieve a map of all tile entities in this column and their locations
     *
     * @return A map, mapping positions to tile entities at that position
     */
    Map<BlockPos, TileEntity> getTileEntityMap();

    /**
     * Check if this column needs to be ticked
     *
     * @return <code>true</code> if any cube in this column needs to be ticked, <code>false</code> otherwise
     */
    boolean shouldTick();

    /**
     * @return x position of this column
     */
    int getX();

    /**
     * @return z position of this column
     */
    int getZ();

    /**
     * @return the height map of this column
     */
    IHeightMap getOpacityIndex();

    /**
     * Retrieve all cubes in this column that are currently loaded
     *
     * @return the cubes
     */
    Collection<Cube> getLoadedCubes();

    /**
     * Iterate over all loaded cubes in this column in order. If <code>startY < endY</code>, order is bottom to top,
     * otherwise order is top to bottom.
     *
     * @param startY initial cube y position
     * @param endY last cube y position
     *
     * @return an iterator over all loaded cubes between <code>startY</code> and <code>endY</code>
     */
    Iterable<Cube> getLoadedCubes(int startY, int endY);

    /**
     * Retrieve the cube at the specified location if it is loaded.
     *
     * @param cubeY cube y position
     *
     * @return the cube at that position, or <code>null</code> if it is not loaded
     */
    @Nullable Cube getLoadedCube(int cubeY);

    /**
     * Retrieve the cube at the specified location
     *
     * @param cubeY cube y position
     *
     * @return the cube at that position
     */
    Cube getCube(int cubeY);

    /**
     * Add a cube to this column
     *
     * @param cube the cube being added
     */
    void addCube(Cube cube);

    /**
     * Remove the cube at the specified height
     *
     * @param cubeY cube y position
     *
     * @return the removed cube if it existed, otherwise <code>null</code>
     */
    @Nullable Cube removeCube(int cubeY);

    /**
     * Check if there are any loaded cube in this column
     *
     * @return <code>true</code> if there is at least on loaded cube in this column, <code>false</code> otherwise
     */
    boolean hasLoadedCubes();

    /**
     * Notify this column that it has been saved
     */
    void markSaved();

    /**
     * Set last save time in world ticks, used to determine if column needs saving
     */
    void setLastSaveTime(long saveTime);

    /**
     * Among all top blocks in this column, return the height of the lowest one
     *
     * @return the height of the lowest top block
     */
    int getLowestHeight();

    /**
     * Retrieve the world to which this column belongs
     *
     * @return the world
     */
    ICubicWorld getCubicWorld();

    /**
     * Note: this method is intended for internal use only.
     *
     * Make the chunk ready to use this cube for the next block operation.
     * This cube will be used only if the coordinates match.
     */
    void preCacheCube(Cube cube);

    /**
     * Set the unloaded flag.
     *
     * TODO: What it's actually needed for now?
     */
    void markUnloaded(boolean unloaded);

    // TODO: remove this?
    void setModified(boolean modified);

    /**
     * Returns the column position
     */
    ChunkPos getChunkCoordIntPair();

    long getInhabitedTime();

    void setInhabitedTime(long inhabitedTime);
}
