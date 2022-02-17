/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2021 OpenCubicChunks
 *  Copyright (c) 2015-2021 contributors
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

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeSet;
import io.github.opencubicchunks.cubicchunks.api.world.storage.StorageFormatProviderBase;
import io.github.opencubicchunks.cubicchunks.api.worldgen.VanillaCompatibilityGeneratorProviderBase;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.server.command.CommandTreeBase;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
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
            + "column (chunk) per tick. This option shouldn't be necessary but may be useful for old worlds where lighting is broken or when "
            + "lighting bugs are encountered.")
    public static int relightChecksPerTickPerColumn = 0;

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
    public static int spawnGenerateDistanceXZ = 12;

    @Config.LangKey("cubicchunks.config.spawn_generate_distance_vertical")
    @Config.Comment("Vertical distance for initially generated spawn area")
    public static int spawnGenerateDistanceY = 8;

    @Config.LangKey("cubicchunks.config.spawn_forceload_distance_horizontal")
    @Config.Comment("Horizontal distance for spawn chunks kept loaded in memory")
    public static int spawnLoadDistanceXZ = 8;

    @Config.LangKey("cubicchunks.config.spawn_forceload_distance_vertical")
    @Config.Comment("Vertical distance for spawn chunks kept loaded in memory")
    public static int spawnLoadDistanceY = 8;

    @Config.LangKey("cubicchunks.config.default_min_height")
    @Config.Comment("World min height. Values that are not an integer multiple of 16 may cause unintended behavior")
    @Config.RangeInt(min = CubicChunks.MIN_SUPPORTED_BLOCK_Y, max = 0)
    public static int defaultMinHeight = -(1 << 30);

    @Config.LangKey("cubicchunks.config.default_max_height")
    @Config.Comment("World max height. Values that are not an integer multiple of 16 may cause unintended behavior")
    @Config.RangeInt(min = 16, max = CubicChunks.MAX_SUPPORTED_BLOCK_Y)
    public static int defaultMaxHeight = 1 << 30;

    @Config.LangKey("cubicchunks.config.worldgen_watchdog_time_limit")
    @Config.Comment("Maximum amount of time (milliseconds) generating a single chunk can take in vanilla compatibility generator before forcing a "
            + "crash.")
    public static int worldgenWatchdogTimeLimit = 10000;

    @Config.LangKey("cubicchunks.config.allow_vanilla_clients")
    @Config.Comment("Allows clients without cubic chunks to join. "
            + "THIS IS INTENDED FOR VANILLA CLIENTS. "
            + "This is VERY likely to break when used with other mods")
    public static boolean allowVanillaClients = false;

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

    @Config.LangKey("cubicchunks.config.ignore_corrupted_chunks")
    @Config.Comment("Ignores and regenerates corrupted chunks instead of crashing the server")
    public static boolean ignoreCorruptedChunks = false;

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
        validateConfigValues();
        ConfigManager.sync(CubicChunks.MODID, Config.Type.INSTANCE);
    }

    private static void validateConfigValues() {
        if (!VanillaCompatibilityGeneratorProviderBase.REGISTRY.containsKey(new ResourceLocation(compatibilityGeneratorType))) {
            CubicChunks.LOGGER.error("Compatibility generator type {} doesn't exist, resetting config to default", compatibilityGeneratorType);
            compatibilityGeneratorType = VanillaCompatibilityGeneratorProviderBase.DEFAULT.toString();
        }
        if (!storageFormat.isEmpty() && !StorageFormatProviderBase.REGISTRY.containsKey(new ResourceLocation(storageFormat))) {
            CubicChunks.LOGGER.error("Storage format {} doesn't exist, resetting config to default", storageFormat);
            storageFormat = "";
        }
    }

    private static void initDimensionRanges() {
        if (excludedDimensionsRanges == null) {
            excludedDimensionsRanges = TreeRangeSet.create();
        }
        excludedDimensionsRanges.clear();

        final Predicate<String> NUMBER_PATTERN = Pattern.compile("^-?\\d+$").asPredicate();
        final Predicate<String> NUMBER_RANGE_PATTERN = Pattern.compile("^-?\\d+:-?\\d+$").asPredicate();

        for (String str : excludedDimensions) {
            try {
                parseRange(NUMBER_PATTERN, NUMBER_RANGE_PATTERN, str);
            } catch (NumberFormatException ex) {
                CubicChunks.LOGGER.error("Error parsing excluded dimension ranges: " + str + " is not a valid range, ignoring");
            }
        }
        Set<Range<Integer>> ranges = excludedDimensionsRanges.asRanges();
        excludedDimensions = new String[ranges.size()];
        int i = 0;
        for (Range<Integer> range : ranges) {
            int min = range.lowerBoundType() == BoundType.CLOSED ? range.lowerEndpoint() : range.lowerEndpoint() + 1;
            int max = range.upperBoundType() == BoundType.CLOSED ? range.upperEndpoint() : range.upperEndpoint() - 1;
            excludedDimensions[i] = min == max ? String.valueOf(min) : (min + ":" + max);
            i++;
        }
    }

    private static void parseRange(Predicate<String> NUMBER_PATTERN, Predicate<String> NUMBER_RANGE_PATTERN, String str) {
        // add ranges as open where possible to allow merging
        if (NUMBER_PATTERN.test(str)) {
            int value = Integer.parseInt(str);
            if (value == Integer.MIN_VALUE || value == Integer.MAX_VALUE) {
                excludedDimensionsRanges.add(Range.singleton(value));
            } else {
                excludedDimensionsRanges.add(Range.open(value - 1, value + 1));
            }
        } else if (NUMBER_RANGE_PATTERN.test(str)) {
            String[] parts = str.split(":");
            int lower = Integer.parseInt(parts[0]);
            int upper = Integer.parseInt(parts[1]);
            if (lower == upper) {
                if (lower == Integer.MIN_VALUE || lower == Integer.MAX_VALUE) {
                    excludedDimensionsRanges.add(Range.singleton(lower));
                } else {
                    excludedDimensionsRanges.add(Range.open(lower - 1, lower + 2));
                }
            } else {
                if (lower == Integer.MIN_VALUE && upper == Integer.MAX_VALUE) {
                    excludedDimensionsRanges.add(Range.closed(lower, upper));
                } else if (lower == Integer.MIN_VALUE) {
                    excludedDimensionsRanges.add(Range.closedOpen(lower, upper + 1));
                } else if (upper == Integer.MAX_VALUE) {
                    excludedDimensionsRanges.add(Range.openClosed(lower - 1, upper));
                } else {
                    excludedDimensionsRanges.add(Range.open(lower - 1, upper + 1));
                }
            }
        } else {
            CubicChunks.LOGGER.error("Error parsing excluded dimension ranges: " + str + " is not a valid range, ignoring");
        }
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(CubicChunks.MODID)) {
            sync();
        }
    }

    public static void registerCommands(FMLServerStartingEvent evt) {
        PermissionAPI.registerNode("cubicchunks.command.set_config", DefaultPermissionLevel.OP, "Allows changing cubic chunks config options");
        PermissionAPI.registerNode("cubicchunks.command.reload_config", DefaultPermissionLevel.OP, "Allows reloading cubic chunks config");
        evt.registerServerCommand(new BaseCubicChunksCommand());
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

    static class BaseCubicChunksCommand extends CommandTreeBase {

        public BaseCubicChunksCommand() {
            addSubcommand(new ConfigCommand());
        }

        @Override public String getName() {
            return "cubicchunks";
        }

        @Override public String getUsage(ICommandSender sender) {
            return "cubicchunks.command.usage.cubicchunks";
        }

    }
    static class ConfigCommand extends CommandTreeBase {

        public ConfigCommand() {
            addSubcommand(new ReloadConfig());
            addSubcommand(new SetConfigBase());
        }

        @Override public String getName() {
            return "config";
        }

        @Override public String getUsage(ICommandSender sender) {
            return "cubicchunks.command.usage.config";
        }
    }

    static class SetConfigBase extends CommandTreeBase {

        public SetConfigBase() {
            Field[] fields = CubicChunksConfig.class.getFields();
            try {
                registerConfigCommandsFor(null, fields, "");
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }

        private void registerConfigCommandsFor(@Nullable Object object, Field[] fields, String prefix) throws ReflectiveOperationException {
            for (Field field : fields) {
                if (field.getAnnotationsByType(Config.Ignore.class).length != 0) {
                    continue;
                }
                boolean requiresWorldRestart = field.getAnnotationsByType(Config.RequiresWorldRestart.class).length != 0;
                String name = prefix + field.getName();
                Class<?> type = field.getType();
                boolean isSimpleType = type.isPrimitive() || type == String.class;
                boolean isCollection = type.isArray() || Map.class.isAssignableFrom(type) || List.class.isAssignableFrom(type);
                boolean isEnum = type.isEnum();
                if (!isSimpleType && !isCollection && !isEnum) {
                    registerConfigCommandsFor(field.get(object), type.getFields(), name + ".");
                    continue;
                }
                addSubcommand(new SetConfig(name, object, field, requiresWorldRestart));
            }
        }

        @Override public String getName() {
            return "set";
        }

        @Override public String getUsage(ICommandSender sender) {
            return "cubicchunks.command.usage.config.set";
        }

        @Override
        public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
            if (sender instanceof EntityPlayer) {
                return PermissionAPI.hasPermission((EntityPlayer) sender, "cubicchunks.command.set_config");
            } else {
                return super.checkPermission(server, sender);
            }
        }
    }
    static class SetConfig extends CommandBase {

        private static final Set<String> TRUE_STRINGS = new HashSet<>(Arrays.asList("true", "1", "yes", "on", "enable", "enabled"));
        private static final Set<String> FALSE_STRINGS = new HashSet<>(Arrays.asList("false", "0", "no", "off", "disable", "disabled"));

        private final String name;
        private final Object object;
        private final Field field;
        private final boolean requiresWorldRestart;

        public SetConfig(String name, @Nullable Object object, Field field, boolean requiresWorldRestart) {
            this.name = name;
            this.object = object;
            this.field = field;
            this.requiresWorldRestart = requiresWorldRestart;
        }

        @Override public String getName() {
            return name;
        }

        @SuppressWarnings("deprecation")
        @Override public String getUsage(ICommandSender sender) {
            if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
                return I18n.format("cubicchunks.command.usage.config.set_option", name);
            } else {
                //we have to use this on the dedicated server, as the client I18n class isn't available there...
                // unfortunately this could result in a client being sent a string in a language other than their configured locale, but i don't
                // see any alternative other than adding separate translation keys for every single config option
                return net.minecraft.util.text.translation.I18n.translateToLocalFormatted("cubicchunks.command.usage.config.set_option", name);
            }
        }

        @Override
        public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
            if (sender instanceof EntityPlayer) {
                return PermissionAPI.hasPermission((EntityPlayer) sender, "cubicchunks.command.set_config");
            } else {
                return super.checkPermission(server, sender);
            }
        }

        @Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            Object newValue = parseConfigValue(args);
            try {
                field.set(object, newValue);
                CubicChunksConfig.sync();
                sender.sendMessage(new TextComponentTranslation("cubicchunks.command.config.set.done", name, stringify(field.get(object))));
                if (requiresWorldRestart) {
                    sender.sendMessage(new TextComponentTranslation("cubicchunks.command.config.set.requires_restart", name));
                }
            } catch (ReflectiveOperationException e) {
                throw new CommandException("cubicchunks.command.config.set.failed", name);
            }
        }

        private String stringify(@Nullable Object o) {
            if (o == null) {
                return "null";
            }
            if (o.getClass().isPrimitive() || o.getClass().isEnum() || o instanceof String || o instanceof Collection) {
                return o.toString();
            }
            if (o instanceof int[]) {
                return Arrays.toString((int[]) o);
            }
            if (o instanceof String[]) {
                return Arrays.deepToString((Object[]) o);
            }
            throw new IllegalArgumentException(o.toString());
        }

        private Object parseConfigValue(String[] args) throws CommandException {
            Class<?> type = field.getType();
            if (type == String.class) {
                return String.join(" ", args);
            }
            if ((type.isPrimitive() || type.isEnum()) && args.length != 1) {
                throw new WrongUsageException("cubicchunks.command.usage.config.set.primitive", name, type.getSimpleName());
            }
            Object o = tryParseSimpleType(type, args[0]);
            if (o != null) {
                return o;
            }
            if (type == int[].class) {
                int[] value = new int[args.length];
                for (int i = 0; i < args.length; i++) {
                    value[i] = parseInt(args[i]);
                }
                return value;
            }
            if (type == String[].class) {
                return args;
            }
            if (type == Map.class) {
                Type genericType = field.getGenericType();
                if (!(genericType instanceof ParameterizedType)) {
                    throw new IllegalStateException("Field " + field.getDeclaringClass() + "." + field.getName() + " appears to be a raw type Map!");
                }
                Type[] actualTypeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
                Type key = actualTypeArguments[0];
                if (!(key instanceof Class)) {
                    throw new IllegalStateException("Field " + field.getDeclaringClass() + "." + field.getName() +
                            " key generic type is not a class!");
                }
                Type value = actualTypeArguments[1];
                if (!(value instanceof Class)) {
                    throw new IllegalStateException("Field " + field.getDeclaringClass() + "." + field.getName() +
                            " key generic type is not a class!");
                }
                Map<Object, Object> map = new HashMap<>();
                for (String arg : args) {
                    String[] split = arg.split("=");
                    Object k = tryParseSimpleType((Class<?>) key, split[0]);
                    Object v = tryParseSimpleType((Class<?>) value, split[1]);
                    map.put(k, v);
                }
                return map;
            }
            throw new IllegalStateException("Unsupported field type " + field);
        }

        @SuppressWarnings({"rawtypes", "unchecked"}) @Nullable
        private Object tryParseSimpleType(Class<?> type, String value) throws CommandException {
            if (type == String.class) {
                return value;
            }
            if (type == int.class) {
                return parseInt(value);
            }
            if (type == boolean.class) {
                return parseBool(value);
            }
            if (type == float.class) {
                return (float) parseDouble(value);
            }
            if (type == double.class) {
                return parseDouble(value);
            }
            if (type.isEnum()) {
                return Enum.valueOf((Class) type, value);
            }
            return null;
        }

        private boolean parseBool(String value) throws CommandException {
            value = value.toLowerCase(Locale.ROOT);
            if (TRUE_STRINGS.contains(value)) {
                return true;
            }
            if (FALSE_STRINGS.contains(value)) {
                return false;
            }
            throw new CommandException("commands.generic.boolean.invalid", value);
        }
    }

    static class ReloadConfig extends CommandBase {

        @Override
        public String getName() {
            return "reload";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "cubicchunks.command.usage.config.reload";
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            CubicChunksConfig.sync();
            sender.sendMessage(new TextComponentTranslation("cubicchunks.command.config.reload.done"));
        }

        @Override
        public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
            if (sender instanceof EntityPlayer) {
                return PermissionAPI.hasPermission((EntityPlayer) sender, "cubicchunks.command.reload_config");
            } else {
                return super.checkPermission(server, sender);
            }
        }
    }
}
