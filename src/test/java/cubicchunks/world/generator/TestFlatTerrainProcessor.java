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
package cubicchunks.world.generator;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;

import cubicchunks.testutil.MinecraftEnvironment;
import cubicchunks.util.Coords;
import cubicchunks.world.CubicWorld;
import cubicchunks.worldgen.generator.ICubePrimer;
import cubicchunks.worldgen.generator.flat.FlatGeneratorSettings;
import cubicchunks.worldgen.generator.flat.FlatTerrainProcessor;
import cubicchunks.worldgen.generator.flat.Layer;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.storage.WorldInfo;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestFlatTerrainProcessor {

    static IBlockState nw;
    @Before
    public void setUp() throws IOException {
        MinecraftEnvironment.init();
        nw = Blocks.NETHER_WART_BLOCK.getDefaultState();
    }

    @Test
    public void testFlatTerrainProcessor() {
        int checkFromY = -1;
        int checkToY = 1;
        FlatGeneratorSettings fgs = new FlatGeneratorSettings();
        CubicWorld world = mock(CubicWorld.class);
        WorldInfo worldInfo = mock(WorldInfo.class);
        when(world.getWorldInfo()).thenReturn(worldInfo);
        
        // Default settings
        when(worldInfo.getGeneratorOptions()).thenReturn(fgs.toJson());
        FlatTerrainProcessor ftp = new FlatTerrainProcessor(world);
        for (int i = checkFromY; i <= checkToY; i++)
            ftp.generateCube(0, i, 0);
        
        // No layers at all
        fgs.layers.clear();
        when(worldInfo.getGeneratorOptions()).thenReturn(fgs.toJson());
        ftp = new FlatTerrainProcessor(world);
        ICubePrimer primer = null;
        for (int i = checkFromY; i <= checkToY; i++) {
            primer = ftp.generateCube(0, i, 0);
            assertEquals(ICubePrimer.DEFAULT_STATE, primer.getBlockState(8, 8, 8));
        }
        
        // Single layer in a middle of every cube
        for (int i = checkFromY; i <= checkToY; i++)
            fgs.layers.put(8 + Coords.cubeToMinBlock(i), new Layer(8 + Coords.cubeToMinBlock(i), 9 + Coords.cubeToMinBlock(i), nw));
        when(worldInfo.getGeneratorOptions()).thenReturn(fgs.toJson());
        ftp = new FlatTerrainProcessor(world);
        for (int i = checkFromY; i <= checkToY; i++) {
            primer = ftp.generateCube(0, i, 0);
            assertEquals(nw,primer.getBlockState(8, 8, 8));
        }
        
        // Two layers with a gap in-between
        for (int i = checkFromY; i <= checkToY; i++)
            fgs.layers.put(12 + Coords.cubeToMinBlock(i), new Layer(12 + Coords.cubeToMinBlock(i), 13 + Coords.cubeToMinBlock(i), nw));
        when(worldInfo.getGeneratorOptions()).thenReturn(fgs.toJson());
        ftp = new FlatTerrainProcessor(world);
        for (int i = checkFromY; i <= checkToY; i++) {
            primer = ftp.generateCube(0, i, 0);
            assertEquals(ICubePrimer.DEFAULT_STATE, primer.getBlockState(8, 11, 8));
        }
    }
}
