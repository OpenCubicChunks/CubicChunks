/*
 *  This file is part of CubicChunks, licensed under the MIT License (MIT).
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

import com.google.common.collect.ImmutableList;
import net.minecraft.world.ServerMultiWorld;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.chunk.ServerChunkProvider;

import java.util.List;
import java.util.Optional;

public class CompatUtil {
    public static boolean hasOptifine() {
        return false; // OptiFine for forge doesn't exist at the moment of writing this
    }


    @SuppressWarnings("unchecked")
    private static final List<Class<?>> allowedServerWorldClasses = ImmutableList.copyOf(new Class[]{
        ServerWorld.class,
        ServerMultiWorld.class,
        // non-existing classes will be Objects
        getClass("WorldServerOF"), // OptiFine's WorldServer, no package
        getClass("WorldServerMultiOF"), // OptiFine's WorldServerMulti, no package
        getClass("net.optifine.override.WorldServerOF"), // OptiFine's WorldServer
        getClass("net.optifine.override.WorldServerMultiOF"), // OptiFine's WorldServerMulti
        getClass("com.forgeessentials.multiworld.WorldServerMultiworld") // ForgeEssentials world
    });

    @SuppressWarnings("unchecked")
    private static final List<Class<?>> allowedServerChunkProviderClasses = ImmutableList.copyOf(new Class[]{
        ServerChunkProvider.class
    });

    private static Class<?> getClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return Object.class;
        }
    }

    public static boolean shouldSkipWorld(World world) {
        return !allowedServerWorldClasses.contains(world.getClass())
            || !allowedServerChunkProviderClasses.contains(world.getChunkProvider().getClass());
    }
}
