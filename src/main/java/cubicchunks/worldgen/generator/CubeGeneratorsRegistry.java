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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;

import cubicchunks.api.ICubicWorldGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.registry.GameRegistry;

// Based on net.minecraftforge.fml.common.registry.GameRegistry.
public class CubeGeneratorsRegistry {

    private static List<ICubicWorldGenerator> sortedGeneratorList;

    /**
     * Callback hook for cube gen - if your mod wishes to add extra mod related
     * generation to the world call this
     *
     * @param random the cube specific {@link Random}.
     * @param pos is a position of a block in cube with lowest world coordinate
     *        {@link BlockPos}.
     * @param world The minecraft {@link World} we're generating for.
     */
    public static void generateWorld(Random random, BlockPos pos, World world) {
        for (ICubicWorldGenerator generator : sortedGeneratorList) {
            generator.generate(random, pos, world);
        }
    }

    @SuppressWarnings("unchecked")
    public static void computeSortedGeneratorList() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Set<IWorldGenerator> forgeWorldGenerators = (HashSet<IWorldGenerator>) FieldUtils.readDeclaredStaticField(GameRegistry.class, "worldGenerators", true);
        Map<IWorldGenerator, Integer> forgeWorldGeneratorIndex = (HashMap<IWorldGenerator, Integer>) FieldUtils.readDeclaredStaticField(GameRegistry.class, "worldGeneratorIndex", true);
        List<ICubicWorldGenerator> list = new ArrayList<ICubicWorldGenerator>();
        for (IWorldGenerator worldGenerator : forgeWorldGenerators) {
            if (worldGenerator instanceof ICubicWorldGenerator) {
                list.add((ICubicWorldGenerator) worldGenerator);
            }
        }
        Collections.sort(list, new Comparator<ICubicWorldGenerator>() {

            @Override
            public int compare(ICubicWorldGenerator o1, ICubicWorldGenerator o2) {
                return Ints.compare(forgeWorldGeneratorIndex.get(o1), forgeWorldGeneratorIndex.get(o2));
            }
        });
        sortedGeneratorList = ImmutableList.copyOf(list);
    }
}
