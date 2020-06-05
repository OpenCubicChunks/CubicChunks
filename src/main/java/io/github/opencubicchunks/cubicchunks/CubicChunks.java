package io.github.opencubicchunks.cubicchunks;

import io.github.opencubicchunks.cubicchunks.meta.EarlyConfig;
import io.github.opencubicchunks.cubicchunks.misc.TestWorldType;
import io.github.opencubicchunks.cubicchunks.network.PacketDispatcher;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Requires Mixin BootStrap in order to use in forge.
 */
// The value here should match an entry in the META-INF/mods.toml file
@Mod(CubicChunks.MODID)
public class CubicChunks {
    public static long SECTIONPOS_SENTINEL = -1;

    // Directly reference a log4j logger.
    public static int worldMAXHeight = 512;

    public static final String MODID = "cubicchunks";
    public static final Logger LOGGER = LogManager.getLogger();
    public static WorldType CUBIC = new TestWorldType();

    public static final String PROTOCOL_VERSION = "0";

    public CubicChunks() {
        EarlyConfig.getCubeDiameter();
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    //Clear ALL Generation stages to prevent the Y Height Crashes
    private void setup(final FMLCommonSetupEvent event) {
        PacketDispatcher.register();

        for (Biome biome : ForgeRegistries.BIOMES) {
            for (GenerationStage.Decoration stage : GenerationStage.Decoration.values()) {
                biome.getFeatures(stage).clear();
            }
        }
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        // do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
}
