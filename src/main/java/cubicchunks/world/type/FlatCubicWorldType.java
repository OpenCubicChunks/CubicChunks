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

import cubicchunks.CubicChunks;
import cubicchunks.util.IntRange;
import cubicchunks.world.ICubicWorld;
import cubicchunks.worldgen.generator.ICubeGenerator;
import cubicchunks.worldgen.generator.flat.FlatTerrainProcessor;
import cubicchunks.worldgen.gui.FlatCubicGui;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FlatCubicWorldType extends WorldType implements ICubicWorldType {

    public FlatCubicWorldType() {//todo: make it private, used in test
        super("FlatCubic");
    }

    public static FlatCubicWorldType create() {
        return new FlatCubicWorldType();
    }

    @Override
    public ICubeGenerator createCubeGenerator(ICubicWorld world) {
        return new FlatTerrainProcessor(world);
    }

    @Override public IntRange calculateGenerationHeightRange(WorldServer world) {
        return new IntRange(0, 256); // TODO: Flat generation height range
    }

    public boolean isCustomizable() {
        return true;
    }

    @SideOnly(Side.CLIENT)
    public void onCustomizeButton(Minecraft mc, GuiCreateWorld guiCreateWorld) {
        if (Loader.isModLoaded("malisiscore")) {
            new FlatCubicGui(guiCreateWorld).display();
        } else {
            mc.displayGuiScreen(new GuiErrorScreen("MalisisCore not found!",
                    "You need to install MalisisCore version at least " + CubicChunks
                            .MALISIS_VERSION + " to use world customization"));
        }
    }
}
