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
package io.github.opencubicchunks.cubicchunks.api.worldgen.populator;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.List;
import java.util.Random;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WorldGenEntitySpawner {

    public static void initialWorldGenSpawn(WorldServer world, Biome biome, int blockX, int blockY, int blockZ,
            int sizeX, int sizeY, int sizeZ, Random random) {
        List<Biome.SpawnListEntry> spawnList = biome.getSpawnableList(EnumCreatureType.CREATURE);

        if (spawnList.isEmpty()) {
            return;
        }
        while (random.nextFloat() < biome.getSpawningChance()) {
            Biome.SpawnListEntry currEntry = WeightedRandom.getRandomItem(world.rand, spawnList);
            int groupCount = MathHelper.getInt(random, currEntry.minGroupCount, currEntry.maxGroupCount);
            IEntityLivingData data = null;
            int randX = blockX + random.nextInt(sizeX);
            int randZ = blockZ + random.nextInt(sizeZ);

            final int initRandX = randX;
            final int initRandZ = randZ;

            for (int i = 0; i < groupCount; ++i) {
                for (int j = 0; j < 4; ++j) {
                    randX += random.nextInt(5) - random.nextInt(5);
                    randZ += random.nextInt(5) - random.nextInt(5);
                    while (randX < blockX || randX >= blockX + sizeX || randZ < blockZ || randZ >= blockZ + sizeZ) {
                        randX = initRandX + random.nextInt(5) - random.nextInt(5);
                        randZ = initRandZ + random.nextInt(5) - random.nextInt(5);
                    }
                    BlockPos pos = ((ICubicWorld)world).findTopBlock(new BlockPos(randX, blockY + sizeY + ICube.SIZE / 2, randZ),
                            blockY, blockY + sizeY - 1, ICubicWorld.SurfaceType.SOLID);
                    if (pos == null) {
                        continue;
                    }

                    if (WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType.ON_GROUND, (World) world, pos)) {
                        EntityLiving spawnedEntity;

                        try {
                            spawnedEntity = currEntry.newInstance((World) world);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                            continue;
                        }

                        spawnedEntity.setLocationAndAngles(randX + 0.5, pos.getY(), randZ + 0.5, random.nextFloat() * 360.0F, 0.0F);
                        Event.Result forgeCanSpawn = ForgeEventFactory.canEntitySpawn(spawnedEntity, world, randX + 0.5f, (float) pos.getY(), randZ + 0.5f, null);
                        if (forgeCanSpawn == Event.Result.DENY) {
                            spawnedEntity.setDead();
                            continue;
                        }
                        world.spawnEntity(spawnedEntity);
                        data = spawnedEntity.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(spawnedEntity)), data);
                        break;
                    }
                }
            }
        }
    }
}
