/*
 *  This file is part of CubicChunks, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2020 OpenCubicChunks
 *  Copyright (c) 2015-2020 contributors
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

import static net.minecraftforge.fml.Logging.CORE;
import static net.minecraftforge.fml.loading.LogMarkers.FORGEMOD;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;

public class CubicChunksConfig {

    private static final ForgeConfigSpec serverSpec;
    public static final Server SERVER;

    private static final ForgeConfigSpec clientSpec;
    public static final Client CLIENT;

    static {
        Pair<Server, ForgeConfigSpec> server = new ForgeConfigSpec.Builder().configure(Server::new);
        serverSpec = server.getRight();
        SERVER = server.getLeft();

        Pair<Client, ForgeConfigSpec> client = new ForgeConfigSpec.Builder().configure(Client::new);
        clientSpec = client.getRight();
        CLIENT = client.getLeft();
    }

    static void register() {
        MinecraftForge.EVENT_BUS.addListener(CubicChunksConfig::onFileChange);
        MinecraftForge.EVENT_BUS.addListener(CubicChunksConfig::onLoad);

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, clientSpec);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, serverSpec);
    }

    private static void onLoad(final ModConfig.Loading configEvent) {
        LogManager.getLogger().debug(FORGEMOD, "Loaded forge config file {}", configEvent.getConfig().getFileName());
    }

    private static void onFileChange(final ModConfig.Reloading configEvent) {
        LogManager.getLogger().fatal(CORE, "Forge config just got changed on the file system!");
    }

    public static class Server {

        public final ForgeConfigSpec.ConfigValue<ForceCCMode> forceCCMode__needSetValueMethod;
        public TemporaryMutableFakeConfigValue<ForceCCMode> forceCCMode = new TemporaryMutableFakeConfigValue<>(ForceCCMode.NEW_WORLD);

        Server(ForgeConfigSpec.Builder spec) {
            this.forceCCMode__needSetValueMethod = spec
                .comment("Determines when a cubic chunks world should be created for non-cubic-chunks world types.\n"
                    + "NONE - only when cubic chunks world type\n"
                    + "NEW_WORLD - only for newly created worlds\n"
                    + "LOAD_NOT_EXCLUDED - load all worlds as cubic chunks, except excluded dimensions\n"
                    + "ALWAYS - load everything as cubic chunks. Overrides forceDimensionExcludes")
                .translation("cubicchunks.config.force_cc")
                .define("forceLoadCubicChunks", ForceCCMode.NEW_WORLD);
        }
    }

    public static class Client {

        public final ForgeConfigSpec.ConfigValue<Integer> verticalRenderDistance__needSetValueMethod;
        public TemporaryMutableFakeConfigValue<Integer> verticalRenderDistance = new TemporaryMutableFakeConfigValue<>(8);

        Client(ForgeConfigSpec.Builder spec) {
            this.verticalRenderDistance__needSetValueMethod = spec
                .comment("Similar to Minecraft's view distance, only for vertical chunks.\n"
                    + "Automatically adjusted by vertical view distance slider on the"
                    + " client. Does not affect rendering, only what chunks are sent to client.")
                .translation("cubicchunks.config.vert_view_distance")
                .define("verticalRenderDistance", 8);
        }
    }

    public static class TemporaryMutableFakeConfigValue<T> {

        private T value;

        TemporaryMutableFakeConfigValue(T defaultValue) {
            this.value = defaultValue;
        }

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = value;
        }
    }

    public enum ForceCCMode {
        NONE,
        NEW_WORLD,
        LOAD_NOT_EXCLUDED,
        ALWAYS;

        public ForceCCMode flip() {
            switch (this) {
                case NONE:
                    return NEW_WORLD;
                case NEW_WORLD:
                    return NONE;
                default:
                    return this;
            }
        }

        public String translationKey() {
            switch (this) {
                case NONE:
                    return "cubicchunks.gui.worldmenu.cc_disable";
                case NEW_WORLD:
                    return "cubicchunks.gui.worldmenu.cc_enable";
                case LOAD_NOT_EXCLUDED:
                    return "cubicchunks.gui.worldmenu.cc_force";
                case ALWAYS:
                    return "cubicchunks.gui.worldmenu.cc_force_ignore_exclude";
                default:
                    throw new IllegalStateException();
            }
        }
    }
}
