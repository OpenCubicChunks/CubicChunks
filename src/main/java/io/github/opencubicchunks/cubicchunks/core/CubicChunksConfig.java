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
package io.github.opencubicchunks.cubicchunks.core;

import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeSet;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mod.EventBusSubscriber(modid = CubicChunks.MODID)
@Config(modid = CubicChunks.MODID, category = "general")
public class CubicChunksConfig {

    @Config.Comment("Chunk garbage collector update interval. Lower value will increase CPU usage, but can reduce memory usage.")
    @Config.LangKey("cubicchunks.config.chunk_gc_interval")
    public static int chunkGCInterval = 20 * 10;


    @Config.LangKey("cubicchunks.config.force_cc")
    @Config.Comment("Enabling this will force creating a cubic chunks world, even if it's not cubic chunks world type. This option is automatically "
            + "set in world creation GUI when creating cubic chunks world with non-cubicchunks world type. This doesn't affect already created "
            + "worlds.")
    public static boolean forceCubicChunks = false;

    @Config.LangKey("cubicchunks.config.cubegen_per_tick")
    @Config.Comment("The maximum number of cubic chunks to generate per tick.")
    public static int maxGeneratedCubesPerTick = 49 * 16;

    @Config.LangKey("cubicchunks.config.fast_entity_spawner")
    @Config.Comment("Enabling this option allows using fast entity spawner instead of vanilla-alike."
            + " Fast entity spawner can reduce server lag. Entity respawn speed will be slightly slower (only one pack per tick)"
            + " and amount of spawned mob will depend only from amount of players.")
    public static boolean useFastEntitySpawner = false;

    @Config.LangKey("cubicchunks.config.use_vanilla_world_generators")
    @Config.Comment("Enabling this option will force cubic chunks to use world generators designed for two dimensional chunks, which are often used "
            + "for custom ore generators added by mods. To do so cubic chunks will pregenerate cubes in a range of height from 0 to 255. This is "
            + "very likely to break a lot of mods, cause the game to hang, and make the world generation depend on the order in which things are "
            + "generated. Use at your own risk.")
    public static boolean useVanillaChunkWorldGenerators = false;

    @Config.LangKey("cubicchunks.config.vert_view_distance")
    @Config.Comment("Similar to Minecraft's view distance, only for vertical chunks. Automatically adjusted by vertical view distance sloder on the"
            + " client. Does not affect rendering, only what chunks are sent to client.")
    public static int verticalCubeLoadDistance = 8;

    @Config.LangKey("cubicchunks.config.dimension_blacklist")
    @Config.Comment("The specified dimension ID ranges won't be created as cubic chunks world for new worlds, and worlds created before this option"
            + " has been added, unless forceDimensionExcludes is set to true. IDs can be specified either as range in format min:max, or as single "
            + "numbers.\n"
            + "Example:\n"
            + "    S:excludedDimensions <\n"
            + "        1\n"
            + "        10:100\n"
            + "        101:200\n"
            + "        -5\n"
            + "     >\n"
            + "The ranges specified can overlap, and the bounds can be specified in reversed order.")
    public static String[] excludedDimensions = new String[0];

    @Config.LangKey("cubicchunks.config.force_dimension_blacklist")
    @Config.Comment("If this is set to true, cubic chunks will respect excluded dimensions even for already existing worlds. If this results in a "
            + "existing dimension switching between cubic chunks and vanilla, the contents of that dimension won't be converted.")
    public static boolean forceDimensionExcludes = false;

    @Config.LangKey("cubicchunks.config.relight_checks_per_tick_per_column")
    @Config.Comment("In an attempt to fix lighting glicthes over time, cubic chunks will keep updating light in specified amount of blocks per "
            + "column (chunk) per tick. Default value of 1 doesn't cause noticeable performance drop, but still fixes most major issues relatively "
            + "quickly.")
    public static int relightChecksPerTickPerColumn = 1;

    @Config.LangKey("cubicchunks.config.do_client_light_fixes")
    @Config.Comment("By default cubic chunks will attempt to go over all the blocks over time to fix lighting only on server. Enable this to also "
            + "fix lighting on the clientside.")
    public static boolean doClientLightFixes = false;

    public static int defaultMaxCubesPerChunkloadingTicket = 25 * 16;
    public static Map<String, Integer> modMaxCubesPerChunkloadingTicket = new HashMap<>();

    static {
        modMaxCubesPerChunkloadingTicket.put("cubicchunks", defaultMaxCubesPerChunkloadingTicket);
    }

    //@Config.Ignore
    private static TreeRangeSet<Integer> excludedDimensionsRanges = null;

    public static void sync() {
        excludedDimensionsRanges = null; // TODO: does it work? attempt to workaround no @Ignore
        ConfigManager.sync(CubicChunks.MODID, Config.Type.INSTANCE);

        initDimensionRanges();
    }

    private static void initDimensionRanges() {
        if (excludedDimensionsRanges == null) {
            excludedDimensionsRanges = TreeRangeSet.create();
        }
        excludedDimensionsRanges.clear();

        final Predicate<String> NUMBER_PATTERN = Pattern.compile("^-?\\d+$").asPredicate();
        final Predicate<String> NUMBER_RANGE_PATTERN = Pattern.compile("^-?\\d+:-?\\d+$").asPredicate();

        int i = 0;
        for (String str : excludedDimensions) {
            if (NUMBER_PATTERN.test(str)) {
                excludedDimensionsRanges.add(Range.singleton(Integer.parseInt(str)));
            } else if (NUMBER_RANGE_PATTERN.test(str)) {
                String[] parts = str.split(":");
                excludedDimensionsRanges.add(Range.closed(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
            } else {
                throw new NumberFormatException(str);
            }
            i++;
        }
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(CubicChunks.MODID)) {
            sync();
        }
    }

    public static void setVerticalViewDistance(int value) {
        verticalCubeLoadDistance = value;
        sync();
    }

    public static void flipForceCubicChunks() {
        forceCubicChunks = !forceCubicChunks;
        sync();
    }

    public static boolean isDimensionExcluded(int dimension) {
        if (excludedDimensionsRanges == null) {
            initDimensionRanges();
        }
        return excludedDimensionsRanges.contains(dimension);
    }
}
