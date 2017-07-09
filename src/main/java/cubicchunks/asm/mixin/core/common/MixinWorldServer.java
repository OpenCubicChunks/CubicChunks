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

import cubicchunks.CubicChunks;
import cubicchunks.entity.CubicEntityTracker;
import cubicchunks.lighting.FirstLightProcessor;
import cubicchunks.server.ChunkGc;
import cubicchunks.server.CubeProviderServer;
import cubicchunks.server.PlayerCubeMap;
import cubicchunks.util.CCEntitySelectors;
import cubicchunks.util.CubePos;
import cubicchunks.world.CubeWorldEntitySpawner;
import cubicchunks.world.CubicSaveHandler;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.NotCubicChunksWorldException;
import cubicchunks.world.column.IColumn;
import cubicchunks.world.cube.Cube;
import cubicchunks.world.provider.ICubicWorldProvider;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntitySkeletonHorse;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

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

import com.google.common.base.Predicate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Implementation of {@link ICubicWorldServer} interface.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(WorldServer.class)
@Implements(@Interface(iface = ICubicWorldServer.class, prefix = "world$"))
public abstract class MixinWorldServer extends MixinWorld implements ICubicWorldServer {

    @Shadow @Mutable @Final private PlayerChunkMap playerChunkMap;
    @Shadow @Mutable @Final private WorldEntitySpawner entitySpawner;
    @Shadow @Mutable @Final private EntityTracker theEntityTracker;
    @Shadow public boolean disableLevelSaving;

    @Nullable private ChunkGc chunkGc;
    @Nullable private FirstLightProcessor firstLightProcessor;
    
    @Override public void initCubicWorld(int minHeight, int maxHeight) {
        super.initCubicWorld(minHeight, maxHeight);
        this.isCubicWorld = true;
        this.entitySpawner = new CubeWorldEntitySpawner();

        this.chunkProvider = new CubeProviderServer(this,
                ((ICubicWorldProvider) this.provider).createCubeGenerator());

        this.playerChunkMap = new PlayerCubeMap(this);
        this.chunkGc = new ChunkGc(getCubeCache());

        this.saveHandler = new CubicSaveHandler(this, this.getSaveHandler());

        this.firstLightProcessor = new FirstLightProcessor(this);
        this.theEntityTracker = new CubicEntityTracker(this);
    }

    @Override public void tickCubicWorld() {
        if (!this.isCubicWorld()) {
            throw new NotCubicChunksWorldException();
        }
        assert chunkGc != null;
        this.chunkGc.tick();
    }

    @Override public CubicEntityTracker getCubicEntityTracker() {
        if (!this.isCubicWorld()) {
            throw new NotCubicChunksWorldException();
        }
        assert this.theEntityTracker instanceof CubicEntityTracker;
        return (CubicEntityTracker) this.theEntityTracker;
    }

    @Override public CubeProviderServer getCubeCache() {
        if (!this.isCubicWorld()) {
            throw new NotCubicChunksWorldException();
        }
        return (CubeProviderServer) this.chunkProvider;
    }

    @Override public FirstLightProcessor getFirstLightProcessor() {
        if (!this.isCubicWorld()) {
            throw new NotCubicChunksWorldException();
        }
        assert this.firstLightProcessor != null;
        return this.firstLightProcessor;
    }
    //vanilla field accessors

    @Override public boolean getDisableLevelSaving() {
        return this.disableLevelSaving;
    }

    @Override public PlayerCubeMap getPlayerCubeMap() {
        if (!this.isCubicWorld()) {
            throw new NotCubicChunksWorldException();
        }
        return (PlayerCubeMap) this.playerChunkMap;
    }
    
    @Inject(method = "scheduleUpdate", at = @At("HEAD"), cancellable = true, require = 1)
    public void scheduleUpdateInject(BlockPos pos, Block blockIn, int delay, CallbackInfo ci) {
        if (this.isCubicWorld()) {
            Cube cube = this.getCubeCache().getLoadedCube(CubePos.fromBlockCoords(pos));
            if (cube != null) {
                cube.scheduleUpdate(pos, blockIn, delay, 0);
            }
            ci.cancel();
        }
    }
    
    @Inject(method = "scheduleBlockUpdate", at = @At("HEAD"), cancellable = true, require = 1)
    public void scheduleBlockUpdateInject(BlockPos pos, Block blockIn, int delay, int priority, CallbackInfo ci) {
        if (this.isCubicWorld()) {
            Cube cube = this.getCubeCache().getLoadedCube(CubePos.fromBlockCoords(pos));
            if (cube != null) {
                cube.scheduleUpdate(pos, blockIn, delay, priority);
            }
            ci.cancel();
        }
    }

    @Inject(method = "updateBlockTick", at = @At("HEAD"), cancellable = true, require = 1)
    public void updateBlockTickInject(BlockPos pos, Block blockIn, int delay, int priority, CallbackInfo ci) {
        if (this.isCubicWorld()) {
            Cube cube = this.getCubeCache().getLoadedCube(CubePos.fromBlockCoords(pos));
            if (cube != null) {
                cube.scheduleUpdate(pos, blockIn, delay, priority);
            }
            ci.cancel();
        }
    }
    
