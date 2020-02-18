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
package io.github.opencubicchunks.cubicchunks.core.client;

import io.github.opencubicchunks.cubicchunks.api.world.CubeEvent;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client.IChunkProviderClient;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.BlankCube;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static net.minecraftforge.common.MinecraftForge.EVENT_BUS;

//TODO: break off ICubeProviderInternal
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CubeProviderClient extends ChunkProviderClient implements ICubeProviderInternal {

    @Nonnull private ICubicWorldInternal.Client world;
    @Nonnull private Cube blankCube;
    @Nonnull private XYZMap<Cube> cubeMap = new XYZMap<>(0.7f, 8000);

    public CubeProviderClient(ICubicWorldInternal.Client world) {
        super((World) world);
        this.world = world;
        // chunk at Integer.MAX_VALUE will be blank chunk
        this.blankCube = new BlankCube(super.provideChunk(Integer.MAX_VALUE, 0));
    }

    @Nullable @Override
    public Chunk getLoadedColumn(int x, int z) {
        return getLoadedChunk(x, z);
    }

    @Override
    public Chunk provideColumn(int x, int z) {
        return provideChunk(x, z);
    }

    @Override
    public Chunk provideChunk(int x, int z) {
        return super.provideChunk(x, z);
    }

    @Nullable @Override
    public Chunk getLoadedChunk(int x, int z) {
        return super.getLoadedChunk(x, z);
    }

    @Override
    public Chunk loadChunk(int cubeX, int cubeZ) {
        Chunk column = new Chunk((World) this.world, cubeX, cubeZ);   // make a new one
        ((IChunkProviderClient) this).getLoadedChunks().put(ChunkPos.asLong(cubeX, cubeZ), column); // add it to the cache

        // fire a forge event... make mods happy :)
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Load(column));

        column.markLoaded(true);
        return column;
    }

    @Override
    public boolean tick() {
        long i = System.currentTimeMillis();
        for (Cube cube : cubeMap) {
            cube.tickCubeCommon(() -> System.currentTimeMillis() - i > 5L);
        }

        if (System.currentTimeMillis() - i > 100L) {
            CubicChunks.LOGGER.info("Warning: Clientside chunk ticking took {} ms", System.currentTimeMillis() - i);
        }

        return false;
    }

    //===========================
    //========Cube stuff=========
    //===========================

    /**
     * This is like ChunkProviderClient.loadChunk(), but more useful for our use case
     * It is used when the server sends a new Cube to this client,
     * and the network handler wants us to create a new Cube.
     *
     * @param pos cube position
     * @return a newly created or cached cube
     */
    @Nullable
    public Cube loadCube(CubePos pos) {
        Cube cube = getLoadedCube(pos);
        if (cube != null) {
            return cube;
        }
        Chunk column = getLoadedColumn(pos.getX(), pos.getZ());
        if (column == null) {
            return null;
        }
        cube = new Cube(column, pos.getY()); // auto added to column
        ((IColumn) column).addCube(cube);
        this.cubeMap.put(cube);
        EVENT_BUS.post(new CubeEvent.Load(cube));
        cube.setCubeLoaded();
        return cube;
    }

    /**
     * This is like ChunkProviderClient.unloadChunk()
     * It is used when the server tells the client to unload a Cube.
     *
     * @param pos position to unload
     */
    public void unloadCube(CubePos pos) {
        Cube cube = getLoadedCube(pos);
        if (cube == null) {
            return;
        }
        cube.onUnload();
        cubeMap.remove(pos.getX(), pos.getY(), pos.getZ());
        cube.getColumn().removeCube(pos.getY());
    }

    @Override
    public Cube getCube(int cubeX, int cubeY, int cubeZ) {
        Cube cube = getLoadedCube(cubeX, cubeY, cubeZ);
        if (cube == null) {
            return blankCube;
        }
        return cube;
    }

    @Override
    public Cube getCube(CubePos coords) {
        return getCube(coords.getX(), coords.getY(), coords.getZ());
    }

    @Nullable @Override
    public Cube getLoadedCube(int cubeX, int cubeY, int cubeZ) {
        return cubeMap.get(cubeX, cubeY, cubeZ);
    }

    @Nullable @Override
    public Cube getLoadedCube(CubePos coords) {
        return getLoadedCube(coords.getX(), coords.getY(), coords.getZ());
    }

    public Iterable<Chunk> getLoadedChunks() {
        return ((IChunkProviderClient) this).getLoadedChunks().values();
    }

    @Override
    public String makeString() {
        return "MultiplayerChunkCache: " + ((IChunkProviderClient) this).getLoadedChunks().values()
                .stream()
                .map(c -> ((IColumn) c).getLoadedCubes().size())
                .reduce(Integer::sum)
                .orElse(-1) + "/" + ((IChunkProviderClient) this).getLoadedChunks().size();
    }
}
