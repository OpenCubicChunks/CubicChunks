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
package cubicchunks.asm;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * This Mixin configuration plugin class launched from cubicchunks.mixin.selectable.json.
 * Note, that this plugin is not working in JUnit tests due to missing field of 
 * FMLInjectionData required for MinecraftForge configuration used here.
 * Therefore two Mixin classes with an injection in a same method and with a same priority will cause Mixin to fail. */
public class CubicChunksMixinConfig implements IMixinConfigPlugin {

    @Nonnull
    public static Logger LOGGER = LogManager.getLogger("CubicChunksMixinConfig");

    @Override
    public void onLoad(String mixinPackage) {
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
        LOGGER.info("Checking config option for "+mixinClassName);
        for (BoolOptions configOption : BoolOptions.values()) {
            if (configOption.mixinClassNamesOnTrue != null)
                for (String mixinClassNameOnTrue : configOption.mixinClassNamesOnTrue)
                    if (mixinClassName.equals(mixinClassNameOnTrue))
                        return configOption.value;

            if (configOption.mixinClassNamesOnFalse != null)
                for (String mixinClassNameOnFalse : configOption.mixinClassNamesOnFalse)
                    if (mixinClassName.equals(mixinClassNameOnFalse))
                        return !configOption.value;
        }
        return true;
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

    public static enum BoolOptions {
        OPTIMIZE_PATH_NAVIGATOR(false, 
                null, 
                new String[] {"cubicchunks.asm.mixin.selectable.common.MixinPathNavigate",
                        "cubicchunks.asm.mixin.selectable.common.MixinWalkNodeProcessor"},
                "Enabling this option will optimize work of vanilla path navigator."
                        + "Using this option in some cases turn entity AI a little dumber."
                        + " Mob standing in a single axis aligned line with player in a middle of a"
                        + " chunk will not try to seek path to player outside of chunks if direct path is blocked."
                        + " You need to restart Minecraft to apply changes."),
        USE_CUBE_ARRAYS_INSIDE_CHUNK_CACHE(true, 
                null, 
                new String[] {"cubicchunks.asm.mixin.selectable.common.MixinWorld_ChunkCache"},
                "Enabling this option will mix cube array into chunk cache"
                        + " for using in entity path navigator."
                        + " Potentially this will slightly reduce server tick time"
                        + " in presence of huge amount of living entities."
                        + " You need to restart Minecraft to apply changes."),
        USE_FAST_COLLISION_CHECK(false, 
                new String[] {"cubicchunks.asm.mixin.selectable.common.MixinWorld_SlowCollisionCheck"},
                new String[] {"cubicchunks.asm.mixin.selectable.common.MixinWorld_CollisionCheck",
                        "cubicchunks.asm.mixin.selectable.common.MixinBlock_FastCollision"},
                "Enabling this option allow using fast collision check."
                        + " Fast collision check can reduce server lag."
                        + " You need to restart Minecraft to apply changes. DO NOT USE UNTIL FIXED!"),
        RANDOM_TICK_IN_CUBE(true, 
                null,
                new String[] {"cubicchunks.asm.mixin.selectable.common.MixinWorldServer_UpdateBlocks"},
                "If set to true, random tick wil be launched from cube instance instead of chunk."
                        + " Cube based random tick may slightly reduce server lag."
                        + " You need to restart Minecraft to apply changes."),
        USE_BIGGER_RENDER_CHUNKS(false, 
                new String[] {
                        "cubicchunks.asm.mixin.selectable.client.MixinRenderGlobal",
                        "cubicchunks.asm.mixin.selectable.client.MixinViewFrustum_RenderHeightFix"},
                new String[] {
                        "cubicchunks.asm.mixin.selectable.client.MixinRenderChunk",
                        "cubicchunks.asm.mixin.selectable.client.MixinCompiledChunk_Fix",
                        "cubicchunks.asm.mixin.selectable.client.MixinRenderGlobal_BiggerRenderChunk",
                        "cubicchunks.asm.mixin.selectable.client.MixinViewFrustum_BiggerRenderChunk"},
                "If set to true, render will use 32 blocks in render chunks instead of 16. "
                + "Incompatibe with Optifine (cause crash). Significally raise FPS.");

        private final boolean defaultValue;
        // Load this Mixin class only if option is false. Can be null.
        @Nullable private final String[] mixinClassNamesOnFalse;
        // Load this Mixin class only if option is true. Can be null.
        @Nullable private final String[] mixinClassNamesOnTrue;
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
}
