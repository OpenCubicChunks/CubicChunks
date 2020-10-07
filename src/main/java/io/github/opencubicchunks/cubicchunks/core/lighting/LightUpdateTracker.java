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
package io.github.opencubicchunks.cubicchunks.core.lighting;

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToCube;

import gnu.trove.list.TShortList;
import gnu.trove.list.array.TShortArrayList;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XYZAddressable;
import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.core.network.PacketCubeSkyLightUpdates;
import io.github.opencubicchunks.cubicchunks.core.server.CubeWatcher;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.core.util.AddressTools;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.util.math.BlockPos;

/**
 * Tracks FirstLightProcessor lighting updates and sends them to client
 */
class LightUpdateTracker {

    private final PlayerCubeMap cubeMap;
    private XYZMap<CubeUpdateList> cubes = new XYZMap<>(0.5f, 100);

    LightUpdateTracker(PlayerCubeMap cubeMap) {
        this.cubeMap = cubeMap;
    }

    void onUpdate(BlockPos blockPos) {
        CubeUpdateList list = cubes.get(
                blockToCube(blockPos.getX()),
                blockToCube(blockPos.getY()),
                blockToCube(blockPos.getZ())
        );
        if (list == null) {
            list = new CubeUpdateList(CubePos.fromBlockCoords(blockPos));
            cubes.put(list);
        }
        list.add(blockPos);
    }

    void sendAll() {
        cubes.forEach(CubeUpdateList::send);
        cubes = new XYZMap<>(0.5f, 100);
    }

    private class CubeUpdateList implements XYZAddressable {

        private static final int MAX_COUNT = 64;
        private final CubePos pos;
        private final TShortList updates = new TShortArrayList(MAX_COUNT);

        CubeUpdateList(CubePos pos) {
            this.pos = pos;
        }

        void add(BlockPos pos) {
            if (updates.size() >= MAX_COUNT) {
                return;
            }
            updates.add((short) AddressTools.getLocalAddress(pos));
        }

        void send() {
            CubeWatcher watcher = cubeMap.getCubeWatcher(this.pos);
            if (watcher != null && watcher.isSentToPlayers()) {
                Cube cube = watcher.getCube();
                assert cube != null;
                if (updates.size() >= MAX_COUNT) {
                    watcher.sendPacketToAllPlayers(new PacketCubeSkyLightUpdates(cube));
                } else {
                    watcher.sendPacketToAllPlayers(new PacketCubeSkyLightUpdates(cube, this.updates));
                }
            }
            updates.clear();
        }

        @Override public int getX() {
            return pos.getX();
        }

        @Override public int getY() {
            return pos.getY();
        }

        @Override public int getZ() {
            return pos.getZ();
        }
    }
}
