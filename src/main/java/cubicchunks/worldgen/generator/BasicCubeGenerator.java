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
package cubicchunks.worldgen.generator;

import cubicchunks.util.Coords;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.SpawnListEntry;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A partial implementation of {@link ICubeGenerator} that handles biome assignment.
 * <p>
 * Structure recreation and lookup are not supported by default.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BasicCubeGenerator implements ICubeGenerator {

    protected ICubicWorld world;
    private Biome[] columnBiomes;

    public BasicCubeGenerator(ICubicWorld world) {
        this.world = world;
    }

    @Override
    public void generateColumn(Column column) {
        this.columnBiomes = this.world.getBiomeProvider()
                .getBiomes(this.columnBiomes,
                        Coords.cubeToMinBlock(column.getX()),
                        Coords.cubeToMinBlock(column.getZ()),
                        Cube.SIZE, Cube.SIZE);

        // Copy ids to column internal biome array
        byte[] columnBiomeArray = column.getBiomeArray();
        for (int i = 0; i < columnBiomeArray.length; ++i) {
            columnBiomeArray[i] = (byte) Biome.getIdForBiome(this.columnBiomes[i]);
        }
    }

    @Override
    public void recreateStructures(Cube cube) {
    }

    @Override
    public void recreateStructures(Column column) {
    }

    @Override
    public List<SpawnListEntry> getPossibleCreatures(EnumCreatureType type, BlockPos pos) {
        return world.getBiome(pos).getSpawnableList(type);
    }

    @Nullable @Override
    public BlockPos getClosestStructure(String name, BlockPos pos, boolean flag) {
        return null;
    }
}
