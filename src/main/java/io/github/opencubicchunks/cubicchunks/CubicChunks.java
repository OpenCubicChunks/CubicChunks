package io.github.opencubicchunks.cubicchunks;

import io.github.opencubicchunks.cubicchunks.chunk.IChunkManager;
import io.github.opencubicchunks.cubicchunks.meta.EarlyConfig;
import io.github.opencubicchunks.cubicchunks.network.PacketDispatcher;
import io.github.opencubicchunks.cubicchunks.world.biome.StripedBiomeSource;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.biome.Biome;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;

/**
 * Requires Mixin BootStrap in order to use in forge.
 */
// The value here should match an entry in the META-INF/mods.toml file
public class CubicChunks implements ModInitializer {

    // TODO: debug and fix optimized cubeload
    public static final boolean OPTIMIZED_CUBELOAD = false;

    public static long SECTIONPOS_SENTINEL = -1;

    public static int MAX_SUPPORTED_HEIGHT = Integer.MAX_VALUE / 2;
    public static int MIN_SUPPORTED_HEIGHT = -MAX_SUPPORTED_HEIGHT;

    public static final String MODID = "cubicchunks";
    public static final Logger LOGGER = LogManager.getLogger();

    public static final String PROTOCOL_VERSION = "0";

    public CubicChunks() {
        if (!(IChunkManager.class.isAssignableFrom(ChunkMap.class))) {
            throw new IllegalStateException("Mixin not applied!");
        }
        EarlyConfig.getDiameterInSections();

        if (System.getProperty("cubicchunks.debug", "false").equalsIgnoreCase("true")) {
            try {
                Class.forName("io.github.opencubicchunks.cubicchunks.debug.DebugVisualization").getMethod("enable").invoke(null);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
                LOGGER.catching(e);
            }
        }
    }

    private static void convertImmutableFeatures(Biome biome) {
        //if (biome.getGenerationSettings().features instanceof ImmutableList) {
        //    biome.getGenerationSettings().features =biome.getGenerationSettings().features.stream().map(Lists::newArrayList).collect(Collectors.toList());
        //}
    }

    @Override
    public void onInitialize() {
        PacketDispatcher.register();

        Registry.register(Registry.BIOME_SOURCE, new ResourceLocation(MODID, "stripes"), StripedBiomeSource.CODEC);
    }
}