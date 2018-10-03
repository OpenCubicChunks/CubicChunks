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
package cubicchunks.world;

import cubicchunks.api.worldgen.biome.CubicBiome;
import cubicchunks.server.CubeWatcher;
import cubicchunks.util.Coords;
import cubicchunks.util.CubePos;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.custom.populator.PopulatorUtils;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FastCubeWorldEntitySpawner extends WorldEntitySpawner {

    @Override
    public int findChunksForSpawning(WorldServer worldOrig, boolean hostileEnable, boolean peacefulEnable, boolean spawnOnSetTickRate) {
        if (!hostileEnable && !peacefulEnable)
            return 0;
        ICubicWorldServer world = (ICubicWorldServer) worldOrig;
        int spawned = 0;
        next_mob_type: for (EnumCreatureType mobType : EnumCreatureType.values()) {
            if (!shouldSpawnType(mobType, hostileEnable, peacefulEnable, spawnOnSetTickRate)) {
                continue;
            }
            int worldEntityCount = 0;
            int maxEntityCount = mobType.getMaxNumberOfCreature() * world.getPlayerEntities().size();
            Class<? extends IAnimals> mobTypeClass = mobType.getCreatureClass();
            for (Entity entity : worldOrig.loadedEntityList) {
                // Here we check if there is already loaded entity of a same
                // type.
                // 'world.countEntities' do a same thing, by contain a lot
                // methods in-between and also check if entity is instance of
                // EntityLiving.
                if (mobTypeClass.isInstance(entity) && ++worldEntityCount > maxEntityCount) {
                    continue next_mob_type;
                }
            }
            spawned += spawnCreatureTypeInAllChunks(mobType, world);
        }
        return spawned;
    }

    private int spawnCreatureTypeInAllChunks(EnumCreatureType mobType, ICubicWorldServer world) {
        Random rand = world.getRand();
        BlockPos spawnPoint = world.getSpawnPoint();
        int spawned = 0;
        Iterator<CubeWatcher> cwi = world.getPlayerCubeMap().getRandomWrappedCubeWatcherIterator(rand.nextInt());
        while (cwi.hasNext()) {
            CubeWatcher chunkInfo = cwi.next();
            if (chunkInfo.isSentToPlayers()) {
                int minBlockX = Coords.cubeToMinBlock(chunkInfo.getX());
                int minBlockY = Coords.cubeToMinBlock(chunkInfo.getY());
                int minBlockZ = Coords.cubeToMinBlock(chunkInfo.getZ());
                BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
                Biome.SpawnListEntry biomeMobs = null;
                IEntityLivingData entityData = null;
                int maxSpawnAttempts = 1;
                int maxPackSize = 0;
                // If we found nice spawnpoint in this attempt we will raise
                // 'maxSpawnAttempts' and 'maxPackSize' values.
                for (int spawnAttempt = 0; spawnAttempt < maxSpawnAttempts && spawned <= maxPackSize; spawnAttempt++) {
                    int blockX = minBlockX + rand.nextInt(Cube.SIZE);
                    int blockY = minBlockY + rand.nextInt(Cube.SIZE);
                    int blockZ = minBlockZ + rand.nextInt(Cube.SIZE);
                    int height = world.getEffectiveHeight(blockX, blockZ) + 1;
                    if (minBlockY > height) {
                        blockY = height;
                        int newCubeY = Coords.blockToCube(blockY);
                        if (newCubeY != chunkInfo.getY()) {
                            CubeWatcher cw = world.getPlayerCubeMap().getCubeWatcher(new CubePos(chunkInfo.getX(), newCubeY, chunkInfo.getZ()));
                            if (cw == null || !cw.isSentToPlayers())
                                continue;
                        }
                    }
                    if (world.isAnyPlayerWithinRangeAt(blockX, blockY, blockZ, 24.0D) ||
                            spawnPoint.distanceSq(blockX, blockY, blockZ) < 576.0D) {
                        continue;
                    }
                    blockPos.setPos(blockX, blockY, blockZ);

                    IBlockState block = world.getBlockState(blockPos);

                    if (block.isNormalCube()) {
                        continue;
                    }
                    float entityX = (float) blockX + 0.5F;
                    float entityY = (float) blockY;
                    float entityZ = (float) blockZ + 0.5F;

                    if(spawnAttempt == 0)
                        biomeMobs = world.getSpawnListEntryForTypeAt(mobType, blockPos);
                    
                    if (biomeMobs == null) {
                        continue;
                    }
                    if (!world.canCreatureTypeSpawnHere(mobType, biomeMobs, blockPos) ||
                            !canCreatureTypeSpawnAtLocation(EntitySpawnPlacementRegistry
                                    .getPlacementForEntity(biomeMobs.entityClass), (World) world, blockPos)) {
                        continue;
                    }
                    EntityLiving toSpawn;

                    try {
                        toSpawn = biomeMobs.entityClass.getConstructor(new Class[] {
                                World.class
                        }).newInstance(world);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                        continue;
                    }

                    toSpawn.setLocationAndAngles(entityX, entityY, entityZ, rand.nextFloat() * 360.0F, 0.0F);

                    Event.Result canSpawn = ForgeEventFactory.canEntitySpawn(toSpawn, (World) world, entityX, entityY, entityZ);
                    if (canSpawn == Event.Result.ALLOW ||
                            (canSpawn == Event.Result.DEFAULT && toSpawn.getCanSpawnHere() &&
                                    toSpawn.isNotColliding())) {
                        if (!ForgeEventFactory.doSpecialSpawn(toSpawn, (World) world, entityX, entityY, entityZ)) {
                            entityData = toSpawn.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(toSpawn)), entityData);
                        }

                        if (toSpawn.isNotColliding()) {
                            world.spawnEntity(toSpawn);
                            if (maxSpawnAttempts == 1) {
                                maxPackSize = ForgeEventFactory.getMaxSpawnPackSize(toSpawn);
                                maxSpawnAttempts = maxPackSize + 6;
                            }
                            spawned++;
                        } else {
                            toSpawn.setDead();
                        }
                    }
                }
                // If pack is spawned this tick, proceed to next entity type.
                if (spawned > 0)
                    return spawned;
            }
        }
        return spawned;
    }

    private static boolean shouldSpawnType(EnumCreatureType type, boolean hostile, boolean peaceful, boolean spawnOnSetTickRate) {
        return !((type.getPeacefulCreature() && !peaceful) ||
                (!type.getPeacefulCreature() && !hostile) ||
                (type.getAnimal() && !spawnOnSetTickRate));
    }

    public static void initialWorldGenSpawn(ICubicWorld world, CubicBiome biome, int blockX, int blockY, int blockZ,
            int sizeX, int sizeY, int sizeZ, Random random) {
        List<Biome.SpawnListEntry> spawnList = biome.getBiome().getSpawnableList(EnumCreatureType.CREATURE);

        if (spawnList.isEmpty()) {
            return;
        }
        while (random.nextFloat() < biome.getBiome().getSpawningChance()) {
            Biome.SpawnListEntry currEntry = WeightedRandom.getRandomItem(world.getRand(), spawnList);
            int groupCount = MathHelper.getInt(random, currEntry.minGroupCount, currEntry.maxGroupCount);
            IEntityLivingData data = null;
            int randX = blockX + random.nextInt(sizeX);
            int randZ = blockZ + random.nextInt(sizeZ);

            final int initRandX = randX;
            final int initRandZ = randZ;

            for (int i = 0; i < groupCount; ++i) {
                for (int j = 0; j < 4; ++j) {
                    do {
                        randX = initRandX + random.nextInt(5) - random.nextInt(5);
                        randZ = initRandZ + random.nextInt(5) - random.nextInt(5);
                    } while (randX < blockX || randX >= blockX + sizeX || randZ < blockZ || randZ >= blockZ + sizeZ);

                    BlockPos pos = PopulatorUtils.findTopBlock(
                            world, new BlockPos(randX, blockY + sizeY + Cube.SIZE / 2, randZ),
                            blockY, blockY + sizeY - 1, PopulatorUtils.SurfaceType.SOLID);
                    if (pos == null) {
                        continue;
                    }

                    if (canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType.ON_GROUND, (World) world, pos)) {
                        EntityLiving spawnedEntity;

                        try {
                            spawnedEntity = currEntry.newInstance((World) world);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                            continue;
                        }

                        spawnedEntity.setLocationAndAngles(randX + 0.5, pos.getY(), randZ + 0.5, random.nextFloat() * 360.0F, 0.0F);
                        world.spawnEntity(spawnedEntity);
                        data = spawnedEntity.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(spawnedEntity)), data);
                        break;
                    }
                }
            }
        }
    }
}
