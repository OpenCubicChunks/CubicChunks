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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.logging.log4j.LogManager;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CubeWorldEntitySpawner extends WorldEntitySpawner {

    private static final int CUBES_PER_CHUNK = 16;
    private static final int MOB_COUNT_DIV = (int) Math.pow(17.0D, 2.0D) * CUBES_PER_CHUNK;
    private static final int SPAWN_RADIUS = 8;
    private int currentPlayer = 0;

    @Nonnull private List<CubePos> cubesForSpawn = new ArrayList<CubePos>();

    @Override
    public int findChunksForSpawning(WorldServer worldOrig, boolean hostileEnable, boolean peacefulEnable, boolean spawnOnSetTickRate) {
        if (!hostileEnable && !peacefulEnable) {
            return 0;
        }
        ICubicWorldServer world = (ICubicWorldServer) worldOrig;
        this.cubesForSpawn.clear();
        this.addEligibleChunks(world);
        int totalSpawnCount = 0;

        next_type: 
        for (EnumCreatureType mobType : EnumCreatureType.values()) {
            if (!shouldSpawnType(mobType, hostileEnable, peacefulEnable, spawnOnSetTickRate)) {
                continue;
            }
            int worldEntityCount = 0;
            int maxEntityCount = mobType.getMaxNumberOfCreature();
            Class<? extends IAnimals> mobTypeClass = mobType.getCreatureClass();
            for (Entity entity : worldOrig.loadedEntityList) {
                if (mobTypeClass.isInstance(entity) && ++worldEntityCount > maxEntityCount)
                        continue next_type;
            }
            totalSpawnCount += spawnCreatureTypeInAllChunks(mobType, world);
        }
        return totalSpawnCount;
    }

    private void addEligibleChunks(ICubicWorldServer world) {
        Random rand = world.getRand();
        List<EntityPlayer> pList = world.getPlayerEntities();
        int pAmount = pList.size();
        if (pAmount == 0)
            return;
        if (++currentPlayer >= pAmount) {
            currentPlayer = 0;
        }
        // This function called every tick, it unnecessary to iterate thru all players.
        EntityPlayer player = pList.get(currentPlayer); 
        if (!player.isSpectator()) {
            CubePos center = CubePos.fromEntity(player);
            for (int cubeXRel = -SPAWN_RADIUS; cubeXRel <= SPAWN_RADIUS; ++cubeXRel) {
                for (int cubeYRel = -SPAWN_RADIUS; cubeYRel <= SPAWN_RADIUS; ++cubeYRel) {
                    for (int cubeZRel = -SPAWN_RADIUS; cubeZRel <= SPAWN_RADIUS; ++cubeZRel) {
                        if (cubeXRel * cubeXRel + cubeYRel * cubeYRel + cubeZRel * cubeZRel <= 8)
                            continue;
                        CubePos chunkPos = center.add(cubeXRel, cubeYRel, cubeZRel);
                        CubeWatcher chunkInfo = world.getPlayerCubeMap().getCubeWatcher(chunkPos);
                        if (chunkInfo != null && chunkInfo.isSentToPlayers()) {
                            cubesForSpawn.add(rand.nextInt(cubesForSpawn.size() + 1), chunkPos);
                        }
                    }
                }
            }
        }
    }

    private int spawnCreatureTypeInAllChunks(EnumCreatureType mobType, ICubicWorldServer world) {

        int spawnablePointsSize = cubesForSpawn.size();
        if(spawnablePointsSize == 0)
            return 0;
        BlockPos spawnPoint = world.getSpawnPoint();
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        CubePos currentChunkPos = this.cubesForSpawn.get(world.getRand().nextInt(spawnablePointsSize));
        BlockPos blockpos = getRandomChunkPosition(world, currentChunkPos);
        if (blockpos == null) {
            return 0;
        }
        IBlockState block = world.getBlockState(blockpos);

        if (block.isNormalCube()) {
            return 0;
        }
        int blockX = blockpos.getX();
        int blockY = blockpos.getY();
        int blockZ = blockpos.getZ();

        int entityBlockX = blockX;
        int entityY = blockY;
        int entityBlockZ = blockZ;
        int searchRadius = 6;
        Random rand = world.getRand();
        entityBlockX += rand.nextInt(searchRadius) - rand.nextInt(searchRadius);
        entityY += rand.nextInt(1) - rand.nextInt(1);
        entityBlockZ += rand.nextInt(searchRadius) - rand.nextInt(searchRadius);
        blockPos.setPos(entityBlockX, entityY, entityBlockZ);
        float entityX = (float) entityBlockX + 0.5F;
        float entityZ = (float) entityBlockZ + 0.5F;

        if (world.isAnyPlayerWithinRangeAt(entityX, entityY, entityZ, 24.0D) ||
                spawnPoint.distanceSq(entityX, entityY, entityZ) < 576.0D) {
            return 0;
        }
        Biome.SpawnListEntry biomeMobs = world.getSpawnListEntryForTypeAt(mobType, blockPos);
        if (biomeMobs == null)
            return 0;
 
        if (!world.canCreatureTypeSpawnHere(mobType, biomeMobs, blockPos) ||
                !canCreatureTypeSpawnAtLocation(EntitySpawnPlacementRegistry
                        .getPlacementForEntity(biomeMobs.entityClass), (World) world, blockPos)) {
            return 0;
        }
        EntityLiving toSpawn;

        try {
            toSpawn = biomeMobs.entityClass.getConstructor(new Class[] {
                    World.class
            }).newInstance(world);
        } catch (Exception exception) {
            exception.printStackTrace();
            return 0;
        }

        toSpawn.setLocationAndAngles(entityX, entityY, entityZ, rand.nextFloat() * 360.0F, 0.0F);

        Event.Result canSpawn = ForgeEventFactory.canEntitySpawn(toSpawn, (World) world, entityX, entityY, entityZ);
        if (canSpawn == Event.Result.ALLOW ||
                (canSpawn == Event.Result.DEFAULT && toSpawn.getCanSpawnHere() &&
                        toSpawn.isNotColliding())) {
            if (!ForgeEventFactory.doSpecialSpawn(toSpawn, (World) world, entityX, entityY, entityZ)) {
                toSpawn.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(toSpawn)), null);
            }

            if (toSpawn.isNotColliding()) {
                Class<? extends IAnimals> mobTypeClass = mobType.getCreatureClass();
                if (!mobTypeClass.isInstance(toSpawn))
                    throw new ClassCastException(toSpawn + " cannot be casted to " + mobTypeClass);
                world.spawnEntity(toSpawn);
                return 1;
            } else {
                toSpawn.setDead();
            }
        }
        return 0;
    }

    private static boolean shouldSpawnType(EnumCreatureType type, boolean hostile, boolean peaceful, boolean spawnOnSetTickRate) {
        return !((type.getPeacefulCreature() && !peaceful) ||
                (!type.getPeacefulCreature() && !hostile) ||
                (type.getAnimal() && !spawnOnSetTickRate));
    }

    private static BlockPos getRandomChunkPosition(ICubicWorldServer world, CubePos pos) {
        int blockX = pos.getMinBlockX() + world.getRand().nextInt(16);
        int blockZ = pos.getMinBlockZ() + world.getRand().nextInt(16);

        int height = world.getEffectiveHeight(blockX, blockZ);
        if (pos.getMinBlockY() > height) {
            return null;
        }
        int blockY = pos.getMinBlockY() + world.getRand().nextInt(16);
        return new BlockPos(blockX, blockY, blockZ);
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
