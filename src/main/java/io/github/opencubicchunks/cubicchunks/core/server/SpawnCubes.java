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
package io.github.opencubicchunks.cubicchunks.core.server;

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.ITicket;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SpawnCubes {

    private static final int DEFAULT_SPAWN_RADIUS = 12;//12 - 1 + 12; // highest render distance is 32
    private static final int DEFAULT_VERTICAL_SPAWN_RADIUS = 8;

    public static void update(World world) {
        if (world.provider.canRespawnHere()) {
            SpawnArea.get(world).update(world);
        }
    }

    public static class SpawnArea extends WorldSavedData implements ITicket {

        private static final String STORAGE = CubicChunks.MODID + "_spawncubes";

        @Nullable private BlockPos spawnPoint = null;
        private int radiusXZ = DEFAULT_SPAWN_RADIUS;
        private int radiusY = DEFAULT_VERTICAL_SPAWN_RADIUS;

        public SpawnArea() {
            this(STORAGE);
        }

        public SpawnArea(String storage) {
            super(storage);
        }

        public void update(World world) {
            update(world, radiusXZ, radiusY); // radius did not change
        }

        public void update(World world, int newRadiusXZ, int newRadiusY) {
            if (!world.getSpawnPoint().equals(spawnPoint) || radiusXZ != newRadiusXZ || radiusY != newRadiusY) { // check if something changed
                removeTickets(world);
                radiusXZ = newRadiusXZ;
                radiusY = newRadiusY;
                addTickets(world); // addTickets will update the spawn location if need be
                markDirty();
            }
        }

        private void removeTickets(World world) {
            if (radiusXZ < 0 || radiusY < 0 || spawnPoint == null) {
                return; // no spawn chunks OR nothing to remove
            }

            ICubeProviderInternal serverCubeCache = (ICubeProviderInternal) world.getChunkProvider();

            int spawnCubeX = Coords.blockToCube(spawnPoint.getX());
            int spawnCubeY = 8;//Coords.blockToCube(spawnPoint.getY()); // TODO: auto-find better value. This matches height range in vanilla
            int spawnCubeZ = Coords.blockToCube(spawnPoint.getZ());

            for (int cubeX = spawnCubeX - radiusXZ; cubeX <= spawnCubeX + radiusXZ; cubeX++) {
                for (int cubeZ = spawnCubeZ - radiusXZ; cubeZ <= spawnCubeZ + radiusXZ; cubeZ++) {
                    for (int cubeY = spawnCubeY + radiusY; cubeY >= spawnCubeY - radiusY; cubeY--) {
                        serverCubeCache.getCube(cubeX, cubeY, cubeZ).getTickets().remove(this);
                    }
                }
            }
        }

        private void addTickets(World world) {
            if (radiusXZ < 0 || radiusY < 0) {
                return; // no spawn cubes
            }

            CubeProviderServer serverCubeCache = (CubeProviderServer) world.getChunkProvider();

            // load the cubes around the spawn point
            CubicChunks.LOGGER.info("Loading cubes for spawn...");
            spawnPoint = world.getSpawnPoint();
            int spawnCubeX = Coords.blockToCube(spawnPoint.getX());
            int spawnCubeY = 8;//Coords.blockToCube(spawnPoint.getY());
            int spawnCubeZ = Coords.blockToCube(spawnPoint.getZ());

            long lastTime = System.currentTimeMillis();
            final int progressReportInterval = 1000;//ms
            int totalToGenerate = (radiusXZ * 2 + 1) * (radiusXZ * 2 + 1) * (radiusY * 2 + 1);
            int generated = 0;

            for (int cubeX = spawnCubeX - radiusXZ; cubeX <= spawnCubeX + radiusXZ; cubeX++) {
                for (int cubeZ = spawnCubeZ - radiusXZ; cubeZ <= spawnCubeZ + radiusXZ; cubeZ++) {
                    for (int cubeY = spawnCubeY + radiusY; cubeY >= spawnCubeY - radiusY; cubeY--) {
                        ICubeProviderServer.Requirement req;
                        // is edge?
                        if (cubeX == spawnCubeX - radiusXZ || cubeX == spawnCubeX + radiusXZ ||
                                cubeZ == spawnCubeZ - radiusXZ || cubeZ == spawnCubeZ + radiusXZ ||
                                cubeY == spawnCubeY - radiusY || cubeY == spawnCubeY + radiusY) {
                            req = ICubeProviderServer.Requirement.GENERATE;
                        } else {
                            req = ICubeProviderServer.Requirement.LIGHT;
                        }

                        serverCubeCache.getCube(cubeX, cubeY, cubeZ, req).getTickets().add(this);
                        generated++;
                        if (System.currentTimeMillis() >= lastTime + progressReportInterval) {
                            lastTime = System.currentTimeMillis();
                            CubicChunks.LOGGER.info("Preparing spawn area: {}%", generated * 100 / totalToGenerate);
                        }
                    }
                }
            }
        }

        public boolean shouldTick() {
            return false;
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            this.radiusXZ = nbt.getInteger("spawnRadius");
            this.radiusY = nbt.hasKey("spawnRadiusY") ?
                    nbt.getInteger("spawnRadiusY") : DEFAULT_VERTICAL_SPAWN_RADIUS;
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
            nbt.setInteger("spawnRadius", this.radiusXZ);
            nbt.setInteger("spawnRadiusY", this.radiusY);
            return nbt;
        }

        public static SpawnArea get(World world) {
            MapStorage storage = world.getPerWorldStorage();
            SpawnArea area = (SpawnArea) storage.getOrLoadData(SpawnArea.class, STORAGE);

            if (area == null) {
                area = new SpawnArea();
                storage.setData(STORAGE, area);
            }
            return area;
        }
    }
}
