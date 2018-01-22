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
package cubicchunks.client;

import cubicchunks.CubicChunks;
import cubicchunks.util.CubePos;
import cubicchunks.util.XYZMap;
import cubicchunks.world.ICubeProvider;
import cubicchunks.world.ICubicWorldClient;
import cubicchunks.world.column.IColumn;
import cubicchunks.world.cube.BlankCube;
import cubicchunks.world.cube.Cube;
import jline.internal.Preconditions;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

//TODO: break off ICubeProvider
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CubeProviderClient extends ChunkProviderClient implements ICubeProvider {

    @Nonnull private ICubicWorldClient world;
    @Nonnull private Cube blankCube;
    @Nonnull private XYZMap<Cube> cubeMap = new XYZMap<>(0.7f, 8000);

    public CubeProviderClient(ICubicWorldClient world) {
        super((World) world);
        this.world = world;
        this.blankCube = new BlankCube((IColumn) blankChunk);
    }

    @Nullable @Override
    public IColumn getLoadedColumn(int x, int z) {
        return (IColumn) getLoadedChunk(x, z);
    }

    @Override
    public IColumn provideColumn(int x, int z) {
        return (IColumn) provideChunk(x, z);
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
        this.chunkMapping.put(ChunkPos.asLong(cubeX, cubeZ), column); // add it to the cache

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
     * @return a newly created or cached cube
     */
    @Nullable
    public Cube loadCube(CubePos pos) {
        Cube cube = getLoadedCube(pos);
        if (cube != null) {
            return cube;
        }
        IColumn column = getLoadedColumn(pos.getX(), pos.getZ());
        if (column == null) {
            return null;
        }
        cube = new Cube(column, pos.getY()); // auto added to column
        cube.setCubeLoaded();
        column.addCube(cube);
        this.cubeMap.put(cube);

        return cube;
    }

    /**
     * This is like ChunkProviderClient.unloadChunk()
     * It is used when the server tells the client to unload a Cube.
     */
    public void unloadCube(CubePos pos) {
        cubeMap.remove(pos.getX(), pos.getY(), pos.getZ());
        IColumn IColumn = getLoadedColumn(pos.getX(), pos.getZ());
        if (IColumn != null) {
            IColumn.removeCube(pos.getY());
        }
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
        return this.chunkMapping.values();
    }

    @Override
    public String makeString() {
        return "MultiplayerChunkCache: " + this.chunkMapping.values()
                .stream()
                .map(c -> ((IColumn) c).getLoadedCubes().size())
                .reduce((a, b) -> a + b)
                .orElse(-1) + "/" + this.chunkMapping.size();
    }
}
