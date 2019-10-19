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
package io.github.opencubicchunks.cubicchunks.core.asm.coremod;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IEnvironmentTokenProvider;

import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

// this needs to be in separate package because the package with the coremod is added to transformer exclusions
// and we need mixins to still be transformed for runtime deobfuscation
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
// the mcVersion value is inlined at compile time, so this MC version check may still fail
@IFMLLoadingPlugin.MCVersion(value = ForgeVersion.mcVersion)
@IFMLLoadingPlugin.SortingIndex(value = 5000)
public class CubicChunksCoreMod implements IFMLLoadingPlugin {

    public static final class TokenProvider implements IEnvironmentTokenProvider {

        @Override
        public int getPriority() {
            return IEnvironmentTokenProvider.DEFAULT_PRIORITY;
        }

        @Override
        public Integer getToken(String token, MixinEnvironment env) {
            if ("FORGE".equals(token)) {
                return Integer.valueOf(ForgeVersion.getBuildVersion());
            } else if ("FML".equals(token)) {
                String fmlVersion = Loader.instance().getFMLVersionString();
                int build = Integer.parseInt(fmlVersion.substring(fmlVersion.lastIndexOf('.') + 1));
                return Integer.valueOf(build);
            } else if ("MC_FORGE".equals(token)) {
                return ForgeVersion.minorVersion;
            }
            return null;
        }

    }

    public CubicChunksCoreMod() {
        initMixin();
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{"io.github.opencubicchunks.cubicchunks.core.asm.transformer.CubicChunksWorldEditTransformer"};
    }

    @Nullable @Override
    public String getModContainerClass() {
        return "io.github.opencubicchunks.cubicchunks.core.asm.CubicChunksCoreContainer";
    }

    @Nullable @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Nullable @Override
    public String getAccessTransformerClass() {
        return null;
    }

    public static void initMixin() {
        MixinBootstrap.init();
        Mixins.addConfiguration("cubicchunks.mixins.core.json");
        Mixins.addConfiguration("cubicchunks.mixins.fixes.json");
        Mixins.addConfiguration("cubicchunks.mixins.selectable.json");
        Mixins.addConfiguration("cubicchunks.mixins.noncritical.json");
        MixinEnvironment.getDefaultEnvironment().registerTokenProviderClass(
                "io.github.opencubicchunks.cubicchunks.core.asm.coremod.CubicChunksCoreMod$TokenProvider");
    }
}
