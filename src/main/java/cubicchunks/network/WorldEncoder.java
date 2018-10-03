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
package cubicchunks.network;

import cubicchunks.util.Coords;
import cubicchunks.world.ClientHeightMap;
import cubicchunks.world.ServerHeightMap;
import cubicchunks.world.column.IColumn;
import cubicchunks.world.cube.Cube;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
class WorldEncoder {

    static void encodeCubes(PacketBuffer out, Collection<Cube> cubes) {
        // write first all the flags, then all the block data, then all the light data etc for better compression

        // 1. emptiness
        cubes.forEach(cube -> {
            out.writeBoolean(cube.isEmpty());
            out.writeBoolean(cube.getStorage() != null);
        });

        // 2. block IDs and metadata
        cubes.forEach(cube -> {
            if (!cube.isEmpty()) {
                //noinspection ConstantConditions
                cube.getStorage().getData().write(out);
            }
        });

        // 3. block light
        cubes.forEach(cube -> {
            if (cube.getStorage() != null) {
                out.writeBytes(cube.getStorage().getBlocklightArray().getData());
            }
        });

        // 4. sky light
        cubes.forEach(cube -> {
            if (cube.getStorage() != null && !cube.getCubicWorld().getProvider().hasNoSky()) {
                out.writeBytes(cube.getStorage().getSkylightArray().getData());
            }
        });

        // 5. heightmap and bottom-block-y. Each non-empty cube has a chance
        // to update this data.
        // trying to keep track of when it changes would be complex, so send
        // it wil all cubes
        cubes.forEach(cube -> {
            if (!cube.isEmpty()) {
                byte[] heightmaps = ((ServerHeightMap) cube.getColumn().getOpacityIndex()).getDataForClient();
                assert heightmaps.length == Cube.SIZE * Cube.SIZE * Integer.BYTES;
                out.writeBytes(heightmaps);
            }
        });
    }

    static void encodeColumn(PacketBuffer out, IColumn column) {
        // 1. biomes
        out.writeBytes(column.getBiomeArray());
    }

    static void decodeColumn(PacketBuffer in, IColumn column) {
        // 1. biomes
        in.readBytes(column.getBiomeArray());
    }

    static void decodeCube(PacketBuffer in, List<Cube> cubes) {
        cubes.stream().filter(Objects::nonNull).forEach(Cube::setClientCube);

        // 1. emptiness
        boolean[] isEmpty = new boolean[cubes.size()];
        boolean[] hasStorage = new boolean[cubes.size()];

        for (int i = 0; i < cubes.size(); i++) {
            isEmpty[i] = in.readBoolean() || cubes.get(i) == null;
            hasStorage[i] = in.readBoolean() && cubes.get(i) != null;
        }

        for (int i = 0; i < cubes.size(); i++) {
            if (hasStorage[i]) {
                Cube cube = cubes.get(i);
                ExtendedBlockStorage storage = new ExtendedBlockStorage(Coords.cubeToMinBlock(cube.getY()),
                        !cube.getCubicWorld().getProvider().hasNoSky());
                cube.setStorage(storage);
            }
        }

        // 2. Block IDs and metadata
        for (int i = 0; i < cubes.size(); i++) {
            if (!isEmpty[i]) {
                //noinspection ConstantConditions
                cubes.get(i).getStorage().getData().read(in);
            }
        }

        // 3. block light
        for (int i = 0; i < cubes.size(); i++) {
            if (hasStorage[i]) {
                //noinspection ConstantConditions
                byte[] data = cubes.get(i).getStorage().getBlocklightArray().getData();
                in.readBytes(data);
            }
        }

        // 4. sky light
        for (int i = 0; i < cubes.size(); i++) {
            if (hasStorage[i] && !cubes.get(i).getCubicWorld().getProvider().hasNoSky()) {
                //noinspection ConstantConditions
                byte[] data = cubes.get(i).getStorage().getSkylightArray().getData();
                in.readBytes(data);
            }
        }

        // 5. heightmaps and after all that - update ref counts
        for (int i = 0; i < cubes.size(); i++) {
            if (!isEmpty[i]) {
                Cube cube = cubes.get(i);
                byte[] heightmaps = new byte[Cube.SIZE * Cube.SIZE * Integer.BYTES];
                in.readBytes(heightmaps);
                ClientHeightMap coi = ((ClientHeightMap) cube.getColumn().getOpacityIndex());
                coi.setData(heightmaps);

                //noinspection ConstantConditions
                cube.getStorage().removeInvalidBlocks(); // recalculateRefCounts
            }
        }
    }

    static int getEncodedSize(IColumn column) {
        return column.getBiomeArray().length;
    }

    static int getEncodedSize(Collection<Cube> cubes) {
        int size = 0;

        size += 2 * cubes.size(); // 1. isEmpty and hasStorage flags

        // 2. block IDs and metadata
        for (Cube cube : cubes) {
            if (!cube.isEmpty()) {
                //noinspection ConstantConditions
                size += cube.getStorage().getData().getSerializedSize();
            }
            if (cube.getStorage() != null) {
                size += cube.getStorage().getBlocklightArray().getData().length;
                if (!cube.getCubicWorld().getProvider().hasNoSky()) {
                    size += cube.getStorage().getSkylightArray().getData().length;
                }
            }
        }

        // heightmaps
        size += 256 * Integer.BYTES * cubes.size();
        return size;
    }

    static ByteBuf createByteBufForWrite(byte[] data) {
        ByteBuf bytebuf = Unpooled.wrappedBuffer(data);
        bytebuf.writerIndex(0);
        return bytebuf;
    }

    static ByteBuf createByteBufForRead(byte[] data) {
        ByteBuf bytebuf = Unpooled.wrappedBuffer(data);
        bytebuf.readerIndex(0);
        return bytebuf;
    }
}
