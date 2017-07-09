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

import static cubicchunks.util.Coords.blockToCube;
import static cubicchunks.util.Coords.blockToLocal;

import cubicchunks.lighting.LightingManager;
import cubicchunks.util.CubePos;
import cubicchunks.world.ICubeProvider;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.NotCubicChunksWorldException;
import cubicchunks.world.cube.Cube;
import cubicchunks.world.provider.ICubicWorldProvider;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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
public abstract class MixinWorld implements ICubicWorld {

    @Shadow protected IChunkProvider chunkProvider;
    @Shadow @Final @Mutable public WorldProvider provider;
    @Shadow @Final public Random rand;
    @Shadow @Final public boolean isRemote;
    @Shadow @Final public Profiler profiler;
    @Shadow protected int updateLCG;
    @Shadow @Final @Mutable protected ISaveHandler saveHandler;
    @Shadow protected boolean findingSpawnPoint;
    @Shadow @Final public List<Entity> loadedEntityList;
    @Shadow @Final protected List<Entity> unloadedEntityList;

    @Shadow protected abstract boolean isChunkLoaded(int i, int i1, boolean allowEmpty);

    @Nullable private LightingManager lightingManager;
    protected boolean isCubicWorld;
    protected int minHeight = 0, maxHeight = 256;

