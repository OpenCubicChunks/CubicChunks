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
package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.server.CubeWatcher;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CubeWorldEntitySpawner implements IWorldEntitySpawner {

    private static final int CUBES_PER_CHUNK = 16;
    private static final int MOB_COUNT_DIV = (int) Math.pow(17.0D, 2.0D) * CUBES_PER_CHUNK;
    private static final int SPAWN_RADIUS = 8;

    @Nonnull private Set<CubePos> cubesForSpawn = new HashSet<>();

    @Override
    public int findChunksForSpawning(WorldServer world, boolean hostileEnable, boolean peacefulEnable, boolean spawnOnSetTickRate) {
        if (!hostileEnable && !peacefulEnable) {
            return 0;
        }
        this.cubesForSpawn.clear();

        int chunkCount = addEligibleChunks(world, this.cubesForSpawn);
        int totalSpawnCount = 0;

        for (EnumCreatureType mobType : EnumCreatureType.values()) {
            if (!shouldSpawnType(mobType, hostileEnable, peacefulEnable, spawnOnSetTickRate)) {
                continue;
            }
            int worldEntityCount = world.countEntities(mobType, true);
            int maxEntityCount = mobType.getMaxNumberOfCreature() * chunkCount / MOB_COUNT_DIV;

            if (worldEntityCount > maxEntityCount) {
                continue;
            }
            ArrayList<CubePos> shuffled = getShuffledCopy(this.cubesForSpawn);
            totalSpawnCount += spawnCreatureTypeInAllChunks(mobType, world, shuffled);
        }
        return totalSpawnCount;
    }

    private int addEligibleChunks(WorldServer world, Set<CubePos> possibleChunks) {
        int chunkCount = 0;
        Random r = world.rand;
        Set<CubePos> allCubes = new HashSet<>();
        for (EntityPlayer player : world.playerEntities) {
            if (player.isSpectator()) {
                continue;
            }
            CubePos center = CubePos.fromEntity(player);

            for (int cubeXRel = -SPAWN_RADIUS; cubeXRel <= SPAWN_RADIUS; ++cubeXRel) {
                for (int cubeYRel = -SPAWN_RADIUS; cubeYRel <= SPAWN_RADIUS; ++cubeYRel) {
                    for (int cubeZRel = -SPAWN_RADIUS; cubeZRel <= SPAWN_RADIUS; ++cubeZRel) {
                        CubePos chunkPos = center.add(cubeXRel, cubeYRel, cubeZRel);

                        if (allCubes.contains(chunkPos)) {
                            continue;
                        }
                        assert !possibleChunks.contains(chunkPos);
                        ++chunkCount;

                        boolean isEdge = cubeXRel == -SPAWN_RADIUS || cubeXRel == SPAWN_RADIUS ||
                                cubeYRel == -SPAWN_RADIUS || cubeYRel == SPAWN_RADIUS ||
                                cubeZRel == -SPAWN_RADIUS || cubeZRel == SPAWN_RADIUS;

                        if (isEdge || !world.getWorldBorder().contains(chunkPos.chunkPos())) {
                            continue;
                        }
                        CubeWatcher chunkInfo = ((PlayerCubeMap) world.getPlayerChunkMap()).getCubeWatcher(chunkPos);

                        if (chunkInfo != null && chunkInfo.isSentToPlayers()) {
                            allCubes.add(chunkPos);
                            if (r.nextInt(SPAWN_RADIUS * 2 + 1) == 0) {
                                possibleChunks.add(chunkPos);
                            }
                        }
                    }
                }
            }
        }
        return chunkCount;
    }

    private int spawnCreatureTypeInAllChunks(EnumCreatureType mobType, WorldServer world, ArrayList<CubePos> chunkList) {
        BlockPos spawnPoint = world.getSpawnPoint();
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        int totalSpawned = 0;

        nextChunk:
        for (CubePos currentChunkPos : chunkList) {
            BlockPos blockpos = getRandomChunkPosition(world, currentChunkPos);
            if (blockpos == null) {
                continue;
            }
            IBlockState block = world.getBlockState(blockpos);

            if (block.isNormalCube()) {
                continue;
            }
            int blockX = blockpos.getX();
            int blockY = blockpos.getY();
            int blockZ = blockpos.getZ();

            int currentPackSize = 0;

            for (int k2 = 0; k2 < 3; ++k2) {
                int entityBlockX = blockX;
                int entityY = blockY;
                int entityBlockZ = blockZ;
                int searchRadius = 6;
                Biome.SpawnListEntry biomeMobs = null;
                IEntityLivingData entityData = null;
                int numSpawnAttempts = MathHelper.ceil(Math.random() * 4.0D);

                Random rand = world.rand;
                for (int spawnAttempt = 0; spawnAttempt < numSpawnAttempts; ++spawnAttempt) {
                    entityBlockX += rand.nextInt(searchRadius) - rand.nextInt(searchRadius);
                    entityY += rand.nextInt(1) - rand.nextInt(1);
                    entityBlockZ += rand.nextInt(searchRadius) - rand.nextInt(searchRadius);
                    blockPos.setPos(entityBlockX, entityY, entityBlockZ);
                    float entityX = (float) entityBlockX + 0.5F;
                    float entityZ = (float) entityBlockZ + 0.5F;

                    if (world.isAnyPlayerWithinRangeAt(entityX, entityY, entityZ, 24.0D) ||
                            spawnPoint.distanceSq(entityX, entityY, entityZ) < 576.0D) {
                        continue;
                    }
                    if (biomeMobs == null) {
                        biomeMobs = world.getSpawnListEntryForTypeAt(mobType, blockPos);

                        if (biomeMobs == null) {
                            break;
                        }
                    }

                    if (!world.canCreatureTypeSpawnHere(mobType, biomeMobs, blockPos) ||
                            !WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntitySpawnPlacementRegistry
                                    .getPlacementForEntity(biomeMobs.entityClass), world, blockPos)) {
                        continue;
                    }
                    EntityLiving toSpawn;

                    try {
                        toSpawn = biomeMobs.entityClass.getConstructor(new Class[]{
                                World.class
                        }).newInstance(world);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                        //TODO: throw when entity creation fails
                        return totalSpawned;
                    }

                    toSpawn.setLocationAndAngles(entityX, entityY, entityZ, rand.nextFloat() * 360.0F, 0.0F);

                    Event.Result canSpawn = ForgeEventFactory.canEntitySpawn(toSpawn, world, entityX, entityY, entityZ, null);
                    if (canSpawn == Event.Result.ALLOW ||
                            (canSpawn == Event.Result.DEFAULT && toSpawn.getCanSpawnHere() &&
                                    toSpawn.isNotColliding())) {
                        if (!ForgeEventFactory.doSpecialSpawn(toSpawn, world, entityX, entityY, entityZ, null)) {
                            entityData = toSpawn.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(toSpawn)), entityData);
                        }

                        if (toSpawn.isNotColliding()) {
                            ++currentPackSize;
                            world.spawnEntity(toSpawn);
                        } else {
                            toSpawn.setDead();
                        }

                        if (blockZ >= ForgeEventFactory.getMaxSpawnPackSize(toSpawn)) {
                            continue nextChunk;
                        }
                    }

                    totalSpawned += currentPackSize;
                }
            }
        }
        return totalSpawned;
    }

    private static <T> ArrayList<T> getShuffledCopy(Collection<T> collection) {
        ArrayList<T> list = new ArrayList<>(collection);
        Collections.shuffle(list);
        return list;
    }

    private static boolean shouldSpawnType(EnumCreatureType type, boolean hostile, boolean peaceful, boolean spawnOnSetTickRate) {
        return !((type.getPeacefulCreature() && !peaceful) ||
                (!type.getPeacefulCreature() && !hostile) ||
                (type.getAnimal() && !spawnOnSetTickRate));
    }

    @Nullable private static BlockPos getRandomChunkPosition(WorldServer world, CubePos pos) {
        int blockX = pos.getMinBlockX() + world.rand.nextInt(Cube.SIZE);
        int blockZ = pos.getMinBlockZ() + world.rand.nextInt(Cube.SIZE);

        int height = world.getHeight(blockX, blockZ);
        if (pos.getMinBlockY() > height) {
            return null;
        }
        int blockY = pos.getMinBlockY() + world.rand.nextInt(Cube.SIZE);
        return new BlockPos(blockX, blockY, blockZ);
    }
}
