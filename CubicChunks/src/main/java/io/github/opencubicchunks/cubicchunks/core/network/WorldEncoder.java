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
package io.github.opencubicchunks.cubicchunks.core.network;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.core.world.ClientHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.ServerHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.chunk.Chunk;
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
            byte flags = 0;
            if(cube.isEmpty())
                flags |= 1;
            if(cube.getStorage() != null)
                flags |= 2;
            if(cube.getBiomeArray() != null)
                flags |= 4;
            out.writeByte(flags);
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
                out.writeBytes(cube.getStorage().getBlockLight().getData());
            }
        });

        // 4. sky light
        cubes.forEach(cube -> {
            if (cube.getStorage() != null && cube.getWorld().provider.hasSkyLight()) {
                out.writeBytes(cube.getStorage().getSkyLight().getData());
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
        
        // 6. biomes
        cubes.forEach(cube -> {
            if (cube.getBiomeArray() != null)
                out.writeBytes(cube.getBiomeArray());
        });
    }

    static void encodeColumn(PacketBuffer out, Chunk column) {
        // 1. biomes
        out.writeBytes(column.getBiomeArray());
    }

    static void decodeColumn(PacketBuffer in, Chunk column) {
        // 1. biomes
        in.readBytes(column.getBiomeArray());
    }

    static void decodeCube(PacketBuffer in, List<Cube> cubes) {
        cubes.stream().filter(Objects::nonNull).forEach(Cube::setClientCube);

        // 1. emptiness
        boolean[] isEmpty = new boolean[cubes.size()];
        boolean[] hasStorage = new boolean[cubes.size()];
        boolean[] hasCustomBiomeMap = new boolean[cubes.size()];

        for (int i = 0; i < cubes.size(); i++) {
            byte flags = in.readByte();
            isEmpty[i] = (flags & 1) != 0 || cubes.get(i) == null;
            hasStorage[i] = (flags & 2) != 0 && cubes.get(i) != null;
            hasCustomBiomeMap[i] = (flags & 4) != 0 && cubes.get(i) != null;
        }

        for (int i = 0; i < cubes.size(); i++) {
            if (hasStorage[i]) {
                Cube cube = cubes.get(i);
                ExtendedBlockStorage storage = new ExtendedBlockStorage(Coords.cubeToMinBlock(cube.getY()),
                        cube.getWorld().provider.hasSkyLight());
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
                byte[] data = cubes.get(i).getStorage().getBlockLight().getData();
                in.readBytes(data);
            }
        }

        // 4. sky light
        for (int i = 0; i < cubes.size(); i++) {
            if (hasStorage[i] && cubes.get(i).getWorld().provider.hasSkyLight()) {
                //noinspection ConstantConditions
                byte[] data = cubes.get(i).getStorage().getSkyLight().getData();
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
                cube.getStorage().recalculateRefCounts();
            }
        }
        
        // 6. biomes
        for (int i = 0; i < cubes.size(); i++) {
            if (!hasCustomBiomeMap[i])
                continue;
            Cube cube = cubes.get(i);
            byte[] blockBiomeArray = new byte[Coords.BIOMES_PER_CUBE];
            in.readBytes(blockBiomeArray);
            cube.setBiomeArray(blockBiomeArray);
        }
    }

    static int getEncodedSize(Chunk column) {
        return column.getBiomeArray().length;
    }

    static int getEncodedSize(Collection<Cube> cubes) {
        int size = 0;

        // 1. isEmpty, hasStorage and hasBiomeArray flags packed in one byte
        size += cubes.size();

        // 2. block IDs and metadata
        for (Cube cube : cubes) {
            if (!cube.isEmpty()) {
                //noinspection ConstantConditions
                size += cube.getStorage().getData().getSerializedSize();
            }
            if (cube.getStorage() != null) {
                size += cube.getStorage().getBlockLight().getData().length;
                if (cube.getWorld().provider.hasSkyLight()) {
                    size += cube.getStorage().getSkyLight().getData().length;
                }
            }
        }

        // heightmaps
        size += 256 * Integer.BYTES * cubes.size();
        // biomes
        for (Cube cube : cubes) {
            byte[] biomeArray = cube.getBiomeArray();
            if (biomeArray == null)
                continue;
            size += biomeArray.length;
        }
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