    @Override public void initCubicWorld(int minHeight1, int maxHeight1) {
        // Set the world height boundaries to their highest and lowest values respectively
        this.minHeight = minHeight1;
        this.maxHeight = maxHeight1;
        //has to be created early so that creating BlankCube won't crash
        this.lightingManager = new LightingManager(this);
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
    
    /** Update LCG value and return updated **/
    @Override public int updateLCG(){
        this.updateLCG = this.updateLCG * 3 + 1013904223;
        return this.updateLCG;
    }

    @Override public ICubeProvider getCubeCache() {
        if (!this.isCubicWorld()) {
            throw new NotCubicChunksWorldException();
        }
        return (ICubeProvider) this.chunkProvider;
    }

    @Override public LightingManager getLightingManager() {
        if (!this.isCubicWorld()) {
            throw new NotCubicChunksWorldException();
        }
        assert this.lightingManager != null;
        return this.lightingManager;
    }

    @Override
    public boolean testForCubes(CubePos start, CubePos end, Predicate<Cube> cubeAllowed) {
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

    //vanilla field accessors

    @Override public WorldProvider getProvider() {
        return this.provider;
    }

    @Override public Random getRand() {
        return this.rand;
    }

    @Override public boolean isRemote() {
        return this.isRemote;
    }

    @Override public List<EntityPlayer> getPlayerEntities() {
        return ((World) (Object) this).playerEntities;
    }

    @Override public Profiler getProfiler() {
        return this.profiler;
    }
    
    /**
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
    
    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    public void getBlockState(BlockPos pos, CallbackInfoReturnable<IBlockState> ci){
        if (this.isCubicWorld()) {
            ci.setReturnValue(this.getCubeCache().getCube(CubePos.fromBlockCoords(pos)).getBlockState(pos));
            ci.cancel();
        }
    }
    
    @Inject(method = "func_191504_a", at = @At("HEAD"), cancellable = true)
    private void addBlocksCollisionBoundingBoxesToList(@Nullable Entity entity, AxisAlignedBB aabb, boolean breakOnWorldBorder,
            @Nullable List<AxisAlignedBB> aabbList, CallbackInfoReturnable<Boolean> ci) {
        if (this.isCubicWorld()) {
            double minX = aabb.minX;
            double minY = aabb.minY;
            double minZ = aabb.minZ;
            double maxX = aabb.maxX;
            double maxY = aabb.maxY;
            double maxZ = aabb.maxZ;
            int x1 = (int) minX - 1;
            int y1 = (int) minY - 1;
            int z1 = (int) minZ - 1;
            int x2 = (int) maxX;
            int y2 = (int) maxY;
            int z2 = (int) maxZ;
            BlockPos.PooledMutableBlockPos pooledmutableblockpos = BlockPos.PooledMutableBlockPos.retain();
            for (int cx = x1 >> 4; cx <= x2 >> 4; cx++)
                for (int cy = y1 >> 4; cy <= y2 >> 4; cy++)
                    for (int cz = z1 >> 4; cz <= z2 >> 4; cz++) {
                        CubePos coords = new CubePos(cx, cy, cz);
                        int minBlockX = coords.getMinBlockX();
                        int minBlockY = coords.getMinBlockY();
                        int minBlockZ = coords.getMinBlockZ();
                        int maxBlockX = coords.getMaxBlockX();
                        int maxBlockY = coords.getMaxBlockY();
                        int maxBlockZ = coords.getMaxBlockZ();
                        Cube loadedCube = this.getCubeCache().getLoadedCube(coords);
                        if (loadedCube != null) {
                            minBlockX = minBlockX > x1 ? minBlockX : x1;
                            minBlockY = minBlockY > y1 ? minBlockY : y1;
                            minBlockZ = minBlockZ > z1 ? minBlockZ : z1;
                            maxBlockX = maxBlockX < x2 ? maxBlockX : x2;
                            maxBlockY = maxBlockY < y2 ? maxBlockY : y2;
                            maxBlockZ = maxBlockZ < z2 ? maxBlockZ : z2;
                            for (int x = minBlockX; x <= maxBlockX; x++)
                                for (int y = minBlockY; y <= maxBlockY; y++)
                                    for (int z = minBlockZ; z <= maxBlockZ; z++) {
                                        pooledmutableblockpos.setPos(x, y, z);
                                        IBlockState bstate = loadedCube.getStorage().get(x & 15, y & 15, z & 15);
                                        bstate.addCollisionBoxToList((World) (Object) this, pooledmutableblockpos, aabb, aabbList, entity, false);
                                        net.minecraftforge.common.MinecraftForge.EVENT_BUS
                                                .post(new net.minecraftforge.event.world.GetCollisionBoxesEvent((World) (Object) this, null, aabb,
                                                        aabbList));
                                    }
                        } else {
                            AxisAlignedBB unloadedCubeAABB = new AxisAlignedBB(minBlockX, minBlockY, minBlockZ, maxBlockX, maxBlockY, maxBlockZ);
                            if (unloadedCubeAABB.intersectsWith(aabb))
                                aabbList.add(unloadedCubeAABB);
                        }
                    }
            pooledmutableblockpos.release();
            ci.setReturnValue(!aabbList.isEmpty());
            ci.cancel();
        }
    }
    
    @Inject(method = "getEntitiesInAABBexcluding", at = @At("HEAD"), cancellable = true)
    private void onGetEntitiesInAABBexcluding(@Nullable Entity entityIn, AxisAlignedBB aabb,
            @Nullable com.google.common.base.Predicate<? super Entity> predicate, CallbackInfoReturnable<List<Entity>> ci) {
        if (this.isCubicWorld()) {
            List<Entity> list = new ArrayList<Entity>();
            int x1 = (int)(aabb.minX - World.MAX_ENTITY_RADIUS);
            int y1 = (int)(aabb.minY - World.MAX_ENTITY_RADIUS);
            int z1 = (int)(aabb.minZ - World.MAX_ENTITY_RADIUS);
            int x2 = (int)(aabb.maxX + World.MAX_ENTITY_RADIUS);
            int y2 = (int)(aabb.maxY + World.MAX_ENTITY_RADIUS);
            int z2 = (int)(aabb.maxZ + World.MAX_ENTITY_RADIUS);
            x1>>=4;
            y1>>=4;
            z1>>=4;
            x2>>=4;
            y2>>=4;
            z2>>=4;
            for (int cx = x1; cx <= x2; cx++)
                for (int cy = y1; cy <= y2; cy++)
                    for (int cz = z1; cz <= z2; cz++) {
                        Cube loadedCube = this.getCubeCache().getLoadedCube(cx, cy, cz);
                        if (loadedCube != null) {
                            for (Entity entity : loadedCube.getEntityContainer().getEntities()) {
                                if (entity != entityIn && entity.getEntityBoundingBox().intersectsWith(aabb)) {
                                    if (predicate == null || predicate.apply(entity)) {
                                        list.add(entity);
                                    } else {
                                        Entity[] parts = entity.getParts();
                                        if (parts != null)
                                            for (Entity entityPart : parts) {
                                                if (entityPart != entityIn && entityPart.getEntityBoundingBox().intersectsWith(aabb)
                                                        && (predicate == null || predicate.apply(entityPart)))
                                                    list.add(entity);
                                            }
                                    }
                                }
                            }
                        }
                    }
            ci.setReturnValue(list);
            ci.cancel();
        }
    }
    
    @Shadow public abstract void onEntityRemoved(Entity entityIn);
    @Shadow protected abstract void tickPlayers();
    @Shadow public abstract void updateEntity(Entity ent);
    @Shadow public abstract void removeEntity(Entity entityIn);

    @Inject(method = "updateEntities", at = @At(value = "HEAD"), require = 1, cancellable = true)
    private void updateEntitiesHandler(CallbackInfo cbi) {
        if(this.isCubicWorld){
            System.out.println("update tick");
            this.profiler.startSection("entities");
            this.profiler.startSection("global");
            this.profiler.endStartSection("remove");
            this.loadedEntityList.removeAll(this.unloadedEntityList);
            for (int k = 0; k < this.unloadedEntityList.size(); ++k) {
                Entity entity1 = (Entity) this.unloadedEntityList.get(k);
                int cubeX = entity1.chunkCoordX;
                int cubeY = entity1.chunkCoordY;
                int cubeZ = entity1.chunkCoordZ;
                Cube loadedCube = this.getCubeCache().getLoadedCube(cubeX, cubeY, cubeZ);
                if (entity1.addedToChunk && loadedCube!=null) {
                    loadedCube.getEntityContainer().remove(entity1);
                }
            }

            for (int l = 0; l < this.unloadedEntityList.size(); ++l)
            {
                this.onEntityRemoved((Entity)this.unloadedEntityList.get(l));
            }

            this.unloadedEntityList.clear();
            this.tickPlayers();
            this.profiler.endStartSection("regular");

            for (int i1 = 0; i1 < this.loadedEntityList.size(); ++i1)
            {
                Entity entity2 = (Entity)this.loadedEntityList.get(i1);
                Entity entity3 = entity2.getRidingEntity();

                if (entity3 != null)
                {
                    if (!entity3.isDead && entity3.isPassenger(entity2))
                    {
                        continue;
                    }

                    entity2.dismountRidingEntity();
                }

                this.profiler.startSection("tick");

                if (!entity2.isDead && !(entity2 instanceof EntityPlayerMP))
                {
                    try
                    {
                        this.updateEntity(entity2);
                    }
                    catch (Throwable throwable1)
                    {
                        CrashReport crashreport1 = CrashReport.makeCrashReport(throwable1, "Ticking entity");
                        CrashReportCategory crashreportcategory1 = crashreport1.makeCategory("Entity being ticked");
                        entity2.addEntityCrashInfo(crashreportcategory1);
                        if (net.minecraftforge.common.ForgeModContainer.removeErroringEntities)
                        {
                            net.minecraftforge.fml.common.FMLLog.severe(crashreport1.getCompleteReport());
                            removeEntity(entity2);
                        }
                        else
                        throw new ReportedException(crashreport1);
                    }
                }

                this.profiler.endSection();
                this.profiler.startSection("remove");

                if (entity2.isDead)
                {
                    int cubeX = entity2.chunkCoordX;
                    int cubeY = entity2.chunkCoordY;
                    int cubeZ = entity2.chunkCoordZ;
                    Cube loadedCube = this.getCubeCache().getLoadedCube(cubeX, cubeY, cubeZ);
                    if (entity2.addedToChunk && loadedCube!=null) {
                        loadedCube.getEntityContainer().remove(entity2);
                    }

                    this.loadedEntityList.remove(i1--);
                    this.onEntityRemoved(entity2);
                }

                this.profiler.endSection();
            }

            this.profiler.endStartSection("blockEntities");
/*            this.processingLoadedTiles = true;
            Iterator<TileEntity> iterator = this.tickableTileEntities.iterator();

            while (iterator.hasNext())
            {
                TileEntity tileentity = (TileEntity)iterator.next();

                if (!tileentity.isInvalid() && tileentity.hasWorld())
                {
                    BlockPos blockpos = tileentity.getPos();

                    if (this.isBlockLoaded(blockpos, false) && this.worldBorder.contains(blockpos)) //Forge: Fix TE's getting an extra tick on the client side....
                    {
                        try
                        {
                            this.profiler.startSection(tileentity.getClass()); // Fix for MC-117087
                            ((ITickable)tileentity).update();
                            this.profiler.endSection();
                        }
                        catch (Throwable throwable)
                        {
                            CrashReport crashreport2 = CrashReport.makeCrashReport(throwable, "Ticking block entity");
                            CrashReportCategory crashreportcategory2 = crashreport2.makeCategory("Block entity being ticked");
                            tileentity.addInfoToCrashReport(crashreportcategory2);
                            if (net.minecraftforge.common.ForgeModContainer.removeErroringTileEntities)
                            {
                                net.minecraftforge.fml.common.FMLLog.severe(crashreport2.getCompleteReport());
                                tileentity.invalidate();
                                this.removeTileEntity(tileentity.getPos());
                            }
                            else
                            throw new ReportedException(crashreport2);
                        }
                    }
                }

                if (tileentity.isInvalid())
                {
                    iterator.remove();
                    this.loadedTileEntityList.remove(tileentity);

                    if (this.isBlockLoaded(tileentity.getPos()))
                    {
                        //Forge: Bugfix: If we set the tile entity it immediately sets it in the chunk, so we could be desyned
                        Chunk chunk = this.getChunkFromBlockCoords(tileentity.getPos());
                        if (chunk.getTileEntity(tileentity.getPos(), net.minecraft.world.chunk.Chunk.EnumCreateEntityType.CHECK) == tileentity)
                            chunk.removeTileEntity(tileentity.getPos());
                    }
                }
            }

            if (!this.tileEntitiesToBeRemoved.isEmpty())
            {
                for (Object tile : tileEntitiesToBeRemoved)
                {
                   ((TileEntity)tile).onChunkUnload();
                }

                this.tickableTileEntities.removeAll(this.tileEntitiesToBeRemoved);
                this.loadedTileEntityList.removeAll(this.tileEntitiesToBeRemoved);
                this.tileEntitiesToBeRemoved.clear();
            }

            this.processingLoadedTiles = false;  //FML Move below remove to prevent CMEs

            this.profiler.endStartSection("pendingBlockEntities");

            if (!this.addedTileEntityList.isEmpty())
            {
                for (int j1 = 0; j1 < this.addedTileEntityList.size(); ++j1)
                {
                    TileEntity tileentity1 = (TileEntity)this.addedTileEntityList.get(j1);

                    if (!tileentity1.isInvalid())
                    {
                        if (!this.loadedTileEntityList.contains(tileentity1))
                        {
                            this.addTileEntity(tileentity1);
                        }

                        if (this.isBlockLoaded(tileentity1.getPos()))
                        {
                            Chunk chunk = this.getChunkFromBlockCoords(tileentity1.getPos());
                            IBlockState iblockstate = chunk.getBlockState(tileentity1.getPos());
                            chunk.addTileEntity(tileentity1.getPos(), tileentity1);
                            this.notifyBlockUpdate(tileentity1.getPos(), iblockstate, iblockstate, 3);
                        }
                    }
                }

                this.addedTileEntityList.clear();
            }*/

            this.profiler.endSection();
            this.profiler.endSection();

            cbi.cancel();
        }
    }


