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
package cubicchunks.worldgen.generator.custom.biome.replacer;

import cubicchunks.api.worldgen.biome.CubicBiome;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Biome block replacer configuration.
 * <p>
 * This should be used in implementation of {@link IBiomeBlockReplacerProvider} to create {@link IBiomeBlockReplacer}
 * with the specified config. It should NOT be used directly in IBiomeBlockReplacer for performance reasons.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BiomeBlockReplacerConfig {

    private Map<ResourceLocation, Object> configMap = new HashMap<>();

    public void set(ResourceLocation location, Object value) {
        configMap.put(location, value);
    }

    public void set(String modid, String name, Object value) {
        configMap.put(new ResourceLocation(modid, name), value);
    }

    /* Boolean getters */
    public boolean getBoolean(ResourceLocation location) {
        return (boolean) this.configMap.get(location);
    }

    public boolean getBoolean(String modid, String name) {
        return getBoolean(new ResourceLocation(modid, name));
    }

    /* Byte getters */
    public byte getByte(ResourceLocation location) {
        return (byte) this.configMap.get(location);
    }

    public byte getByte(String modid, String name) {
        return getByte(new ResourceLocation(modid, name));
    }

    /* Character getters */
    public char getChar(ResourceLocation location) {
        return (char) this.configMap.get(location);
    }

    public char getChar(String modid, String name) {
        return getChar(new ResourceLocation(modid, name));
    }

    /* Short getters */
    public short getShort(ResourceLocation location) {
        return (short) this.configMap.get(location);
    }

    public short getShort(String modid, String name) {
        return getShort(new ResourceLocation(modid, name));
    }

    /* Int getters */
    public int getInt(ResourceLocation location) {
        return (int) this.configMap.get(location);
    }

    public int getInt(String modid, String name) {
        return getInt(new ResourceLocation(modid, name));
    }

    /* Float getters */
    public float getFloat(ResourceLocation location) {
        return (float) this.configMap.get(location);
    }

    public float getFloat(String modid, String name) {
        return getFloat(new ResourceLocation(modid, name));
    }

    /* Long getters */
    public long getLong(ResourceLocation location) {
        return (long) this.configMap.get(location);
    }

    public long getLong(String modid, String name) {
        return getLong(new ResourceLocation(modid, name));
    }

    /* Double getters */
    public double getDouble(ResourceLocation location) {
        return (double) this.configMap.get(location);
    }

    public double getDouble(String modid, String name) {
        return getDouble(new ResourceLocation(modid, name));
    }

    /* String getters */
    public String getString(ResourceLocation location) {
        Object v = this.configMap.get(location);
        if (v == null) {
            throw new NullPointerException();
        }
        return (String) v;
    }

    public String getString(String modid, String name) {
        return getString(new ResourceLocation(modid, name));
    }

    public void fillDefaults() {
        CubicBiome.REGISTRY.forEach(biome ->
                biome.getReplacerProviders().forEach(prov ->
                        prov.getPossibleConfigOptions().forEach(confOpt ->
                                set(confOpt.getLocation(), confOpt.getDefaultValue()))
                )
        );
    }
}
