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
package cubicchunks.server;

import cubicchunks.CubicChunks;
import cubicchunks.util.Coords;
import cubicchunks.util.ticket.ITicket;
import cubicchunks.world.ICubeProvider;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.IProviderExtras;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SpawnCubes {

    private static final int DEFAULT_SPAWN_RADIUS = 12; // highest render distance is 32

    public static void update(ICubicWorld world) {
        if (world.getProvider().canRespawnHere()) {
            SpawnArea.get(world).update(world);
        }
    }

    public static class SpawnArea extends WorldSavedData implements ITicket {

        private static final String STORAGE = CubicChunks.MODID + "_spawncubes";

        @Nullable private BlockPos spawnPoint = null;
        private int radius = DEFAULT_SPAWN_RADIUS;

        public SpawnArea() {
            this(STORAGE);
        }

        public SpawnArea(String storage) {
            super(storage);
        }

        public void update(ICubicWorld world) {
            update(world, radius); // radius did not change
        }

        public void update(ICubicWorld world, int newRadius) {
            if (!world.getSpawnPoint().equals(spawnPoint) || radius != newRadius) { // check if something changed
                removeTickets(world);
                radius = newRadius;
                addTickets(world); // addTickets will update the spawn location if need be
                markDirty();
            }
        }

        private void removeTickets(ICubicWorld world) {
            if (radius < 0 || spawnPoint == null) {
                return; // no spawn chunks OR nothing to remove
            }

            ICubeProvider serverCubeCache = world.getCubeCache();

            int spawnCubeX = Coords.blockToCube(spawnPoint.getX());
            int spawnCubeY = Coords.blockToCube(spawnPoint.getY());
            int spawnCubeZ = Coords.blockToCube(spawnPoint.getZ());

            for (int cubeX = spawnCubeX - radius; cubeX <= spawnCubeX + radius; cubeX++) {
                for (int cubeZ = spawnCubeZ - radius; cubeZ <= spawnCubeZ + radius; cubeZ++) {
                    for (int cubeY = spawnCubeY + radius; cubeY >= spawnCubeY - radius; cubeY--) {
                        serverCubeCache.getCube(cubeX, cubeY, cubeZ).getTickets().remove(this);
                    }
                }
            }
        }

        private void addTickets(ICubicWorld world) {
            if (radius < 0) {
                return; // no spawn cubes
            }

            CubeProviderServer serverCubeCache = (CubeProviderServer) world.getCubeCache();

            // load the cubes around the spawn point
            CubicChunks.LOGGER.info("Loading cubes for spawn...");
            spawnPoint = world.getSpawnPoint();
            int spawnCubeX = Coords.blockToCube(spawnPoint.getX());
            int spawnCubeY = Coords.blockToCube(spawnPoint.getY());
            int spawnCubeZ = Coords.blockToCube(spawnPoint.getZ());

            long lastTime = System.currentTimeMillis();
            final int progressReportInterval = 1000;//ms
            int totalToGenerate = (radius * 2 + 1) * (radius * 2 + 1) * (radius * 2 + 1);
            int generated = 0;

            for (int cubeX = spawnCubeX - radius; cubeX <= spawnCubeX + radius; cubeX++) {
                for (int cubeZ = spawnCubeZ - radius; cubeZ <= spawnCubeZ + radius; cubeZ++) {
                    for (int cubeY = spawnCubeY + radius; cubeY >= spawnCubeY - radius; cubeY--) {
                        serverCubeCache.getCube(cubeX, cubeY, cubeZ, IProviderExtras.Requirement.LIGHT).getTickets().add(this);
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
            return true;
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            this.radius = nbt.getInteger("spawnRadius");
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
            nbt.setInteger("spawnRadius", this.radius);
            return nbt;
        }

        public static SpawnArea get(ICubicWorld world) {
            MapStorage storage = ((World) world).getPerWorldStorage();
            SpawnArea area = (SpawnArea) storage.getOrLoadData(SpawnArea.class, STORAGE);

            if (area == null) {
                area = new SpawnArea();
                storage.setData(STORAGE, area);
            }
            return area;
        }
    }
}