    @Override public boolean isBlockColumnLoaded(BlockPos pos) {
        return isBlockColumnLoaded(pos, true);
    }

    @Override public boolean isBlockColumnLoaded(BlockPos pos, boolean allowEmpty) {
        return this.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4, allowEmpty);
    }

    //vanilla methods

    //==============================================
    @Shadow public abstract boolean isValid(BlockPos pos);

    @Intrinsic public boolean world$isValid(BlockPos pos) {
        return this.isValid(pos);
    }

    //==============================================
    @Shadow public abstract boolean isBlockLoaded(BlockPos pos);

    @Intrinsic public boolean world$isBlockLoaded(BlockPos pos) {
        return this.isBlockLoaded(pos);
    }

    //==============================================
    @Shadow public abstract boolean isBlockLoaded(BlockPos pos, boolean allowEmpty);

    @Intrinsic public boolean world$isBlockLoaded(BlockPos pos, boolean allowEmpty) {
        return this.isBlockLoaded(pos, allowEmpty);
    }

    //==============================================
    @Shadow public abstract void loadEntities(Collection<Entity> entities);

    @Intrinsic public void world$loadEntities(Collection<Entity> entities) {
        this.loadEntities(entities);
    }

    //==============================================
    @Shadow public abstract void addTileEntities(Collection<TileEntity> values);

    @Intrinsic public void world$addTileEntities(Collection<TileEntity> values) {
        this.addTileEntities(values);
    }

    //==============================================
    @Shadow public abstract void unloadEntities(Collection<Entity> entities);

    @Intrinsic public void world$unloadEntities(Collection<Entity> entities) {
        this.unloadEntities(entities);
    }

    //==============================================
    @Shadow public abstract void removeTileEntity(BlockPos pos);

    @Intrinsic public void world$removeTileEntity(BlockPos pos) {
        this.removeTileEntity(pos);
    }

    //==============================================
    @Shadow public abstract long getTotalWorldTime();

    @Intrinsic public long world$getTotalWorldTime() {
        return this.getTotalWorldTime();
    }
    //==============================================

    @Shadow public abstract void setTileEntity(BlockPos blockpos, @Nullable TileEntity tileentity);

    @Intrinsic public void world$setTileEntity(BlockPos blockpos, @Nullable TileEntity tileentity) {
        this.setTileEntity(blockpos, tileentity);
    }
    //==============================================

    @Shadow public abstract void markBlockRangeForRenderUpdate(BlockPos blockpos, BlockPos blockpos1);

    @Intrinsic public void world$markBlockRangeForRenderUpdate(BlockPos blockpos, BlockPos blockpos1) {
        this.markBlockRangeForRenderUpdate(blockpos, blockpos1);
    }
    //==============================================

    @Shadow public abstract boolean addTileEntity(TileEntity tileEntity);

    @Intrinsic public boolean world$addTileEntity(TileEntity tileEntity) {
        return this.addTileEntity(tileEntity);
    }
    //==============================================

    @Shadow
    public abstract void markBlockRangeForRenderUpdate(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ);

    @Intrinsic
    public void world$markBlockRangeForRenderUpdate(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ) {
        this.markBlockRangeForRenderUpdate(minBlockX, minBlockY, minBlockZ, maxBlockX, maxBlockY, maxBlockZ);
    }
    //==============================================

    @Shadow public abstract long getSeed();

    @Intrinsic public long world$getSeed() {
        return this.getSeed();
    }
    //==============================================

    @Shadow public abstract boolean checkLightFor(EnumSkyBlock sky, BlockPos pos);

    @Intrinsic public boolean world$checkLightFor(EnumSkyBlock type, BlockPos pos) {
        return this.checkLightFor(type, pos);
    }
    //==============================================

    @Shadow public abstract ISaveHandler getSaveHandler();

    @Intrinsic public ISaveHandler world$getSaveHandler() {
        return this.getSaveHandler();
    }
    //==============================================

    @Shadow public abstract MinecraftServer getMinecraftServer();

    @Intrinsic public MinecraftServer world$getMinecraftServer() {
        return this.getMinecraftServer();
    }
    //==============================================

    @Shadow public abstract void addBlockEvent(BlockPos blockPos, Block i, int t, int p);

    @Intrinsic public void world$addBlockEvent(BlockPos blockPos, Block i, int t, int p) {
        this.addBlockEvent(blockPos, i, t, p);
    }
    //==============================================

    @Shadow public abstract void scheduleBlockUpdate(BlockPos blockPos, Block i, int t, int p);

    @Intrinsic public void world$scheduleBlockUpdate(BlockPos blockPos, Block i, int t, int p) {
        this.scheduleBlockUpdate(blockPos, i, t, p);
    }
    //==============================================

    @Shadow public abstract GameRules getGameRules();

    @Intrinsic public GameRules world$getGameRules() {
        return this.getGameRules();
    }
    //==============================================

    @Shadow public abstract WorldInfo getWorldInfo();

    @Intrinsic public WorldInfo world$getWorldInfo() {
        return this.getWorldInfo();
    }
    //==============================================

    @Nullable @Shadow public abstract TileEntity getTileEntity(BlockPos pos);

    @Nullable @Intrinsic public TileEntity world$getTileEntity(BlockPos pos) {
        return this.getTileEntity(pos);
    }
    //==============================================

    @Shadow public abstract boolean setBlockState(BlockPos blockPos, IBlockState blockState, int i);

    @Intrinsic public boolean world$setBlockState(BlockPos blockPos, IBlockState blockState, int i) {
        return this.setBlockState(blockPos, blockState, i);
    }
    //==============================================

    @Shadow public abstract IBlockState getBlockState(BlockPos blockPos);

    @Intrinsic public IBlockState world$getBlockState(BlockPos blockPos) {
        return this.getBlockState(blockPos);
    }
    //==============================================
    
    @Shadow public abstract boolean isAirBlock(BlockPos randomPos);

    @Intrinsic public boolean world$isAirBlock(BlockPos pos) {
        return this.isAirBlock(pos);
    }
    //==============================================

    @Shadow public abstract Biome getBiome(BlockPos cubeCenter);

    @Intrinsic public Biome world$getBiome(BlockPos pos) {
        return this.getBiome(pos);
    }
    //==============================================

    @Shadow public abstract BiomeProvider getBiomeProvider();

    @Intrinsic public BiomeProvider world$getBiomeProvider() {
        return this.getBiomeProvider();
    }
    //==============================================

    @Shadow public abstract BlockPos getSpawnPoint();

    @Intrinsic public BlockPos world$getSpawnPoint() {
        return this.getSpawnPoint();
    }
    //==============================================

    @Shadow public abstract WorldBorder getWorldBorder();

    @Intrinsic public WorldBorder world$getWorldBorder() {
        return this.getWorldBorder();
    }
    //==============================================

    @Shadow(remap = false) public abstract int countEntities(EnumCreatureType type, boolean flag);

    @Intrinsic public int world$countEntities(EnumCreatureType type, boolean flag) {
        return this.countEntities(type, flag);
    }
    //==============================================

    @Shadow public abstract boolean isAnyPlayerWithinRangeAt(double f, double i3, double f1, double v);

    @Intrinsic public boolean world$isAnyPlayerWithinRangeAt(double f, double i3, double f1, double v) {
        return this.isAnyPlayerWithinRangeAt(f, i3, f1, v);
    }
    //==============================================

    @Shadow public abstract DifficultyInstance getDifficultyForLocation(BlockPos pos);

    @Intrinsic public DifficultyInstance world$getDifficultyForLocation(BlockPos pos) {
        return this.getDifficultyForLocation(pos);
    }
    //==============================================

    @Shadow public abstract boolean spawnEntity(Entity entity);

    @Intrinsic public boolean world$spawnEntity(Entity entity) {
        return this.spawnEntity(entity);
    }
    //==============================================

    @Shadow public abstract boolean isAreaLoaded(BlockPos start, BlockPos end);

    @Intrinsic public boolean world$isAreaLoaded(BlockPos start, BlockPos end) {
        return this.isAreaLoaded(start, end);
    }
    //==============================================

    @Shadow public abstract int getActualHeight();

    @Intrinsic public int world$getActualHeight() {
        return this.getActualHeight();
    }
    //==============================================

    @Shadow public abstract void notifyLightSet(BlockPos pos);

    @Intrinsic public void world$notifyLightSet(BlockPos pos) {
        this.notifyLightSet(pos);
    }
    //==============================================

    @Shadow public abstract WorldType getWorldType();

    @Intrinsic public WorldType world$getWorldType() {
        return this.getWorldType();
    }
    //==============================================

    @Shadow public abstract boolean canBlockFreezeWater(BlockPos topBlock);

    @Intrinsic public boolean world$canBlockFreezeWater(BlockPos topBlock) {
        return canBlockFreezeWater(topBlock);
    }
    //==============================================

    @Shadow public abstract boolean canSnowAt(BlockPos aboveTop, boolean flag);

    @Intrinsic public boolean world$canSnowAt(BlockPos aboveTop, boolean flag) {
        return canSnowAt(aboveTop, flag);
    }
    //==============================================
}
