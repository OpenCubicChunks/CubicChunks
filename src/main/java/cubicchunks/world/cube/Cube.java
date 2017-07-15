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
package cubicchunks.world.cube;

import static cubicchunks.util.Coords.blockToLocal;
import static cubicchunks.util.Coords.localToBlock;

import cubicchunks.CubicChunks;
import cubicchunks.lighting.LightingManager;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.util.CubePos;
import cubicchunks.util.XYZAddressable;
import cubicchunks.util.ticket.TicketList;
import cubicchunks.world.EntityContainer;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.IHeightMap;
import cubicchunks.world.column.IColumn;
import cubicchunks.worldgen.generator.ICubePrimer;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.logging.log4j.LogManager;

import com.google.common.collect.Sets;

/**
 * A cube is our extension of minecraft's chunk system to three dimensions. Each cube encloses a cubic area in the world
 * with a side length of {@link Cube#SIZE}, aligned to multiples of that length and stored within columns.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Cube implements XYZAddressable {

    @Nullable protected static final ExtendedBlockStorage NULL_STORAGE = null;

    private final Set<NextTickListEntry> pendingTickListEntriesHashSet = new HashSet<NextTickListEntry>();
    private final TreeSet<NextTickListEntry> pendingTickListEntriesTreeSet = new TreeSet<NextTickListEntry>();
    /**
     * Side length of a cube
     */
    public static final int SIZE = 16;

    /**
     * Tickets keep this chunk loaded and ticking. See the docs of {@link TicketList} and {@link
     * cubicchunks.util.ticket.ITicket} for additional information.
     */
    @Nonnull private final TicketList tickets; // tickets prevent this Cube from being unloaded
    /**
     * Has anything within the cube changed since it was loaded from disk?
     */
    private boolean isModified = false;
    /**
     * Has the cube generator's populate() method been called for this cube?
     */
    private boolean isPopulated = false;
    /**
     * Has the cube generator's populate() method been called for every cube potentially writing to this cube during
     * population?
     */
    private boolean isFullyPopulated = false;
    /**
     * Has the initial light map been calculated?
     */
    private boolean isInitialLightingDone = false;
    /**
     * The world of this cube
     */
    @Nonnull private final ICubicWorld world;
    /**
     * The column of this cube
     */
    @Nonnull private final IColumn column;
    /**
     * The position of this cube, in cube space
     */
    @Nonnull private final CubePos coords;
    /**
     * Blocks in this cube
     */
    @Nullable private ExtendedBlockStorage storage;
    /**
     * Entities in this cube
     */
    @Nonnull private final EntityContainer entities;
    /**
     * The position of tile entities in this cube, and their corresponding tile entity
     */
    @Nonnull private final Map<BlockPos, TileEntity> tileEntityMap;
    /**
     * The positions of tile entities queued for creation
     */
    @Nonnull private final ConcurrentLinkedQueue<BlockPos> tileEntityPosQueue;

    private final LightingManager.CubeLightUpdateInfo cubeLightUpdateInfo;

    /**
     * Is this cube loaded and not queued for unload
     */
    private boolean isCubeLoaded;

    /**
     * Create a new cube in the specified column at the specified location. The newly created cube will only contain air
     * blocks.
     *
     * @param column column of this cube
     * @param cubeY cube y position
     */
    public Cube(IColumn column, int cubeY) {
        this.world = column.getCubicWorld();
        this.column = column;
        this.coords = new CubePos(column.getX(), cubeY, column.getZ());

        this.tickets = new TicketList();

        this.entities = new EntityContainer();
        this.tileEntityMap = new HashMap<>();
        this.tileEntityPosQueue = new ConcurrentLinkedQueue<>();

        this.cubeLightUpdateInfo = world.getLightingManager().createCubeLightUpdateInfo(this);

        this.storage = NULL_STORAGE;
    }

    /**
     * Create a new cube at the specified location by copying blocks from a cube primer.
     *
     * @param column column of this cube
     * @param cubeY cube y position
     * @param primer primer containing the blocks for this cube
     */
    @SuppressWarnings("deprecation") // when a block is generated, does it really have any extra
    // information it could give us about its opacity by knowing its location?
    public Cube(IColumn column, int cubeY, ICubePrimer primer) {
        this(column, cubeY);

        int miny = Coords.cubeToMinBlock(cubeY);
        IHeightMap opindex = column.getOpacityIndex();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                for (int y = 15; y >= 0; y--) {
                    IBlockState newstate = primer.getBlockState(x, y, z);

                    if (newstate.getMaterial() != Material.AIR) {
                        if (storage == NULL_STORAGE) {
                            newStorage();
                        }
                        storage.set(x, y, z, newstate);

                        if (newstate.getLightOpacity() != 0) {
                            column.setModified(true); //TODO: this is a bit of am abstraction leak... maybe ServerHeightMap needs its own isModified
                            opindex.onOpacityChange(x, miny + y, z, newstate.getLightOpacity());
                        }
                    }
                }
            }
        }
        isModified = true;
    }


    /**
     * Constructor to be used from subclasses to provide all field values
     */
    protected Cube(TicketList tickers, ICubicWorld world, IColumn column, CubePos coords, ExtendedBlockStorage storage,
            EntityContainer entities, Map<BlockPos, TileEntity> tileEntityMap,
            ConcurrentLinkedQueue<BlockPos> tileEntityPosQueue, LightingManager.CubeLightUpdateInfo lightInfo) {
        this.tickets = tickers;
        this.world = world;
        this.column = column;
        this.coords = coords;
        this.storage = storage;
        this.entities = entities;
        this.tileEntityMap = tileEntityMap;
        this.tileEntityPosQueue = tileEntityPosQueue;
        this.cubeLightUpdateInfo = lightInfo;
    }

    //======================================
    //========Chunk vanilla methods=========
    //======================================

    /**
     * Retrieve the block state at the specified location
     *
     * @param pos target location
     *
     * @return The block state
     *
     * @see Cube#getBlockState(int, int, int)
     */
    public IBlockState getBlockState(BlockPos pos) {
        return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Set the block state at the specified location
     *
     * @param pos target location
     * @param newstate target state of the block at that position
     *
     * @return The the old state of the block at the position, or null if there was no change
     *
     * @see IColumn#setBlockState(BlockPos, IBlockState)
     */
    @Nullable public IBlockState setBlockState(BlockPos pos, IBlockState newstate) {
        return column.setBlockState(pos, newstate);
    }

    /**
     * Retrieve the block state at the specified location
     *
     * @param blockX block x position
     * @param localOrBlockY block or local y position
     * @param blockZ block z position
     *
     * @return The block state
     *
     * @see Cube#getBlockState(BlockPos)
     */
    public IBlockState getBlockState(int blockX, int localOrBlockY, int blockZ) {
        if (storage == NULL_STORAGE) {
            return Blocks.AIR.getDefaultState();
        }
        return storage.get(blockToLocal(blockX), blockToLocal(localOrBlockY), blockToLocal(blockZ));
    }

    /**
     * Retrieve the raw light level at the specified location
     *
     * @param lightType The type of light (sky or block light)
     * @param pos The position at which light should be checked
     *
     * @return the light level
     */
    public int getLightFor(EnumSkyBlock lightType, BlockPos pos) {
        return column.getLightFor(lightType, pos);
    }

    /**
     * Set the raw light level at the specified location
     *
     * @param lightType The type of light (sky or block light)
     * @param pos The position at which light should be updated
     * @param light the light level
     */
    public void setLightFor(EnumSkyBlock lightType, BlockPos pos, int light) {
        column.setLightFor(lightType, pos, light);
    }

    /**
     * Create a tile entity at the given position if the block is able to hold one
     *
     * @param pos position where the tile entity should be placed
     *
     * @return the created tile entity, or <code>null</code> if the block at that position does not provide tile
     * entities
     */
    @Nullable
    private TileEntity createTileEntity(BlockPos pos) {
        IBlockState blockState = getBlockState(pos);
        Block block = blockState.getBlock();

        if (block.hasTileEntity(blockState)) {
            return block.createTileEntity((World) this.world, blockState);
        }
        return null;
    }

    /**
     * Retrieve the tile entity at the specified location
     *
     * @param pos target location
     * @param createType how fast the tile entity is needed
     *
     * @return the tile entity at the specified location, or <code>null</code> if there is no entity and
     * <code>createType</code> was not {@link net.minecraft.world.chunk.Chunk.EnumCreateEntityType#IMMEDIATE}
     */
    @Nullable public TileEntity getTileEntity(BlockPos pos, Chunk.EnumCreateEntityType createType) {
        return column.getTileEntity(pos, createType);
    }

    /**
     * Add a tile entity to this cube
     *
     * @param tileEntity The tile entity to add
     */
    public void addTileEntity(TileEntity tileEntity) {
        column.addTileEntity(tileEntity);
    }

    /**
     * Tick this cube on client side
     *
     * @param tryToTickFaster Whether costly calculations should be skipped in order to catch up with ticks
     */
    @SideOnly(value = Side.CLIENT)
    public void tickCubeClient(BooleanSupplier tryToTickFaster) {
        if (!this.isInitialLightingDone && this.isPopulated) {
            this.tryDoFirstLight(); //TODO: Very icky light population code! REMOVE IT!
        }

        while (!this.tileEntityPosQueue.isEmpty()) {
            BlockPos blockpos = this.tileEntityPosQueue.poll();

            IBlockState state = this.getBlockState(blockpos);
            Block block = state.getBlock();

            if (this.getTileEntity(blockpos, Chunk.EnumCreateEntityType.CHECK) == null &&
                    block.hasTileEntity(state)) {
                TileEntity tileentity = this.createTileEntity(blockpos);
                this.world.setTileEntity(blockpos, tileentity);
                this.world.markBlockRangeForRenderUpdate(blockpos, blockpos);
            }
        }

        if (!tryToTickFaster.getAsBoolean() && this.cubeLightUpdateInfo != null) {
            this.cubeLightUpdateInfo.tick();
        }
    }

    /**
     * Tick this cube on server side. Block tick updates launched here.
     * @param currentTime - current World time
     * @param worldIn - World server containing this cube
     * @param rand - World specific Random
     */
    public void tickCubeServer(long currentTime, WorldServer worldIn, Random rand) {
        if (!isFullyPopulated) {
            return;
        }
        Iterator<NextTickListEntry> pti = pendingTickListEntriesTreeSet.iterator();
        while (pti.hasNext()) {
            NextTickListEntry ntle = pti.next();
            if (ntle.scheduledTime > currentTime)
                return;
            BlockPos pos = ntle.position;
            IBlockState iblockstate = this.storage.get(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
            if (iblockstate.getMaterial() != Material.AIR && iblockstate.getBlock() == ntle.getBlock()) {
                iblockstate.getBlock().updateTick(worldIn, ntle.position, iblockstate, rand);
            }
            pti.remove();
        }
        int tickLimit = 256; // 16x16 per cube per tick will be enough
        pti = pendingTickListEntriesHashSet.iterator();
        while (pti.hasNext() && --tickLimit != 0) {
            NextTickListEntry ntle = pti.next();
            pendingTickListEntriesTreeSet.add(ntle);
            pti.remove();
        }
    }

    public void scheduleUpdate(BlockPos pos, Block blockIn, int delay, int priority) {
        if (pos instanceof BlockPos.MutableBlockPos || pos instanceof BlockPos.PooledMutableBlockPos) {
            pos = new BlockPos(pos);
            LogManager.getLogger().warn((String) "Tried to assign a mutable BlockPos to tick data...",
                    (Throwable) (new Error(pos.getClass().toString())));
        }
        if (world.getCubeCache().getLoadedCube(CubePos.fromBlockCoords(pos)) != null) {
            NextTickListEntry nextticklistentry = new NextTickListEntry(pos, blockIn);
            nextticklistentry.setScheduledTime((long) delay + world.getTotalWorldTime());
            nextticklistentry.setPriority(priority);
            this.pendingTickListEntriesHashSet.add(nextticklistentry);
        }
    }

    /**
     * Calculate diffuse skylight in this cube if surrounding cubes are loaded
     */
    //TODO: Redo light population code
    private void tryDoFirstLight() {
        BlockPos pos = this.getCoords().getMinBlockPos();
        final int radius = 17;
        // TODO replace hardcoded constant with reference to Cube#SIZE
        if (!world.isAreaLoaded(pos.add(-radius, -radius, -radius), pos.add(15 + radius, 15 + radius, 15 + radius))) {
            return;
        }
        //client cubes (setClientCube) are always fully generated, isInitialLightingDone is never false
        ((ICubicWorldServer) this.world).getFirstLightProcessor().diffuseSkylight(this);
        this.isInitialLightingDone = true;
    }

    //=================================
    //=========Other methods===========
    //=================================

    /**
     * Check if there are any non-air blocks in this cube
     *
     * @return <code>true</code> if this cube contains only air blocks, <code>false</code> otherwise
     */
    public boolean isEmpty() {
        return storage == null || this.storage.isEmpty();
    }

    /**
     * Convert an integer-encoded address to a local block to a global block position
     *
     * @param localAddress the address of the block
     *
     * @return the block position
     */
    public BlockPos localAddressToBlockPos(int localAddress) {
        int x = Coords.localToBlock(this.coords.getX(), AddressTools.getLocalX(localAddress));
        int y = Coords.localToBlock(this.coords.getY(), AddressTools.getLocalY(localAddress));
        int z = Coords.localToBlock(this.coords.getZ(), AddressTools.getLocalZ(localAddress));
        return new BlockPos(x, y, z);
    }

    /**
     * @return this cube's world
     */
    public ICubicWorld getCubicWorld() {
        return this.world;
    }

    /**
     * @return this cube's column
     */
    public IColumn getColumn() {
        return this.column;
    }

    /**
     * Retrieve this cube's x coordinate in cube space
     *
     * @return cube x position
     */
    public int getX() {
        return this.coords.getX();
    }

    /**
     * Retrieve this cube's y coordinate in cube space
     *
     * @return cube y position
     */
    public int getY() {
        return this.coords.getY();
    }

    /**
     * Retrieve this cube's z coordinate in cube space
     *
     * @return cube z position
     */
    public int getZ() {
        return this.coords.getZ();
    }

    /**
     * @return this cube's position
     */
    public CubePos getCoords() {
        return this.coords;
    }

    /**
     * Check whether a given global block position is contained in this cube
     *
     * @param blockPos the position of the block
     *
     * @return <code>true</code> if the position is within this cube, <code>false</code> otherwise
     */
    public boolean containsBlockPos(BlockPos blockPos) {
        return this.coords.getX() == Coords.blockToCube(blockPos.getX())
                && this.coords.getY() == Coords.blockToCube(blockPos.getY())
                && this.coords.getZ() == Coords.blockToCube(blockPos.getZ());
    }

    @Nullable public ExtendedBlockStorage getStorage() {
        return this.storage;
    }

    @Nullable public ExtendedBlockStorage setStorage(@Nullable ExtendedBlockStorage ebs) {
        this.isModified = true;
        return this.storage = ebs;
    }

    private void newStorage() {
        storage = new ExtendedBlockStorage(Coords.cubeToMinBlock(getY()), world.getProvider().hasSkyLight());
    }

    /**
     * Retrieve a map of positions to their respective tile entities
     *
     * @return a map containing all tile entities in this cube
     */
    public Map<BlockPos, TileEntity> getTileEntityMap() {
        return this.tileEntityMap;
    }

    /**
     * Retrieve a list of entities in this cube
     *
     * @return the entities' container
     */
    public EntityContainer getEntityContainer() {
        return this.entities;
    }

    /**
     * Finish the cube loading process
     */
    public void onLoad() {
        if (isCubeLoaded) {
            CubicChunks.LOGGER.error("Attempting to load already loaded cube at " + this.getCoords());
            return;
        }
        // tell the world about tile entities
        this.world.addTileEntities(this.tileEntityMap.values());
        this.world.loadEntities(this.entities.getEntities());
        this.isCubeLoaded = true;
    }

    /**
     * Mark this cube as no longer part of this world
     */
    public void onUnload() {
        if (!isCubeLoaded) {
            CubicChunks.LOGGER.error("Attempting to unload already unloaded cube at " + this.getCoords());
            return;
        }
        //first mark as unloaded so that entity list and tile entity map isn't modified while iterating
        //and it also preserves all entities/time entities so they can be saved
        this.isCubeLoaded = false;

        // tell the world to forget about entities
        this.world.unloadEntities(this.entities.getEntities());

        for (Entity entity : this.entities.getEntities()) {
            //CHECKED: 1.10.2-12.18.1.2092
            entity.addedToChunk = false; // World tries to remove entities from Cubes
            // if (addedToCube || Column is loaded)
            // so we need to set addedToChunk to false as a hack!
            // else World would reload this Cube!
        }

        // tell the world to forget about tile entities
        for (TileEntity blockEntity : this.tileEntityMap.values()) {
            this.world.removeTileEntity(blockEntity.getPos());
        }
    }

    /**
     * Check if any modifications happened to this cube since it was loaded from disk
     *
     * @return <code>true</code> if this cube should be written back to disk
     */
    public boolean needsSaving() {
        return this.entities.needsSaving(true, this.world.getTotalWorldTime(), this.isModified);
    }

    /**
     * Mark this cube as saved to disk
     */
    public void markSaved() {
        this.entities.markSaved(this.world.getTotalWorldTime());
        this.isModified = false;
    }

    /**
     * Mark this cube as one, who need to be saved to disk
     */
    public void markDirty() {
        this.isModified = true;
    }

    /**
     * Retrieve a list of tickets currently holding this cube loaded
     *
     * @return the list of tickets
     */
    public TicketList getTickets() {
        return tickets;
    }

    public void markForRenderUpdate() {
        this.world.markBlockRangeForRenderUpdate(
                Coords.cubeToMinBlock(this.coords.getX()), Coords.cubeToMinBlock(this.coords.getY()), Coords.cubeToMinBlock(this.coords.getZ()),
                Coords.cubeToMaxBlock(this.coords.getX()), Coords.cubeToMaxBlock(this.coords.getY()), Coords.cubeToMaxBlock(this.coords.getZ())
        );
    }

    /**
     * Return a seed for random number generation. This seed is persistent across server restarts and returns the same
     * value on every call for any given cube in any given world.
     *
     * @return this cube's random seed
     */
    public long cubeRandomSeed() {
        long hash = 3;
        hash = 41 * hash + this.world.getSeed();
        hash = 41 * hash + getX();
        hash = 41 * hash + getY();
        return 41 * hash + getZ();
    }

    @Nullable
    public LightingManager.CubeLightUpdateInfo getCubeLightUpdateInfo() {
        return this.cubeLightUpdateInfo;
    }

    /**
     * Mark this cube as a client side cube. Less work is done in this case, as we expect to receive updates from the
     * server
     */
    public void setClientCube() {
        this.isPopulated = true;
        this.isFullyPopulated = true;
        this.isInitialLightingDone = true;
    }

    /**
     * Check whether this cube was populated, i.e. if this cube was passed as argument to {@link
     * cubicchunks.worldgen.generator.ICubeGenerator#populate(Cube)}. Check there for more information regarding
     * population.
     *
     * @return <code>true</code> if this cube has been populated, <code>false</code> otherwise
     */
    public boolean isPopulated() {
        return isPopulated;
    }

    /**
     * Mark this cube as populated. This means that this cube was passed as argument to {@link
     * cubicchunks.worldgen.generator.ICubeGenerator#populate(Cube)}. Check there for more information regarding
     * population.
     *
     * @param populated whether this cube was populated
     */
    public void setPopulated(boolean populated) {
        this.isPopulated = populated;
        this.isModified = true;
    }

    /**
     * Check whether this cube was fully populated, i.e. if any cube potentially writing to this cube was passed as an
     * argument to {@link cubicchunks.worldgen.generator.ICubeGenerator#populate(Cube)}. Check there for more
     * information regarding population
     *
     * @return <code>true</code> if this cube has been populated, <code>false</code> otherwise
     */
    public boolean isFullyPopulated() {
        return this.isFullyPopulated;
    }

    /**
     * Mark this cube as fully populated. This means that any cube potentially writing to this cube was passed as an
     * argument to {@link cubicchunks.worldgen.generator.ICubeGenerator#populate(Cube)}. Check there for more
     * information regarding population
     *
     * @param populated whether this cube was fully populated
     */
    public void setFullyPopulated(boolean populated) {
        this.isFullyPopulated = populated;
        this.isModified = true;
    }

    /**
     * Check whether this cube's initial diffuse skylight has been calculated
     *
     * @return <code>true</code> if it has been calculated, <code>false</code> otherwise
     */
    public boolean isInitialLightingDone() {
        return isInitialLightingDone;
    }

    /**
     * Notify this cube that it's initial diffuse skylight has been calculated
     */
    public void setInitialLightingDone(boolean initialLightingDone) {
        this.isInitialLightingDone = initialLightingDone;
        this.isModified = true;
    }

    public void setCubeLoaded() {
        this.isCubeLoaded = true;
    }

    public boolean isCubeLoaded() {
        return this.isCubeLoaded;
    }
}
