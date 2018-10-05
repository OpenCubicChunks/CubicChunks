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
package cubicchunks;

import static cubicchunks.api.worldgen.biome.CubicBiome.oceanWaterReplacer;
import static cubicchunks.api.worldgen.biome.CubicBiome.terrainShapeReplacer;

import cubicchunks.api.worldgen.biome.CubicBiome;
import cubicchunks.debug.DebugWorldType;
import cubicchunks.network.PacketDispatcher;
import cubicchunks.proxy.CommonProxy;
import cubicchunks.world.type.CustomCubicWorldType;
import cubicchunks.world.type.FlatCubicWorldType;
import cubicchunks.world.type.VanillaCubicWorldType;
import cubicchunks.worldgen.generator.CubeGeneratorsRegistry;
import cubicchunks.worldgen.generator.custom.ConversionUtils;
import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings;
import cubicchunks.worldgen.generator.custom.biome.replacer.MesaSurfaceReplacer;
import cubicchunks.worldgen.generator.custom.biome.replacer.MutatedSavannaSurfaceReplacer;
import cubicchunks.worldgen.generator.custom.biome.replacer.SwampWaterWithLilypadReplacer;
import cubicchunks.worldgen.generator.custom.biome.replacer.TaigaSurfaceReplacer;
import cubicchunks.worldgen.generator.custom.populator.DefaultDecorator;
import cubicchunks.worldgen.generator.custom.populator.DesertDecorator;
import cubicchunks.worldgen.generator.custom.populator.ForestDecorator;
import cubicchunks.worldgen.generator.custom.populator.JungleDecorator;
import cubicchunks.worldgen.generator.custom.populator.PlainsDecorator;
import cubicchunks.worldgen.generator.custom.populator.SavannaDecorator;
import cubicchunks.worldgen.generator.custom.populator.SnowBiomeDecorator;
import cubicchunks.worldgen.generator.custom.populator.SwampDecorator;
import cubicchunks.worldgen.generator.custom.populator.TaigaDecorator;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeBeach;
import net.minecraft.world.biome.BiomeDesert;
import net.minecraft.world.biome.BiomeForest;
import net.minecraft.world.biome.BiomeForestMutated;
import net.minecraft.world.biome.BiomeHills;
import net.minecraft.world.biome.BiomeJungle;
import net.minecraft.world.biome.BiomeMesa;
import net.minecraft.world.biome.BiomeMushroomIsland;
import net.minecraft.world.biome.BiomeOcean;
import net.minecraft.world.biome.BiomePlains;
import net.minecraft.world.biome.BiomeRiver;
import net.minecraft.world.biome.BiomeSavanna;
import net.minecraft.world.biome.BiomeSavannaMutated;
import net.minecraft.world.biome.BiomeSnow;
import net.minecraft.world.biome.BiomeStoneBeach;
import net.minecraft.world.biome.BiomeSwamp;
import net.minecraft.world.biome.BiomeTaiga;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;
import net.minecraftforge.fml.common.versioning.InvalidVersionSpecificationException;
import net.minecraftforge.fml.common.versioning.VersionRange;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mod(modid = CubicChunks.MODID,
        name = "CubicChunks",
        version = CubicChunks.VERSION)
@Mod.EventBusSubscriber
public class CubicChunks {

    public static final int FIXER_VERSION = 1;

    public static final VersionRange SUPPORTED_SERVER_VERSIONS;
    public static final VersionRange SUPPORTED_CLIENT_VERSIONS;

    static {
        try {
            // currently no known unsupported version. Versions newer than current will be only checked on the other side
            // (I know this can be hard to actually fully understand)
            SUPPORTED_SERVER_VERSIONS = VersionRange.createFromVersionSpec("*");
            SUPPORTED_CLIENT_VERSIONS = VersionRange.createFromVersionSpec("*");
        } catch (InvalidVersionSpecificationException e) {
            throw new Error(e);
        }
    }

    public static final int MIN_BLOCK_Y = Integer.MIN_VALUE >> 1;
    public static final int MAX_BLOCK_Y = Integer.MAX_VALUE >> 1;

    public static final boolean DEBUG_ENABLED = System.getProperty("cubicchunks.debug", "false").equalsIgnoreCase("true");
    public static final String MODID = "cubicchunks";
    public static final String VERSION = "@@VERSION@@";
    public static final String MALISIS_VERSION = "@@MALISIS_VERSION@@";

