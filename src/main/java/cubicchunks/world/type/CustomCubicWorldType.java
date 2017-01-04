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
package cubicchunks.world.type;

import cubicchunks.util.Box;
import cubicchunks.util.CubePos;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.BasicCubeGenerator;
import cubicchunks.worldgen.generator.CubePrimer;
import cubicchunks.worldgen.generator.ICubeGenerator;
import cubicchunks.worldgen.generator.ICubePrimer;
import cubicchunks.worldgen.generator.custom.CustomFeatureProcessor;
import cubicchunks.worldgen.generator.custom.CustomPopulationProcessor;
import cubicchunks.worldgen.generator.custom.CustomTerrainProcessor;
import cubicchunks.worldgen.gui.CustomCubicGui;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CustomCubicWorldType extends WorldType implements ICubicWorldType {

    private CustomCubicWorldType() {
        super("CustomCubic");
    }

    public static void create() {
        new CustomCubicWorldType();
    }

    @Override
    public WorldProvider getReplacedProviderFor(WorldProvider provider) {
        return provider; // TODO: Custom Nether? Custom End????
    }

    @Override
    public ICubeGenerator createCubeGenerator(ICubicWorld world) {
        CustomTerrainProcessor terrain = new CustomTerrainProcessor(world);
        CustomFeatureProcessor features = new CustomFeatureProcessor();
        CustomPopulationProcessor population = new CustomPopulationProcessor(world);

        //TODO: this is mostly a hack to get the old system working
        return new BasicCubeGenerator(world) {
            @Override
            public ICubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
                ICubePrimer primer = new CubePrimer();

                terrain.calculate(primer, cubeX, cubeY, cubeZ);
                features.generate(world, primer, new CubePos(cubeX, cubeY, cubeZ));

                return primer;
            }

            @Override
            public void populate(Cube cube) {
                population.populate(cube);
            }

            @Override
            public Box getPopulationRequirement(Cube cube) {
                return RECOMMENDED_POPULATOR_REQUIREMENT;
            }
        };
    }

    public boolean isCustomizable() {
        return true;
    }

    @SideOnly(Side.CLIENT)
    public void onCustomizeButton(Minecraft mc, GuiCreateWorld guiCreateWorld) {
        new CustomCubicGui(guiCreateWorld).display();
    }
}
