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

import io.github.opencubicchunks.cubicchunks.core.network.PacketDispatcher;
import io.github.opencubicchunks.cubicchunks.core.proxy.CommonProxy;
import io.github.opencubicchunks.cubicchunks.core.world.type.VanillaCubicWorldType;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubeGeneratorsRegistry;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;
import net.minecraftforge.fml.common.versioning.InvalidVersionSpecificationException;
import net.minecraftforge.fml.common.versioning.VersionRange;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

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

    public static final int FIXER_VERSION = 0;

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

    @Nonnull
    public static Logger LOGGER = LogManager.getLogger("EarlyCubicChunks");//use some logger even before it's set. useful for unit tests

    @SidedProxy(clientSide = "io.github.opencubicchunks.cubicchunks.core.proxy.ClientProxy", serverSide = "io.github.opencubicchunks.cubicchunks.core.proxy.ServerProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        LOGGER = e.getModLog();

        VanillaCubicWorldType.create();
        LOGGER.debug("Registered world types");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        proxy.init();

        PacketDispatcher.registerPackets();
        CubeGeneratorsRegistry.computeSortedGeneratorList();
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