    @Nonnull
    public static Logger LOGGER = LogManager.getLogger("EarlyCubicChunks");//use some logger even before it's set. useful for unit tests

    @SidedProxy(clientSide = "cubicchunks.proxy.ClientProxy", serverSide = "cubicchunks.proxy.ServerProxy")
    public static CommonProxy proxy;

    @SubscribeEvent
    public static void registerRegistries(RegistryEvent.NewRegistry evt) {
        CubicBiome.init();
    }

    @SubscribeEvent
    public static void registerCubicBiomes(RegistryEvent.Register<CubicBiome> event) {
        // Vanilla biomes are initialized during bootstrap which happens before registration events
        // so it should be safe to use them here
        autoRegister(event, Biome.class, b -> b
                .addDefaultBlockReplacers()
                .defaultDecorators());
        autoRegister(event, BiomeBeach.class, b -> b
                .addDefaultBlockReplacers()
                .defaultDecorators());
        autoRegister(event, BiomeDesert.class, b -> b
                .addDefaultBlockReplacers()
                .defaultDecorators().decorator(new DesertDecorator()));
        autoRegister(event, BiomeForest.class, b -> b
                .addDefaultBlockReplacers()
                .decorator(new ForestDecorator()).defaultDecorators());
        autoRegister(event, BiomeForestMutated.class, b -> b
                .addDefaultBlockReplacers()
                .decorator(new ForestDecorator()).defaultDecorators());
        autoRegister(event, BiomeHills.class, b -> b
                .addDefaultBlockReplacers()
                .defaultDecorators());
        autoRegister(event, BiomeJungle.class, b -> b
                .addDefaultBlockReplacers()
                .defaultDecorators().decorator(new JungleDecorator()));
        autoRegister(event, BiomeMesa.class, b -> b
                .addBlockReplacer(terrainShapeReplacer()).addBlockReplacer(MesaSurfaceReplacer.provider()).addBlockReplacer(oceanWaterReplacer())
                .decoratorProvider(DefaultDecorator.Ores::new).decoratorProvider(DefaultDecorator::new));
        autoRegister(event, BiomeMushroomIsland.class, b -> b
                .addDefaultBlockReplacers()
                .defaultDecorators());
        autoRegister(event, BiomeOcean.class, b -> b
                .addDefaultBlockReplacers()
                .defaultDecorators());
        autoRegister(event, BiomePlains.class, b -> b
                .addDefaultBlockReplacers()
                .decorator(new PlainsDecorator()).defaultDecorators());
        autoRegister(event, BiomeRiver.class, b -> b
                .addDefaultBlockReplacers()
                .defaultDecorators());
        autoRegister(event, BiomeSavanna.class, b -> b
                .addDefaultBlockReplacers()
                .decorator(new SavannaDecorator()).defaultDecorators());
        autoRegister(event, BiomeSavannaMutated.class, b -> b
                .addBlockReplacer(terrainShapeReplacer()).addBlockReplacer(MutatedSavannaSurfaceReplacer.provider()).addBlockReplacer(oceanWaterReplacer())
                .defaultDecorators());
        autoRegister(event, BiomeSnow.class, b -> b
                .addDefaultBlockReplacers()
                .decorator(new SnowBiomeDecorator()).defaultDecorators());
        autoRegister(event, BiomeStoneBeach.class, b -> b
                .addDefaultBlockReplacers()
                .defaultDecorators());
        autoRegister(event, BiomeSwamp.class, b -> b
                .addDefaultBlockReplacers().addBlockReplacer(SwampWaterWithLilypadReplacer.provider())
                .defaultDecorators().decorator(new SwampDecorator()));
        autoRegister(event, BiomeTaiga.class, b -> b
                .addBlockReplacer(terrainShapeReplacer()).addBlockReplacer(TaigaSurfaceReplacer.provider()).addBlockReplacer(oceanWaterReplacer())
                .decorator(new TaigaDecorator()).defaultDecorators());

    }

