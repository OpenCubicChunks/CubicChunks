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
package cubicchunks.worldgen.generator.flat;

import cubicchunks.api.worldgen.biome.CubicBiome;
import cubicchunks.util.Box;
import cubicchunks.util.Coords;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.BasicCubeGenerator;
import cubicchunks.worldgen.generator.CubeGeneratorsRegistry;
import cubicchunks.worldgen.generator.CubePrimer;
import cubicchunks.worldgen.generator.ICubePrimer;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A cube generator that generates a flat surface of grass, dirt and stone.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FlatTerrainProcessor extends BasicCubeGenerator {

    private final FlatGeneratorSettings conf;

    public FlatTerrainProcessor(ICubicWorld world) {
        super(world);
        String json = world.getWorldInfo().getGeneratorOptions();
        conf = FlatGeneratorSettings.fromJson(json);
    }

    @Override
    public ICubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
        ICubePrimer primer = new CubePrimer();
        int floorY = Coords.cubeToMinBlock(cubeY);
        int topY = Coords.cubeToMinBlock(cubeY + 1);
        NavigableMap<Integer, Layer> cubeLayerSubMap = conf.layers.subMap(conf.layers.floorKey(floorY), true, topY, false);
        for (Entry<Integer, Layer> entry : cubeLayerSubMap.entrySet()) {
            Layer layer = entry.getValue();
            int fromY = layer.fromY - floorY;
            int toY = layer.toY - floorY;
            IBlockState iBlockState = layer.blockState;
            for (int y = fromY > 0 ? fromY : 0; y < (toY < 16 ? toY : 16); y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        primer.setBlockState(x, y, z, iBlockState);
                    }
                }
            }
        }
        return primer;
    }

    @Override
    public void populate(Cube cube) {
        CubeGeneratorsRegistry.generateWorld(cube.getCubicWorld(),
                new Random(cube.cubeRandomSeed()), cube.getCoords(),
                CubicBiome.getCubic(world.getBiome(cube.getCoords().getCenterBlockPos()))
        );
    }

    @Override
    public Box getPopulationRequirement(Cube cube) {
        return NO_POPULATOR_REQUIREMENT;
    }

    @Override
    public BlockPos getClosestStructure(String name, BlockPos pos, boolean flag) {
        // eyes of ender are the new F3 for finding the origin :P
        return name.equals("Stronghold") ? new BlockPos(0, 0, 0) : null; 
    }
}
