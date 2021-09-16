package io.github.opencubicchunks.cubicchunks;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import io.github.opencubicchunks.cubicchunks.config.EarlyConfig;
import io.github.opencubicchunks.cubicchunks.levelgen.biome.StripedBiomeSource;
import io.github.opencubicchunks.cubicchunks.levelgen.feature.CubicFeatures;
import io.github.opencubicchunks.cubicchunks.levelgen.placement.CubicFeatureDecorators;
import io.github.opencubicchunks.cubicchunks.network.PacketDispatcher;
import io.github.opencubicchunks.cubicchunks.server.level.CubeMap;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import net.fabricmc.api.ModInitializer;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Requires Mixin BootStrap in order to use in forge.
 */
// The value here should match an entry in the META-INF/mods.toml file
public class CubicChunks implements ModInitializer {

    // TODO: debug and fix optimized cubeload
    public static final boolean OPTIMIZED_CUBELOAD = false;

    public static final long SECTIONPOS_SENTINEL = -1;

    public static final int MAX_SUPPORTED_HEIGHT = Integer.MAX_VALUE / 2;
    public static final int MIN_SUPPORTED_HEIGHT = -MAX_SUPPORTED_HEIGHT;
    public static final int SEA_LEVEL = 64;

    public static final String MODID = "cubicchunks";
    public static final Logger LOGGER = LogManager.getLogger();

    public static final String PROTOCOL_VERSION = "0";

    public static final Map<String, CubicLevelHeightAccessor.WorldStyle> DIMENSION_TO_WORLD_STYLE = Util.make(new HashMap<>(), (set) -> {
        set.put("minecraft:overworld", CubicLevelHeightAccessor.WorldStyle.CUBIC);
        set.put("minecraft:the_nether", CubicLevelHeightAccessor.WorldStyle.CUBIC);
        set.put("minecraft:the_end", CubicLevelHeightAccessor.WorldStyle.CHUNK);
    });

    private static final Config CONFIG = new Config();

    public CubicChunks() {
        if (!(CubeMap.class.isAssignableFrom(ChunkMap.class))) {
            throw new IllegalStateException("Mixin not applied!");
        }
        EarlyConfig.getDiameterInSections();

        if (System.getProperty("cubicchunks.debug", "false").equalsIgnoreCase("true")) {
            try {
                Class.forName("io.github.opencubicchunks.cubicchunks.debug.DebugVisualization").getMethod("enable").invoke(null);
                SharedConstants.IS_RUNNING_IN_IDE = true;
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
                LOGGER.catching(e);
            }
        }

        //Custom CC Features
        CubicFeatureDecorators.init();
        CubicFeatures.init();
    }

    public static Config config() {
        return CONFIG;
    }

    @Override
    public void onInitialize() {
        PacketDispatcher.register();

        Registry.register(Registry.BIOME_SOURCE, new ResourceLocation(MODID, "stripes"), StripedBiomeSource.CODEC);
//        Registry.register(Registry.CHUNK_GENERATOR, new ResourceLocation(MODID, "generator"), CCNoiseBasedChunkGenerator.CODEC);
    }

    //TODO: Implement a file for this.
    public static class Config {
        public Client client = new Client();

        public void markDirty() {
        }

        public static class Client {
            public int verticalViewDistance = 8;
        }
    }
}