    @Inject(method = "updateBlocks", at = @At("HEAD"), cancellable = true)
    public void onUpdateBlocks(CallbackInfo ci) {
        if (!this.isCubicWorld())
            return;
        WorldServer thisWorld = (WorldServer) (Object) this;
        boolean isRaining = thisWorld.isRaining();
        boolean isThundering = thisWorld.isThundering();
        this.profiler.startSection("pollingChunks");
        CubeProviderServer cubeProvider = (CubeProviderServer)this.chunkProvider;
        Iterator<IColumn> iterator = cubeProvider.columnsIterator();
        while(iterator.hasNext())
        {
            this.profiler.startSection("getChunk");
            IColumn column = iterator.next();
            Chunk chunk = (Chunk) column;
//          this.profiler.endStartSection("checkNextLight");
//          chunk.enqueueRelightChecks();
//          this.profiler.endStartSection("tickChunk");
//          chunk.onTick(false);
            this.profiler.endStartSection("thunder");

            if (this.provider.canDoLightning(chunk) && isRaining && isThundering && this.rand.nextInt(100000) == 0) {
                int l = this.updateLCG() >> 2;
                int targetX = l & 15;
                int targetZ = l >> 8 & 15;
                int blockPosY = column.getHeightValue(targetX, targetZ);
                Cube cube = column.getLoadedCube(blockPosY >> 4);
                if (cube != null) {
                    CubePos cubeCoords = cube.getCoords();
                    AxisAlignedBB axisalignedbb = new AxisAlignedBB(cubeCoords.getMinBlockPos(), cubeCoords.getMaxBlockPos());
                    List<EntityLivingBase> thunderTargets = new ArrayList<EntityLivingBase>();
                    cube.getEntityContainer().getEntitiesOfTypeWithinAAAB(EntityLivingBase.class, axisalignedbb, thunderTargets,
                            CCEntitySelectors.LIVING_CAN_SEE_SKY);
                    BlockPos blockpos = null;
                    if (!thunderTargets.isEmpty()) {
                        blockpos = ((EntityLivingBase) thunderTargets.get(this.rand.nextInt(thunderTargets.size()))).getPosition();
                    } else {
                        blockpos = new BlockPos(column.getX() * 16 + targetX, blockPosY, column.getZ() * 16 + targetZ);
                    }

                    if (column.canSeeSky(blockpos) && this.getBiome(blockpos).canRain()) {
                        DifficultyInstance difficultyinstance = this.getDifficultyForLocation(blockpos);

                        if (this.getGameRules().getBoolean("doMobSpawning")
                                && this.rand.nextDouble() < (double) difficultyinstance.getAdditionalDifficulty() * 0.01D) {
                            EntitySkeletonHorse entityskeletonhorse = new EntitySkeletonHorse(thisWorld);
                            entityskeletonhorse.setTrap(true);
                            entityskeletonhorse.setGrowingAge(0);
                            entityskeletonhorse.setPosition((double) blockpos.getX(), (double) blockpos.getY(), (double) blockpos.getZ());
                            this.spawnEntity(entityskeletonhorse);
                            thisWorld.addWeatherEffect(new EntityLightningBolt(thisWorld, (double) blockpos.getX(), (double) blockpos.getY(),
                                    (double) blockpos.getZ(), true));
                        } else {
                            thisWorld.addWeatherEffect(new EntityLightningBolt(thisWorld, (double) blockpos.getX(), (double) blockpos.getY(),
                                    (double) blockpos.getZ(), false));
                        }
                    }
                }
            }

            this.profiler.endStartSection("iceandsnow");

            if (this.provider.canDoRainSnowIce(chunk) && this.rand.nextInt(16) == 0) {
                int l = this.updateLCG() >> 2;
                int targetX = l & 15;
                int targetZ = l >> 8 & 15;
                int blockPosY = column.getHeightValue(targetX, targetZ);
                Cube cube = column.getLoadedCube(blockPosY >> 4);
                if (cube != null) {
                    BlockPos blockpos1 = new BlockPos(column.getX() * 16 + targetX, blockPosY, column.getZ() * 16 + targetZ);
                    BlockPos blockpos2 = blockpos1.down();

                    this.profiler.startSection("ice");
                    if (thisWorld.canBlockFreezeNoWater(blockpos2)) {
                        column.setBlockState(blockpos2, Blocks.ICE.getDefaultState());
                    }

                    this.profiler.endStartSection("snow");
                    if (isRaining && this.canSnowAt(blockpos1, true)) {
                        column.setBlockState(blockpos1, Blocks.SNOW_LAYER.getDefaultState());
                    }

                    this.profiler.endStartSection("rain");
                    if (isRaining && this.getBiome(blockpos2).canRain()) {
                        column.getBlockState(blockpos2).getBlock().fillWithRain(thisWorld, blockpos2);
                    }
                    this.profiler.endSection();
                }
            }
            // Random block tick handled by Cube
            this.profiler.endSection();
        }
        this.profiler.endSection();
        ci.cancel();
    }

    //vanilla methods
    //==============================================
    @Nullable @Shadow
    public abstract Biome.SpawnListEntry getSpawnListEntryForTypeAt(EnumCreatureType type, BlockPos pos);

    @Nullable @Intrinsic
    public Biome.SpawnListEntry world$getSpawnListEntryForTypeAt(EnumCreatureType type, BlockPos pos) {
        return this.getSpawnListEntryForTypeAt(type, pos);
    }

    //==============================================
    @Shadow
    public abstract boolean canCreatureTypeSpawnHere(EnumCreatureType type, Biome.SpawnListEntry entry, BlockPos pos);

    @Intrinsic
    public boolean world$canCreatureTypeSpawnHere(EnumCreatureType type, Biome.SpawnListEntry entry, BlockPos pos) {
        return this.canCreatureTypeSpawnHere(type, entry, pos);
    }
    //==============================================
}
