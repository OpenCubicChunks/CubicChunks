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
package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XYZAddressable;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import java.util.EnumSet;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ICube extends XYZAddressable, ICapabilityProvider {

    /**
     * Side length of a cube
     */
    int SIZE = 16;
    double SIZE_D = 16.0D;

    /**
     * Retrieve the block state at the specified location
     *
     * @param pos target location
     *
     * @return The block state
     *
     * @see #getBlockState(int, int, int)
     */
    IBlockState getBlockState(BlockPos pos);

    /**
     * Set the block state at the specified location
     *
     * @param pos target location
     * @param newstate target state of the block at that position
     *
     * @return The the old state of the block at the position, or null if there was no change
     *
     * @see Chunk#setBlockState(BlockPos, IBlockState)
     */
    @Nullable IBlockState setBlockState(BlockPos pos, IBlockState newstate);

    /**
     * Retrieve the block state at the specified location
     *
     * @param blockX block x position
     * @param localOrBlockY block or local y position
     * @param blockZ block z position
     *
     * @return The block state
     *
     * @see #getBlockState(BlockPos)
     */
    IBlockState getBlockState(int blockX, int localOrBlockY, int blockZ);

    /**
     * Retrieve the raw light level at the specified location
     *
     * @param lightType The type of light (sky or block light)
     * @param pos The position at which light should be checked
     *
     * @return the light level
     */
    int getLightFor(EnumSkyBlock lightType, BlockPos pos);

    /**
     * Set the raw light level at the specified location
     *
     * @param lightType The type of light (sky or block light)
     * @param pos The position at which light should be updated
     * @param light the light level
     */
    void setLightFor(EnumSkyBlock lightType, BlockPos pos, int light);

    /**
     * Retrieve the tile entity at the specified location
     *
     * @param pos target location
     * @param createType how fast the tile entity is needed
     *
     * @return the tile entity at the specified location, or {@code null} if there is no entity and
     * {@code createType} was not {@link Chunk.EnumCreateEntityType#IMMEDIATE}
     */
    @Nullable TileEntity getTileEntity(BlockPos pos, Chunk.EnumCreateEntityType createType);

    /**
     * Add a tile entity to this cube
     *
     * @param tileEntity The tile entity to add
     */
    void addTileEntity(TileEntity tileEntity);

    /**
     * Check if there are any non-air blocks in this cube
     *
     * @return {@code true} if this cube contains only air blocks, {@code false} otherwise
     */
    boolean isEmpty();

    /**
     * Convert an integer-encoded address to a local block to a global block position
     *
     * @param localAddress the address of the block
     *
     * @return the block position
     */
    BlockPos localAddressToBlockPos(int localAddress);

    /**
     * @param <T> dummy generic parameter to return a type that is both {@link World} and {@link ICubicWorld}
     * @return this cube's world
     */
    <T extends World & ICubicWorld> T getWorld();

    /**
     * @param <T> dummy generic parameter to return a type that is both {@link Chunk} and {@link IColumn}
     * @return this cube's column
     */
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    <T extends Chunk & IColumn> T getColumn();

    /**
     * Retrieve this cube's x coordinate in cube space
     *
     * @return cube x position
     */
    int getX();

    /**
     * Retrieve this cube's y coordinate in cube space
     *
     * @return cube y position
     */
    int getY();

    /**
     * Retrieve this cube's z coordinate in cube space
     *
     * @return cube z position
     */
    int getZ();

    /**
     * @return this cube's position
     */
    CubePos getCoords();

    /**
     * Check whether a given global block position is contained in this cube
     *
     * @param blockPos the position of the block
     *
     * @return {@code true} if the position is within this cube, {@code false} otherwise
     */
    boolean containsBlockPos(BlockPos blockPos);

    @Nullable ExtendedBlockStorage getStorage();

    /**
     * Retrieve a map of positions to their respective tile entities
     *
     * @return a map containing all tile entities in this cube
     */
    Map<BlockPos, TileEntity> getTileEntityMap();

    /**
     * Returns the internal entity container.
     *
     * @return the entity container
     */
    ClassInheritanceMultiMap<Entity> getEntitySet();

    void addEntity(Entity entity);

    boolean removeEntity(Entity entity);

    /**
     * Check if any modifications happened to this cube since it was loaded from disk
     *
     * @return {@code true} if this cube should be written back to disk
     */
    boolean needsSaving();

    /**
     * Check whether this cube was populated, i.e. if this cube was passed as argument to
     * {@link ICubeGenerator#populate(ICube)}. Check there for more information regarding
     * population.
     *
     * @return {@code true} if this cube has been populated, {@code false} otherwise
     */
    boolean isPopulated();

    /**
     * Check whether this cube was fully populated, i.e. if any cube potentially writing to this cube was passed as an
     * argument to {@link ICubeGenerator#populate(ICube)}. Check there for more
     * information regarding population
     *
     * @return {@code true} if this cube has been populated, {@code false} otherwise
     */
    boolean isFullyPopulated();

    /**
     * Gets internal isSurfaceTracked value. Intended to be used only for serialization.
     *
     * @return true if the contents of thic cube have already been supplied to surface tracker
     */
    boolean isSurfaceTracked();

    /**
     * Check whether this cube's initial diffuse skylight has been calculated
     *
     * @return {@code true} if it has been calculated, {@code false} otherwise
     */
    boolean isInitialLightingDone();

    boolean isCubeLoaded();

    boolean hasLightUpdates();

    Biome getBiome(BlockPos pos);

    /**
     * Set biome at a cube-local 2x2 block column.
     *
     * @param localBiomeX cube-local X coordinate. One unit is 2 blocks
     * @param localBiomeZ cube-local Z coordinate. One unit is 2 blocks
     * @param biome biome at the given cube coordinates
     * @deprecated Due to changes in Minecraft 1.15.x, biome storage will be changed to 1 biome per 4x4x4 blocks
     */
    @Deprecated
    void setBiome(int localBiomeX, int localBiomeZ, Biome biome);

    @Nullable
    CapabilityDispatcher getCapabilities();

    /**
     * Returns a set of reasons this cube is forced to remain loaded if it's forced to remain loaded,
     * or empty enum set if it can be unloaded.
     *
     * @return EnumSet of reasons for this cube to stay loaded. Empty if it can be unloaded.
     */
    EnumSet<ForcedLoadReason> getForceLoadStatus();

    enum ForcedLoadReason {
        SPAWN_AREA, PLAYER, MOD_TICKET, OTHER
    }
}
