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
package cubicchunks.lighting;

import cubicchunks.network.PacketCubeSkyLightUpdates;
import cubicchunks.server.CubeWatcher;
import cubicchunks.server.PlayerCubeMap;
import cubicchunks.util.AddressTools;
import cubicchunks.util.CubePos;
import cubicchunks.util.XYZAddressable;
import cubicchunks.util.XYZMap;
import gnu.trove.list.TShortList;
import gnu.trove.list.array.TShortArrayList;
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
        CubeUpdateList list = cubes.get(blockPos.getX(), blockPos.getY(), blockPos.getZ());
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
                watcher.sendPacketToAllPlayers(new PacketCubeSkyLightUpdates(watcher.getCube(), this.updates));
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
