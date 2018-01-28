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
package io.github.opencubicchunks.cubicchunks.api.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.apache.commons.lang3.reflect.FieldUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;

import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.ICubicPopulator;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.registry.GameRegistry;

// Based on net.minecraftforge.fml.common.registry.GameRegistry.
public class CubeGeneratorsRegistry {

    private static List<ICubicPopulator> sortedGeneratorList;

    /**
     * Callback hook for cube gen - if your mod wishes to add extra mod related
     * generation to the world call this
     *
     * @param random the cube specific {@link Random}.
     * @param pos is position of the populated cube
     * @param world The {@link ICubicWorld} we're generating for
     * @param biome The biome we are generating in
     */
    public static void generateWorld(World world, Random random, CubePos pos, Biome biome) {
        for (ICubicPopulator generator : sortedGeneratorList) {
            generator.generate(world, random, pos, biome);
        }
    }

    @SuppressWarnings("unchecked")
    public static void computeSortedGeneratorList() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Set<IWorldGenerator> forgeWorldGenerators = (HashSet<IWorldGenerator>) FieldUtils.readDeclaredStaticField(GameRegistry.class, "worldGenerators", true);
        Map<IWorldGenerator, Integer> forgeWorldGeneratorIndex = (HashMap<IWorldGenerator, Integer>) FieldUtils.readDeclaredStaticField(GameRegistry.class, "worldGeneratorIndex", true);
        List<ICubicPopulator> list = new ArrayList<ICubicPopulator>();
        for (IWorldGenerator worldGenerator : forgeWorldGenerators) {
            if (worldGenerator instanceof ICubicPopulator) {
                list.add((ICubicPopulator) worldGenerator);
            }
        }
        Collections.sort(list, (o1, o2) -> Ints.compare(forgeWorldGeneratorIndex.get(o1), forgeWorldGeneratorIndex.get(o2)));
        sortedGeneratorList = ImmutableList.copyOf(list);
    }
}
