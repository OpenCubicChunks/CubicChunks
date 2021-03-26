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
package io.github.opencubicchunks.cubicchunks.core;

import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeSet;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.ResourceLocation;
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

    @Config.Comment("Eliminates a few data copies in compatibility generator. May break some mods." +
            " Disable if you experience issues in modded dimensions or world types")
    @Config.LangKey("cubicchunks.config.optimized_compatibility_generator")
    public static boolean optimizedCompatibilityGenerator = true;


    @Config.LangKey("cubicchunks.config.force_cc")
    @Config.Comment("Determines when a cubic chunks world should be created for non-cubic-chunks world types.\n"
        + "NONE - only when cubic chunks world type\n"
        + "NEW_WORLD - only for newly created worlds\n"
        + "LOAD_NOT_EXCLUDED - load all worlds as cubic chunks, except excluded dimensions\n"
        + "ALWAYS - load everything as cubic chunks. Overrides forceDimensionExcludes")
    public static ForceCCMode forceLoadCubicChunks = ForceCCMode.NONE;

    @Config.LangKey("cubicchunks.config.cubegen_per_tick")
    @Config.Comment("The maximum number of cubic chunks to generate per tick.")
    public static int maxGeneratedCubesPerTick = 49 * 16;

    @Config.LangKey("cubicchunks.config.max_cube_generation_time_millis")
    @Config.Comment("Maximum amount of time spent on generating chunks per dimension.")
    public static int maxCubeGenerationTimeMillis = 50;

    @Config.LangKey("cubicchunks.config.use_vanilla_world_generators")
    @Config.Comment("Enabling this option will force cubic chunks to use world generators designed for two dimensional chunks, which are often used "
            + "for custom ore generators added by mods. To do so cubic chunks will pregenerate cubes in a range of height from 0 to 255. This is "
            + "very likely to break a lot of mods, cause the game to hang, and make the world generation depend on the order in which things are "
            + "generated. Use at your own risk.")
    public static boolean useVanillaChunkWorldGenerators = false;

    @Config.LangKey("cubicchunks.config.vert_view_distance")
    @Config.Comment("Similar to Minecraft's view distance, only for vertical chunks. Automatically adjusted by vertical view distance slider on the"
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
    public static String[] excludedDimensions = {"1"};

    @Config.LangKey("cubicchunks.config.force_dimension_blacklist")
    @Config.Comment("If this is set to true, cubic chunks will respect excluded dimensions even for already existing worlds. If this results in a "
            + "existing dimension switching between cubic chunks and vanilla, the contents of that dimension won't be converted.")
    public static boolean forceDimensionExcludes = false;

    @Config.LangKey("cubicchunks.config.relight_checks_per_tick_per_column")
    @Config.Comment("In an attempt to fix lighting glitches over time, cubic chunks will keep updating light in specified amount of blocks per "
            + "column (chunk) per tick. Default value of 1 doesn't cause noticeable performance drop, but still fixes most major issues relatively "
            + "quickly.")
    public static int relightChecksPerTickPerColumn = 1;

    @Config.LangKey("cubicchunks.config.do_client_light_fixes")
    @Config.Comment("By default cubic chunks will attempt to go over all the blocks over time to fix lighting only on server. Enable this to also "
            + "fix lighting on the clientside.")
    public static boolean doClientLightFixes = false;

    @Config.LangKey("cubicchunks.config.biome_temperature_center_y")
    @Config.Comment("Heights below this value will have normal, unmodified biome temperature")
    public static int biomeTemperatureCenterY = 64;

    @Config.LangKey("cubicchunks.config.biome_temperature_y_factor")
    @Config.Comment("How much should biome temperature increase with height (negative values decrease temperature)")
    public static float biomeTemperatureHeightFactor = -0.05F / 30.0F;

    @Config.LangKey("cubicchunks.config.biome_temperature_scale_max_y")
    @Config.Comment("Above this height, biome temperature will no longer change")
    public static int biomeTemperatureScaleMaxY = 256;

    @Config.LangKey("cubicchunks.config.compatibility_generator_type")
    @Config.Comment("Vanilla compatibility generator type, which will convert vanilla world type generators output in cubic")
    public static String compatibilityGeneratorType = "cubicchunks:default";

    @Config.LangKey("cubicchunks.config.storage_format")
    @Config.Comment("The storage format. Note: this will be used for all newly created worlds. Existing worlds will continue to use the format they were created with.\n"
                    + "If empty, the storage format for new worlds will be determined automatically.")
    public static String storageFormat = "";

    @Config.LangKey("cubicchunks.config.spawn_generate_distance_horizontal")
    @Config.Comment("Horizontal distance for initially generated spawn area")
    @Config.RequiresWorldRestart
    public static int spawnGenerateDistanceXZ = 12;

    @Config.LangKey("cubicchunks.config.spawn_generate_distance_vertical")
    @Config.Comment("Vertical distance for initially generated spawn area")
    @Config.RequiresWorldRestart
    public static int spawnGenerateDistanceY = 8;

    @Config.LangKey("cubicchunks.config.spawn_forceload_distance_horizontal")
    @Config.Comment("Horizontal distance for spawn chunks kept loaded in memory")
    @Config.RequiresWorldRestart
    public static int spawnLoadDistanceXZ = 8;

    @Config.LangKey("cubicchunks.config.spawn_forceload_distance_vertical")
    @Config.Comment("Vertical distance for spawn chunks kept loaded in memory")
    @Config.RequiresWorldRestart
    public static int spawnLoadDistanceY = 8;

    @Config.LangKey("cubicchunks.config.default_min_height")
    @Config.Comment("World min height. Values that are not an integer multiple of 16 may cause unintended behavior")
    @Config.RangeInt(min = CubicChunks.MIN_SUPPORTED_BLOCK_Y, max = 0)
    public static int defaultMinHeight = -(1 << 30);

    @Config.LangKey("cubicchunks.config.default_max_height")
    @Config.Comment("World max height. Values that are not an integer multiple of 16 may cause unintended behavior")
    @Config.RangeInt(min = 16, max = CubicChunks.MAX_SUPPORTED_BLOCK_Y)
    public static int defaultMaxHeight = 1 << 30;

    @Config.LangKey("cubicchunks.config.replace_light_recheck")
    @Config.Comment("Replaces vanilla light check code with cubic chunks code for cubic chunks worlds.\n"
            + "Cubic chunks version keeps track of light changes on the server and sends them to client\n"
            + "and handles the edge of the world by scheduling chunk edge updates instead of failing.")
    public static boolean replaceLightRecheck = false;

    @Config.LangKey("cubicchunks.config.update_known_broken_lighting_on_load")
    @Config.Comment("Attempts to detect worlds saved with cubic chunks versions with lighting glitches, and fix them on world load.")
    public static boolean updateKnownBrokenLightingOnLoad = true;

    @Config.LangKey("cubicchunks.config.worldgen_watchdog_time_limit")
    @Config.Comment("Maximum amount of time (milliseconds) generating a single chunk can take in vanilla compatibility generator before forcing a "
            + "crash.")
    public static int worldgenWatchdogTimeLimit = 10000;

    @Config.LangKey("cubicchunks.config.allow_vanilla_clients")
    @Config.Comment("Allows clients without cubic chunks to join. "
            + "THIS IS INTENDED FOR VANILLA CLIENTS. "
            + "This is VERY likely to break when used with other mods")
    public static boolean allowVanillaClients = false;

    @Config.LangKey("cubicchunks.config.fast_simplified_sky_light")
    @Config.Comment("Forces an MC-classic-like skylight propagation algorithm. It's much faster and doesn't look too bad. "
            + "You can enable it if you don't need normal skylight values but want extra performance for worldgen and block updates")
    public static boolean fastSimplifiedSkyLight = false;

    @Config.LangKey("cubicchunks.config.cubes_to_send_per_tick")
    @Config.Comment("Max amount of cubes sent to client per tick to players")
    public static int cubesToSendPerTick = 81 * 8 + 1;

    @Config.LangKey("cubicchunks.config.vanilla_clients")
    @Config.Comment("Options relating to support for vanilla clients.")
    public static VanillaClients vanillaClients = new VanillaClients();

    @Config.LangKey("cubicchunks.config.use_shadow_paging_io")
    @Config.Comment("Whether cubic chunks save format IO should use shadow paging. This may be slightly slower and use "
            + "a bit more storage but should significantly improve reliability in case of improper shutdown.")
    @Config.RequiresWorldRestart
    public static boolean useShadowPagingIO = true;

    public static final class VanillaClients {
        @Config.LangKey("cubicchunks.config.vanilla_clients.horizontal_slices")
        @Config.Comment("Enables horizontal slices for vanilla clients. "
                        + "This will cause coordinates to wrap around on the X and Z axes in the same way as on Y.")
        public boolean horizontalSlices = true;

        @Config.LangKey("cubicchunks.config.vanilla_clients.horizontal_slices_bedrock_only")
        @Config.Comment("If horizontal slices is enabled, restricts horizontal slices to Bedrock edition clients.\n"
                        + "Note that Bedrock clients are not supported directly, but only when connecting through a proxy such as Geyser.")
        public boolean horizontalSlicesBedrockOnly = true;

        @Config.LangKey("cubicchunks.config.vanilla_clients.horizontal_slice_size")
        @Config.Comment("The size (radius) of a horizontal slice.")
        public int horizontalSliceSize = 65536;
    }

    public static int defaultMaxCubesPerChunkloadingTicket = 25 * 16;
    public static Map<String, Integer> modMaxCubesPerChunkloadingTicket = new HashMap<>();

    static {
        modMaxCubesPerChunkloadingTicket.put("cubicchunks", defaultMaxCubesPerChunkloadingTicket);
    }

    @Config.Ignore
    private static TreeRangeSet<Integer> excludedDimensionsRanges = null;

    public static void sync() {
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

    public static void disableCubicChunks() {
        forceLoadCubicChunks = ForceCCMode.NONE;
        sync();
    }
    
    public static void setGenerator(ResourceLocation generatorTypeIn) {
        if(forceLoadCubicChunks == ForceCCMode.NONE)
            forceLoadCubicChunks = ForceCCMode.NEW_WORLD;
        compatibilityGeneratorType = generatorTypeIn.toString();
        sync();
    }

    public static boolean isDimensionExcluded(int dimension) {
        if (excludedDimensionsRanges == null) {
            initDimensionRanges();
        }
        return excludedDimensionsRanges.contains(dimension);
    }

    public enum ForceCCMode {
        NONE,
        NEW_WORLD,
        LOAD_NOT_EXCLUDED,
        ALWAYS
    }
}
