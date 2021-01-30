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
package io.github.opencubicchunks.cubicchunks.core.asm;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;

/**
 * This Mixin configuration plugin class launched from cubicchunks.mixin.selectable.json.
 * Note, that this plugin is not working in JUnit tests due to missing field of 
 * FMLInjectionData required for MinecraftForge configuration used here.
 * Therefore two Mixin classes with an injection in a same method and with a same priority will cause Mixin to fail. */
public class CubicChunksMixinConfig implements IMixinConfigPlugin {

    @Nonnull
    public static Logger LOGGER = LogManager.getLogger("CubicChunksMixinConfig");
    private final Object2BooleanMap<String> modDependencyConditions = new Object2BooleanLinkedOpenHashMap<String>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void onLoad(String mixinPackage) {
        OptifineState optifineState;
        String optifineVersion = System.getProperty("cubicchunks.optifineVersion", null);
        if (optifineVersion == null) {
            try {
                Class optifineInstallerClass = Class.forName("optifine.Installer");
                Method getVersionHandler = optifineInstallerClass.getMethod("getOptiFineVersion", new Class[0]);
                optifineVersion = (String) getVersionHandler.invoke(null, new Object[0]);
                optifineVersion = optifineVersion.replace("_pre", "");
                optifineVersion = optifineVersion.substring(optifineVersion.length() - 2);
                LOGGER.info("Detected Optifine version: " + optifineVersion);
            } catch (ClassNotFoundException e) {
                optifineVersion = null;
                LOGGER.info("No Optifine detected");
            } catch (Exception e) {
                LOGGER.error("Optifine detected, but could not detect version. It may not work. Assuming Optifine E1...", e);
                optifineVersion = "E1";
            }
        }

        if (optifineVersion == null) {
            optifineState = OptifineState.NOT_LOADED;
        } else if (optifineVersion.compareTo("G5") >= 0) {
            LOGGER.error("Unknown optifine version: " + optifineVersion + ", it may not work. Assuming E1-G5.");
            optifineState = OptifineState.LOADED_E1;
        } else if (optifineVersion.compareTo("E1") >= 0) {
            optifineState = OptifineState.LOADED_E1;
        } else {
            new RuntimeException("Unsupported optifine version " + optifineVersion + ", trying E1-G5 specific mixins").printStackTrace();
            optifineState = OptifineState.LOADED_E1;
        }

        modDependencyConditions.defaultReturnValue(true);
        modDependencyConditions.put(
                "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.MixinRenderGlobalNoOptifine",
                optifineState == OptifineState.NOT_LOADED);
        modDependencyConditions.put(
                "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.vertviewdist.MixinRenderGlobalNoOptifine",
                optifineState == OptifineState.NOT_LOADED && BoolOptions.VERT_RENDER_DISTANCE.getValue());

        modDependencyConditions.put(
                "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine.MixinRenderGlobalOptifine_E",
                optifineState != OptifineState.NOT_LOADED);
        modDependencyConditions.put(
                "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine.MixinRenderChunk",
                optifineState != OptifineState.NOT_LOADED);
        modDependencyConditions.put(
                "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine.MixinRenderChunkUtils",
                optifineState != OptifineState.NOT_LOADED);
        modDependencyConditions.put(
                "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine.MixinExtendedBlockStorage",
                optifineState != OptifineState.NOT_LOADED);
        modDependencyConditions.put(
                "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine.MixinRenderList",
                optifineState != OptifineState.NOT_LOADED);
        modDependencyConditions.put(
                "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine.MixinViewFrustum",
                optifineState != OptifineState.NOT_LOADED);
        modDependencyConditions.put(
                "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine.MixinChunkVisibility",
                optifineState != OptifineState.NOT_LOADED);
        modDependencyConditions.put(
                "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.IGuiVideoSettings",
                optifineState == OptifineState.NOT_LOADED);

        //BetterFps FastBeacon Handling
        boolean enableBetterFpsBeaconFix = false;
        try{
            Class betterFpsConditions = Class.forName("guichaguri.betterfps.transformers.Conditions");
            Method getFixSetting = betterFpsConditions.getMethod("shouldPatch", String.class);
            boolean betterFpsFastBeaconActive = (boolean) getFixSetting.invoke(null, "fastBeacon");
            if(betterFpsFastBeaconActive){
                enableBetterFpsBeaconFix = true;
                LOGGER.info("BetterFps FastBeacon active, will activate mixin for beacons with FastBeacon.");
            }else{
                LOGGER.info("BetterFps is installed, but FastBeacon is not active. Will not enable FastBeacon mixin.");
            }
        } catch (ClassNotFoundException e) {
            LOGGER.info("BetterFps is NOT installed. Will not enable FastBeacon mixin.");
        } catch (Exception e) {
            LOGGER.info("Problem trying to detect BetterFps settings. Will not enable FastBeacon mixin.");
        }
        modDependencyConditions.put(
                "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common.MixinTileEntityBeaconBetterFps", 
                enableBetterFpsBeaconFix);

        File folder = new File(".", "config");
        folder.mkdirs();
        File configFile = new File(folder, "cubicchunks_mixin_config.json");
        LOGGER.info("Loading configuration file " + configFile.getAbsolutePath());
        try {
            if (!configFile.exists())
                this.writeConfigToJson(configFile);
            this.readConfigFromJson(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return this.shouldApplyMixin(mixinClassName);
    }

    public boolean shouldApplyMixin(String mixinClassName) {
        for (BoolOptions configOption : BoolOptions.values()) {
            for (String mixinClassNameOnTrue : configOption.mixinClassNamesOnTrue) {
                if (mixinClassName.equals(mixinClassNameOnTrue)) {
                    boolean load = configOption.value && modDependencyConditions.getBoolean(mixinClassName);
                    LOGGER.debug("shouldApplyMixin({}) = {} from {}.mixinClassNamesOnTrue", mixinClassName, load, configOption);
                    return load;
                }
            }

            for (String mixinClassNameOnFalse : configOption.mixinClassNamesOnFalse) {
                if (mixinClassName.equals(mixinClassNameOnFalse)) {
                    boolean load = !configOption.value && modDependencyConditions.getBoolean(mixinClassName);
                    LOGGER.debug("shouldApplyMixin({}) = {} from {}.mixinClassNamesOnFalse", mixinClassName, load, configOption);
                    return load;
                }
            }
        }
        boolean load =  modDependencyConditions.getBoolean(mixinClassName);
        LOGGER.debug("shouldApplyMixin({}) = {} from modDependencyConditions", mixinClassName, load);
        return load;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    public enum BoolOptions {
        OPTIMIZE_PATH_NAVIGATOR(false, 
                new String[] {},
                new String[] {"io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common.MixinPathNavigate",
                        "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common.MixinWalkNodeProcessor"},
                "Enabling this option will optimize work of vanilla path navigator."
                        + "Using this option in some cases turn entity AI a little dumber."
                        + " Mob standing in a single axis aligned line with player in a middle of a"
                        + " chunk will not try to seek path to player outside of chunks if direct path is blocked."
                        + " You need to restart Minecraft to apply changes."),
        USE_CUBE_ARRAYS_INSIDE_CHUNK_CACHE(true, 
                new String[] {"io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.MixinChunkCache_Vanilla"},
                new String[] {"io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common.MixinChunkCache",
                        "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.MixinChunkCache_Cubic"},
                "Enabling this option will mix cube array into chunk cache"
                        + " for using in entity path navigator."
                        + " Potentially this will slightly reduce server tick time"
                        + " in presence of huge amount of living entities."
                        + " You need to restart Minecraft to apply changes."),
        USE_FAST_COLLISION_CHECK(false, 
                new String[] {"io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common.MixinWorld_SlowCollisionCheck"},
                new String[] {"io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common.MixinWorld_CollisionCheck"},
                "Enabling this option allow using fast collision check."
                        + " Fast collision check can reduce server lag."
                        + " You need to restart Minecraft to apply changes."),
        VERT_RENDER_DISTANCE(true,
                new String[] {},
                new String[] {"io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.MixinEntityRenderer",
                        "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.MixinRenderGlobal"},
                "Enabling this option will make the vertical view distance slider affect clientside vertical render distance." +
                        " When disabled, only serverside load distance is affected.");

        private final boolean defaultValue;
        // Load this Mixin class only if option is false.
        private final String[] mixinClassNamesOnFalse;
        // Load this Mixin class only if option is true.
        private final String[] mixinClassNamesOnTrue;
        private final String description;
        private boolean value;

        private BoolOptions(boolean defaultValue1, String[] mixinClassNamesOnFalse1, String[] mixinClassNamesOnTrue1, String description1) {
            defaultValue = defaultValue1;
            mixinClassNamesOnFalse = mixinClassNamesOnFalse1;
            mixinClassNamesOnTrue = mixinClassNamesOnTrue1;
            description = description1;
            value = defaultValue;
        }

        public boolean getValue() {
            return value;
        }
    }

    public static String getNicelyFormattedName(String name) {
        StringBuffer out = new StringBuffer();
        char char_ = '_';
        char prevchar = 0;
        for (char c : name.toCharArray()) {
            if (c != char_ && prevchar != char_) {
                out.append(String.valueOf(c).toLowerCase());
            } else if (c != char_) {
                out.append(String.valueOf(c));
            }
            prevchar = c;
        }
        return out.toString();
    }
    
    private void writeConfigToJson(File configFile) throws IOException {
        JsonWriter writer = new JsonWriter(new FileWriter(configFile));
        writer.setIndent(" ");
        writer.beginArray();
        for (BoolOptions configOption : BoolOptions.values()) {
            writer.beginObject();
            writer.name(configOption.name());
            writer.value(configOption.value);
            writer.name("description");
            writer.value(configOption.description);
            writer.endObject();
        }
        writer.endArray();
        writer.close();
    }
    
    private void readConfigFromJson(File configFile) throws IOException {
        int expectingOptionsNumber = BoolOptions.values().length;
        JsonReader reader = new JsonReader(new FileReader(configFile));
        reader.beginArray();
        while(reader.hasNext()){
            reader.beginObject();
            next_object:while(reader.hasNext()){
                String name = reader.nextName();
                for(BoolOptions option:BoolOptions.values()){
                    if(option.name().equals(name)){
                        expectingOptionsNumber--;
                        option.value = reader.nextBoolean();
                        continue next_object;
                    }
                }
                reader.skipValue();
            }
            reader.endObject();
        }
        reader.endArray();
        reader.close();
        if (expectingOptionsNumber != 0)
            this.writeConfigToJson(configFile);
    }

    private enum OptifineState {
        NOT_LOADED,
        LOADED_E1
    }
}
