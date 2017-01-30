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
import cubicchunks.lighting.LightingManager;
import cubicchunks.util.Coords;
import cubicchunks.world.ClientHeightMap;
import cubicchunks.world.ICubeProvider;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.IHeightMap;
import cubicchunks.world.ServerHeightMap;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A quasi-chunk, representing an infinitely tall section of the world.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Column extends Chunk implements IColumn {

    @Nonnull private final CubeMap cubeMap;
    @Nonnull private final IHeightMap opacityIndex;

    @Nonnull private final ICubeProvider provider;
    @Nonnull private final ICubicWorld world;

    @Nonnull private final LightingManager lightManager;

    public Column(ICubeProvider provider, ICubicWorld world, int x, int z) {
        // NOTE: this constructor is called by the chunk loader
        super((World) world, x, z);

        this.provider = provider;
        this.world = world;
        this.lightManager = world.getLightingManager();

        this.cubeMap = new CubeMap();
        //clientside we don't really need that much data. we actually only need top and bottom block Y positions
        if (this.getWorld().isRemote) {
            this.opacityIndex = new ClientHeightMap(this);
        } else {
            this.opacityIndex = new ServerHeightMap();
        }

        // make sure no one's using data structures that have been replaced
        // also saves memory
        /*
         * TODO: setting these vars to null would save memory, also... make sure we're actually
         * not using them
         */
        // this.chunkSections = null;
        // this.heightMap = null;
        // this.skylightUpdateMap = null;

        Arrays.fill(super.getBiomeArray(), (byte) -1);
    }

    //=================================================
    //===============VANILLA METHODS===================
    //=================================================

    @Override
    public int getHeight(BlockPos pos) {
        return this.getHeightValue(
                Coords.blockToLocal(pos.getX()),
                Coords.blockToLocal(pos.getZ()));
    }

    @Override
    public int getHeightValue(int localX, int localZ) {
        // NOTE: the "height value" here is the height of the transparent block
        // on top of the highest non-transparent block
        return opacityIndex.getTopBlockY(localX, localZ) + 1;
    }

    @Override
    @Deprecated
    // TODO: stop this method form being used by vanilla (any algorithms in vanilla that use it are be broken any way)
    // don't use this! It's only here because vanilla needs it
    // CHECKED: 1.11-13.19.0.2148
    public int getTopFilledSegment() {
        //NOTE: this method actually returns block Y coords

        int blockY = Coords.NO_HEIGHT;
        for (int localX = 0; localX < Cube.SIZE; localX++) {
            for (int localZ = 0; localZ < Cube.SIZE; localZ++) {
                int y = this.opacityIndex.getTopBlockY(localX, localZ);
                if (y > blockY) {
                    blockY = y;
                }
            }
        }
        if (blockY < getCubicWorld().getMinHeight()) {
            // PANIC!
            // this column doesn't have any blocks in it that aren't air!
            // but we can't return null here because vanilla code expects there to be a surface down there somewhere
            // we don't actually know where the surface is yet, because maybe it hasn't been generated
            // but we do know that the surface has to be at least at sea level,
            // so let's go with that for now and hope for the best

            return Coords.cubeToMinBlock(Coords.blockToCube(this.getCubicWorld().getProvider().getAverageGroundLevel()));
        }
        return Coords.cubeToMinBlock(Coords.blockToCube(blockY)); // return the lowest block in the Cube (kinda weird I know)
    }

    @Override
    @Deprecated // Vanilla can safely use this for block ticking, but just try to avoid it!
    // CHECKED: 1.11-13.19.0.2148
    public ExtendedBlockStorage[] getBlockStorageArray() {
        return cubeMap.getStoragesToTick();
    }

    @SideOnly(Side.CLIENT)
    protected void generateHeightMap() {
        //this method reduces to no-op with CubicChunks, heightmap is generated in real time
    }

    @Override
    @Deprecated
    public void generateSkylightMap() {
        throw new UnsupportedOperationException("Functionality of this method is replaced with LightingManager");
    }

    // CHECKED: 1.11-13.19.0.2148
    // this is here so that it's not hidden when updating
    @Override public int getBlockLightOpacity(BlockPos pos) {
        return super.getBlockLightOpacity(pos);
    }


    @Override
    public IBlockState getBlockState(BlockPos pos) {
        return super.getBlockState(pos);
    }

    @Override
    public IBlockState getBlockState(final int blockX, final int blockY, final int blockZ) {
        //forward to cube
        return this.getCube(Coords.blockToCube(blockY)).getBlockState(blockX, blockY, blockZ);
    }

    @Nullable @Override public IBlockState setBlockState(BlockPos pos, IBlockState newstate) {
        // TODO: Move all setBlockState logic to Cube
        Cube cube = getCube(Coords.blockToCube(pos.getY()));
        IBlockState oldstate = cube.getBlockState(pos);

        // get the old opacity for use when updating the heightmap,
        // this has to be done before setBlockState so that the old TileEntity is still there
        int oldOpacity = oldstate.getLightOpacity(this.getWorld(), pos);

        oldstate = cube.setBlockStateDirect(pos, newstate); // forward to cube
        if (oldstate == null) {
            // Nothing changed
            return null;
        }
        // vanilla does light updates before handling TileEntities and calling onBlockOadded
        // doing it differently shouldn't cause issues
        this.lightManager.doOnBlockSetLightUpdates(this, pos, newstate, oldOpacity);

        return oldstate;
    }

    @Override
    public int getLightFor(EnumSkyBlock type, BlockPos pos) {
        //forward to cube
        return getCube(pos).getLightFor(type, pos);
    }

    @Override
    public void setLightFor(EnumSkyBlock type, BlockPos pos, int value) {
        //forward to cube
        getCube(pos).setLightFor(type, pos, value);
    }

    @Override
    public int getLightSubtracted(BlockPos pos, int amount) {
        //forward to cube
        return getCube(pos).getLightSubtracted(pos, amount);
    }

    @Override
    public void addEntity(Entity entity) {
        //forward to cube
        getCube(Coords.getCubeYForEntity(entity)).addEntity(entity);
    }

    @Override
    public void removeEntity(Entity entityIn) {
        this.removeEntityAtIndex(entityIn, entityIn.chunkCoordY);
    }

    @Override
    public void removeEntityAtIndex(Entity entity, int cubeY) {
        //forward to cube
        getCube(cubeY).removeEntity(entity);
    }

    @Override
    public boolean canSeeSky(BlockPos pos) {
        int height = this.getHeightValue(
                Coords.blockToLocal(pos.getX()),
                Coords.blockToLocal(pos.getZ()));

        return pos.getY() >= height;
    }

    @Nullable @Override
    public TileEntity getTileEntity(BlockPos pos, Chunk.EnumCreateEntityType createType) {
        //forward to cube
        return getCube(pos).getTileEntity(pos, createType);
    }

    @Override
    public void addTileEntity(TileEntity tileEntity) {
        // pass off to the cube
        getCube(tileEntity.getPos()).addTileEntity(tileEntity);
    }

    @Override
    public void addTileEntity(BlockPos pos, TileEntity blockEntity) {
        // pass off to the cube
        getCube(pos).addTileEntity(pos, blockEntity);
    }

    @Override
    public void removeTileEntity(BlockPos pos) {
        //forward to cube
        this.getCube(pos).removeTileEntity(pos);
    }

    @Override
    public void onChunkLoad() {
        this.isChunkLoaded = true;
        MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(this));
    }

    @Override
    public void onChunkUnload() {
        this.isChunkLoaded = false;
        MinecraftForge.EVENT_BUS.post(new ChunkEvent.Unload(this));
    }

    //setChunkModified() goes here, it's unchanged

    @Override
    public void getEntitiesWithinAABBForEntity(@Nullable Entity exclude, AxisAlignedBB queryBox, List<Entity> out,
            @Nullable Predicate<? super Entity> predicate) {

        // get a y-range that 2 blocks wider than the box for safety
        int minCubeY = Coords.blockToCube(MathHelper.floor(queryBox.minY - World.MAX_ENTITY_RADIUS));
        int maxCubeY = Coords.blockToCube(MathHelper.floor(queryBox.maxY + World.MAX_ENTITY_RADIUS));

        for (int cubeY = minCubeY; cubeY <= maxCubeY; cubeY++) {
            Cube cube = getCube(cubeY);
            cube.getEntitiesWithinAABBForEntity(exclude, queryBox, out, predicate);
        }
    }

    @Override
    public <T extends Entity> void getEntitiesOfTypeWithinAAAB(Class<? extends T> entityType, AxisAlignedBB queryBox, List<T> out,
            @Nullable Predicate<? super T> predicate) {

        // get a y-range that 2 blocks wider than the box for safety
        int minCubeY = Coords.blockToCube(MathHelper.floor(queryBox.minY - World.MAX_ENTITY_RADIUS));
        int maxCubeY = Coords.blockToCube(MathHelper.floor(queryBox.maxY + World.MAX_ENTITY_RADIUS));

        for (int cubeY = minCubeY; cubeY < maxCubeY + 1; cubeY++) {
            Cube cube = getCube(cubeY);
            cube.getEntitiesOfTypeWithinAAAB(entityType, queryBox, out, predicate);
        }
    }

    @Override
    public boolean needsSaving(boolean flag) {
        return this.isModified;
    }

    //getRandomWithSeed(seed) doesn't need changes

    //isEmpty() doesn't need changes

    @Override
    @Deprecated
    public void populateChunk(IChunkProvider chunkProvider, IChunkGenerator chunkGenerator) {
        throw new UnsupportedOperationException("This method is incompatible with CubicChunks");
    }

    @Override
    //TODO: Actual precipitation heightmap, currently skylight heightmap is used which triggers an old MC alpha bug
    public BlockPos getPrecipitationHeight(BlockPos pos) {
        return new BlockPos(pos.getX(), this.getHeight(pos), pos.getZ());
    }

    @Override
    public void onTick(boolean tryToTickFaster) {
        this.chunkTicked = true;
        cubeMap.forEach((c) -> c.tickCube(tryToTickFaster));
    }

    @Override
    @Deprecated
    public boolean isPopulated() {
        return true; //stub... its broken and only used in World.markAndNotifyBlock()
    }

    //isCHunkTicked() doesn't need changes

    //getChunkCoordIntPair doesn't need changes

    @Override
    // used for by ChunkCache, and that is used for rendering to see
    // if there are any blocks, or is there just air
    // CHECKED: 1.11-13.19.0.2148
    public boolean getAreLevelsEmpty(int minBlockY, int maxBlockY) {
        int minCubeY = Coords.blockToCube(minBlockY);
        int maxCubeY = Coords.blockToCube(maxBlockY);
        for (int cubeY = minCubeY; cubeY <= maxCubeY; cubeY++) {
            Cube cube = getCube(cubeY); // yes, load/generate a chunk if there is none...
            // even if its not loaded there is still technical something there
            if (!cube.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Deprecated
    public void setStorageArrays(ExtendedBlockStorage[] newArray) {
        throw new UnsupportedOperationException("This method is incompatible with Cubic Chunks");
    }

    @Override
    @Deprecated
    @SideOnly(Side.CLIENT)
    public void fillChunk(PacketBuffer buf, int p_186033_2_, boolean p_186033_3_) {
        throw new UnsupportedOperationException("This method is incompatible with Cubic Chunks");
    }

    //getBiome doesn't need changes

    //getBiomeArray doesn't need changes

    //setBiomeArray doesn't need changes


    // TODO: lighting should not be done in Column
    @Override
    @Deprecated
    public void resetRelightChecks() {
        throw new UnsupportedOperationException("This method is incompatible with Cubic Chunks");
    }

    //TODO: enqueueRelightChecks() must die! (it should be in its own lighting system or at least only in Cube)
    @Override
    @Deprecated
    public void enqueueRelightChecks() {
        // stub
    }

    @Override
    @Deprecated
    public void checkLight() {
        //no-op on cubic chunks
    }

    //isLoaded doesn't need changes

    //getWorld doesn't need changes

    @Override
    @Deprecated // TODO: only used by IONbtReader and IONbtWriter ... and those are super broken
    //       add throw new UnsupportedOperationException();
    public int[] getHeightMap() {
        return this.opacityIndex.getHeightmap();
    }

    @Override
    @Deprecated
    public void setHeightMap(int[] newHeightMap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<BlockPos, TileEntity> getTileEntityMap() {
        //TODO: Important: Fix getTileEntityMap. Need to implement special Map that accesses tile entities from cubeMap
        return super.getTileEntityMap();
    }

    /**
     * Retrieve a list of all entities in this column
     *
     * @return the list of entities
     */
    @Override
    public ClassInheritanceMultiMap<Entity>[] getEntityLists() {
        //TODO: need to make it returns something that contains correct data
        //Forge needs it and editing Forge classes with ASM is a bad idea
        return super.getEntityLists();
    }

    @Override
    @Deprecated //TODO: only used in PlayerCubeMap.getChunkIterator() (see todo's in that)
    //      add throw new UnsupportedOperationException();
    public boolean isTerrainPopulated() {
        //with cubic chunks the whole column is never fully generated,
        //this method is currently used to determine list of chunks to be ticked
        //and PlayerCubeMap :(
        return true; //TODO: stub, replace with new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean isLightPopulated() {
        //with cubic chunks light is never generated in the whole column
        //this method is currently used to determine list of chunks to be ticked
        //and PlayerCubeMap :(
        return true; //TODO: stub, replace with new UnsupportedOperationException();
    }

    @Override public boolean shouldTick() {
        for (Cube cube : cubeMap) {
            if (cube.getTickets().shouldTick()) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Deprecated
    public void removeInvalidTileEntity(BlockPos pos) {
        throw new UnsupportedOperationException("Not implemented because not used");
    }

    //===========================================
    //===========CubicChunks methods=============
    //===========================================

    @Override public int getX() {
        return this.xPosition;
    }

    @Override public int getZ() {
        return this.zPosition;
    }

    @Override public IHeightMap getOpacityIndex() {
        return this.opacityIndex;
    }

    @Override public Collection<Cube> getLoadedCubes() {
        return this.cubeMap.all();
    }


    // =========================================
    // =======Mini CubeCache like methods=======
    // =========================================

    @Override public Iterable<Cube> getLoadedCubes(int startY, int endY) {
        return this.cubeMap.cubes(startY, endY);
    }

    @Override @Nullable
    public Cube getLoadedCube(int cubeY) {
        return provider.getLoadedCube(getX(), cubeY, getZ());
    }

    @Override public Cube getCube(int cubeY) {
        return provider.getCube(getX(), cubeY, getZ());
    }

    /**
     * Retrieve the cube containing the specified block
     *
     * @param pos the target block position
     *
     * @return the cube containing that block
     */
    private Cube getCube(BlockPos pos) {
        return getCube(Coords.blockToCube(pos.getY()));
    }

    @Override public void addCube(Cube cube) {
        this.cubeMap.put(cube);
    }

    @Override @Nullable public Cube removeCube(int cubeY) {
        return this.cubeMap.remove(cubeY);
    }

    @Override public boolean hasLoadedCubes() {
        return !this.cubeMap.isEmpty();
    }

    // ======= end cube cache like methods =======
    // ===========================================

    @Override public void markSaved() {
        this.setModified(false);
    }

    @Override
    public int getLowestHeight() {
        return opacityIndex.getLowestTopBlockY();
    }

    @Override public ICubicWorld getCubicWorld() {
        return world;
    }

    @Override public void markUnloaded(boolean unloaded) {
        this.unloaded = unloaded;
    }

    /**
     * Returns the column position
     */
    @Override public ChunkPos getPos() {
        return getChunkCoordIntPair();
    }
}
