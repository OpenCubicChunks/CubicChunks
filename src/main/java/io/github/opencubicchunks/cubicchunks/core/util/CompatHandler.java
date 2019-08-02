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
package io.github.opencubicchunks.cubicchunks.core.util;

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.Loader;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CompatHandler {


    private static final Map<String, String> packageToModId = getPackageToModId();

    private static Map<String, String> getPackageToModId() {
        return Collections.unmodifiableMap(Loader.instance().getActiveModList().stream()
                .flatMap(mod -> mod.getOwnedPackages().stream().map(pkg -> new AbstractMap.SimpleEntry<>(pkg, mod.getModId())))
                .collect(Collectors
                        .toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue, (a, b) -> a.equals(b) ? a : a + " or " + b)));
    }

    public static Set<String> getModsForStacktrace(StackTraceElement[] stacktrace) {
        Set<String> mods = new HashSet<>();
        for (StackTraceElement traceElement : stacktrace) {
            try {
                Class<?> cl = Class.forName(traceElement.getClassName());
                if (cl != null && cl.getPackage() != null) {
                    String modid = packageToModId.get(cl.getPackage().getName());
                    if (modid != null && !modid.equals("minecraft") && !modid.equals("forge") && !modid.equals("cubicchunks")) {
                        mods.add(modid);
                    }
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
        return mods;
    }

    public static void beforeGenerate(World world, IWorldGenerator generator) {
        Class<? extends IWorldGenerator> genClass = generator.getClass();
        if (!packageToModId.containsKey(genClass.getPackage().getName())) {
            CubicChunks.bigWarning("Found IWorldGenerator %s that doesn't come from any mod! This is most likely a bug.", genClass);
            return;
        }
        String modid = packageToModId.get(genClass.getPackage().getName());
        if (modid.equals("ic2") || modid.equals("thaumcraft")) {
            ((ICubicWorldInternal.Server) world).fakeWorldHeight(256);
        }
    }

    public static void afterGenerate(World world) {
        ((ICubicWorldInternal.Server) world).fakeWorldHeight(0);
    }
}
