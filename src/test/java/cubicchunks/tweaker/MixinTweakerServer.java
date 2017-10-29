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
package cubicchunks.tweaker;

import cubicchunks.asm.CubicChunksCoreMod;
import cubicchunks.util.ReflectionUtil;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.gradle.GradleStartCommon;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.lwts.AbstractTestTweaker;

import java.io.File;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MixinTweakerServer extends AbstractTestTweaker {

    @Override
    public void injectIntoClassLoader(LaunchClassLoader loader) {
        System.setProperty("net.minecraftforge.gradle.GradleStart.srg.srg-mcp",
                ((File) ReflectionHelper.getPrivateValue(GradleStartCommon.class, null, "SRG_SRG_MCP")).getPath());
        super.injectIntoClassLoader(loader);
        registerAccessTransformer("META-INF/cubicchunks_at.cfg");
        CubicChunksCoreMod.initMixin();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.SERVER);
    }
}