    private static void autoRegister(RegistryEvent.Register<CubicBiome> event, Class<? extends Biome> cl, Consumer<CubicBiome.Builder> cons) {
        ForgeRegistries.BIOMES.getValues().stream()
                .filter(x -> x.getRegistryName().getResourceDomain().equals("minecraft"))
                .filter(x -> x.getClass() == cl).forEach(b -> {
            CubicBiome.Builder builder = CubicBiome.createForBiome(b);
            cons.accept(builder);
            CubicBiome biome = builder.defaultPostDecorators().setRegistryName(MODID, b.getRegistryName().getResourcePath()).create();
            event.getRegistry().register(biome);
        });
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        LOGGER = e.getModLog();
        ConversionUtils.initFlowNoiseHack();

        CCFixType.addFixableWorldType(VanillaCubicWorldType.create());
        CCFixType.addFixableWorldType(FlatCubicWorldType.create());
        CCFixType.addFixableWorldType(CustomCubicWorldType.create());
        CCFixType.addFixableWorldType(DebugWorldType.create());
        LOGGER.debug("Registered world types");

        CCFixType.registerWalkers();
        CustomGeneratorSettings.registerDataFixers();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        proxy.init();

        PacketDispatcher.registerPackets();
        CubeGeneratorsRegistry.computeSortedGeneratorList();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        CubicBiome.postInit();
    }

    @EventHandler
    public void onServerAboutToStart(FMLServerAboutToStartEvent event) {
        proxy.setBuildLimit(event.getServer());
    }

    @NetworkCheckHandler
    public static boolean checkCanConnectWithMods(Map<String, String> modVersions, Side remoteSide) {
        String remoteFullVersion = modVersions.get(MODID);
        if (remoteFullVersion == null) {
            if (remoteSide.isClient()) {
                return false; // don't allow client without CC to connect
            }
            return true; // allow connecting to server without CC
        }
        if (!checkVersionFormat(VERSION, remoteSide.isClient() ? Side.SERVER : Side.CLIENT)) {
            return true;
        }
        if (!checkVersionFormat(remoteFullVersion, remoteSide)) {
            return true;
        }

        ArtifactVersion version = new DefaultArtifactVersion(remoteFullVersion);
        ArtifactVersion currentVersion = new DefaultArtifactVersion(VERSION);
        if (currentVersion.compareTo(version) < 0) {
            return true; // allow connection if this version is older, let newer one decide
        }
        return (remoteSide.isClient() ? SUPPORTED_CLIENT_VERSIONS : SUPPORTED_SERVER_VERSIONS).containsVersion(version);
    }

    // returns true if version format is known. Side can be null if not logging connection attempt
    private static boolean checkVersionFormat(String version, @Nullable Side remoteSide) {
        int mcVersionSplit = version.indexOf('-');
        if (mcVersionSplit < 0) {
            LOGGER.warn("Connection attempt with unexpected " + remoteSide + " version string: " + version + ". Cannot split into MC "
                    + "version and mod version. Assuming dev environment or special/unknown version, connection will be allowed.");
            return false;
        }

        String modVersion = version.substring(mcVersionSplit + 1);

        if (modVersion.isEmpty()) {
            LOGGER.warn("Connection attempt with unexpected " + remoteSide + " version string: " + version + ". Mod version part not "
                    + "found. Assuming dev environment or special/unknown version,, connection will be allowed");
            return false;
        }

        final String versionRegex = "\\d+\\." + "\\d+\\." + "\\d+\\." + "\\d+" + "(-.+)?";//"MAJORMOD.MAJORAPI.MINOR.PATCH(-final/rcX/betaX)"

        if (!modVersion.matches(versionRegex)) {
            LOGGER.warn("Connection attempt with unexpected " + remoteSide + " version string: " + version + ". Mod version part (" +
                    modVersion + ") does not match expected format ('MAJORMOD.MAJORAPI.MINOR.PATCH(-optionalText)'). Assuming dev " +
                    "environment or special/unknown version, connection will be allowed");
            return false;
        }
        return true;
    }

    public static ResourceLocation location(String location) {
        return new ResourceLocation(MODID, location);
    }

    // essentially a copy of FMLLog.bigWarning, with more lines of stacktrace
    public static void bigWarning(String format, Object... data)
    {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        LOGGER.log(Level.WARN, "****************************************");
        LOGGER.log(Level.WARN, "* "+format, data);
        for (int i = 2; i < 10 && i < trace.length; i++)
        {
            LOGGER.log(Level.WARN, "*  at {}{}", trace[i].toString(), i == 9 ? "..." : "");
        }
        LOGGER.log(Level.WARN, "****************************************");
    }
}
